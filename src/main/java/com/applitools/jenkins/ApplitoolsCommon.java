package com.applitools.jenkins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.JobProperty;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hudson.util.Secret;
import jenkins.model.ArtifactManager;
import jenkins.util.VirtualFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import static com.applitools.jenkins.ApplitoolsBuildWrapper.ARTIFACT_PATHS;
import static com.applitools.jenkins.ApplitoolsEnvironmentUtil.APPLITOOLS_BATCH_ID;

/**
 * Common methods to be used other parts of the plugin
 */
public class ApplitoolsCommon {

    public final static String APPLITOOLS_DEFAULT_URL = "https://eyes.applitools.com";
    public final static boolean NOTIFY_ON_COMPLETION = true;
    public final static String BATCH_NOTIFICATION_PATH = "/api/sessions/batches/%s/close/bypointerid";
    public final static String BATCH_BIND_POINTERS_PATH = "/api/sessions/batches/bindpointers/%s";
    public final static String APPLITOOLS_ARTIFACT_FOLDER = ".applitools";
    public final static String APPLITOOLS_ARTIFACT_PREFIX = "APPLITOOLS";
    public final static Pattern artifactRegexp = Pattern.compile(ApplitoolsCommon.APPLITOOLS_ARTIFACT_PREFIX + "_(.*)");
    private static final Logger logger = Logger.getLogger(ApplitoolsStatusDisplayAction.class.getName());
    private static Map<String, String> env;

    @SuppressWarnings("rawtypes")
    public static void integrateWithApplitools(Run run, String serverURL, boolean notifyOnCompletion,
                                               Secret applitoolsApiKey, boolean dontCloseBatches,
                                               boolean eyesScmIntegrationEnabled
    ) throws IOException {
        updateProjectProperties(run, serverURL, notifyOnCompletion, applitoolsApiKey, dontCloseBatches, eyesScmIntegrationEnabled);
        addApplitoolsActionToBuild(run);
        run.save();
    }

    @SuppressWarnings("rawtypes")
    private static void updateProjectProperties(Run run, String serverURL, boolean notifyOnCompletion,
                                                Secret applitoolsApiKey, boolean dontCloseBatches,
                                                boolean eyesScmIntegrationEnabled
    ) throws IOException {
        boolean found = false;
        for (Object property : run.getParent().getAllProperties()) {
            if (property instanceof ApplitoolsProjectConfigProperty) {
                ((ApplitoolsProjectConfigProperty) property).setServerURL(serverURL);
                ((ApplitoolsProjectConfigProperty) property).setNotifyOnCompletion(notifyOnCompletion);
                ((ApplitoolsProjectConfigProperty) property).setApplitoolsApiKey(applitoolsApiKey);
                ((ApplitoolsProjectConfigProperty) property).setDontCloseBatches(dontCloseBatches);
                ((ApplitoolsProjectConfigProperty) property).setEyesScmIntegrationEnabled(eyesScmIntegrationEnabled);
                found = true;
                break;
            }
        }
        if (!found) {
            JobProperty jp = new ApplitoolsProjectConfigProperty(
                    serverURL, notifyOnCompletion, applitoolsApiKey, dontCloseBatches, eyesScmIntegrationEnabled);
            run.getParent().addProperty(jp);
        }
        run.getParent().save();
    }

    private static void addApplitoolsActionToBuild(final Run<?, ?> build) {
        ApplitoolsStatusDisplayAction buildAction = build.getAction(ApplitoolsStatusDisplayAction.class);
        if (buildAction == null) {
            buildAction = new ApplitoolsStatusDisplayAction(build);
            build.addAction(buildAction);
        }
    }

    private static void sendBindBatchPointersRequest(String serverURL, String batchId, String buildId, Secret apiKey,
                                                     final TaskListener listener)
            throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        URI targetUrl = null;
        try {
            targetUrl = new URIBuilder(serverURL)
                    .setPath(String.format(BATCH_BIND_POINTERS_PATH, buildId))
                    .addParameter("apiKey", apiKey.getPlainText())
                    .build();
        } catch (URISyntaxException e) {
            logger.warning("Couldn't build URI: " + e.getMessage());
        }

        HttpPost request = new HttpPost(targetUrl);
        String jsonData = "{\"secondaryBatchPointerId\":\"" + batchId + "\"}";
        HttpEntity params = new StringEntity(jsonData, ContentType.APPLICATION_JSON);
        request.setEntity(params);
        try {
            listener.getLogger().printf("Batch Bind Pointers request called with batch id %s and secondary batch pointer id %s%n", buildId, batchId);
            listener.getLogger().printf("REQUEST: %s%n", request);
            listener.getLogger().printf("REQUEST BODY: %s %s%n", params, jsonData);
            HttpResponse httpResponse = httpClient.execute(request);
            StatusLine statusLine = httpResponse.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            listener.getLogger().println("Batch binding is done with " + statusCode + " status");
            if (statusCode >= 400) {
                listener.error("Batch binding failed with " + statusCode + " status");
            }
        } catch (IOException e) {
            listener.error("Batch binding failed with " + e.getMessage());
            throw e;
        } finally {
            request.abort();
        }
    }

    public static void buildEnvVariablesForExternalUsage(Map<String, String> env, final Run<?, ?> build,
                                                         final TaskListener listener, FilePath workspace,
                                                         Launcher launcher, String serverURL, Secret applitoolsApiKey,
                                                         Map<String, String> artifacts, boolean scmIntegrationEnabled) {
        ApplitoolsCommon.env = env;
        String projectName = build.getParent().getDisplayName();
        MutableBoolean isCustom = new MutableBoolean(false);

        String batchId = ApplitoolsStatusDisplayAction.generateBatchId(
                env, projectName, build.getNumber(), build.getTimestamp(), artifacts, isCustom, scmIntegrationEnabled);

        if (scmIntegrationEnabled) {

            String buildId = null;
            if (env.get("APPLITOOLS_BATCH_ID") != null) {
                buildId = env.get("APPLITOOLS_BATCH_ID");
            } else {
                buildId = env.get("GIT_COMMIT");
            }

            String apiKeySysEnv = System.getenv("APPLITOOLS_API_KEY");
            if (applitoolsApiKey.equals(apiKeySysEnv)) {
                listener.getLogger().println("API Key is the same as the one in the system environment variable APPLITOOLS_API_KEY");
            } else {
                listener.getLogger().println("API Key is different from the one in the system environment variable APPLITOOLS_API_KEY");
            }

            String apiKeyEnv = env.get("APPLITOOLS_API_KEY");
            if (applitoolsApiKey.equals(apiKeyEnv)) {
                listener.getLogger().println("API Key is the same as the one in the environment variable APPLITOOLS_API_KEY");
            } else {
                listener.getLogger().println("API Key is different from the one in the environment variable APPLITOOLS_API_KEY");
            }

            if (apiKeySysEnv != null && apiKeySysEnv.equals(apiKeyEnv)) {
                listener.getLogger().println("System env var APPLITOOLS_API_KEY is the same as the loaded env var");
            } else {
                listener.getLogger().println("System env var APPLITOOLS_API_KEY is different from the loaded env var");
            }

            String decryptedApiKey = applitoolsApiKey.getPlainText();

            listener.getLogger().println("APPLITOOLS_SHOW_API_KEY exists in global context: " + System.getenv().containsKey("APPLITOOLS_SHOW_API_KEY"));
            listener.getLogger().println("APPLITOOLS_SHOW_API_KEY exists in local context: " + env.containsKey("APPLITOOLS_SHOW_API_KEY"));
            listener.getLogger().println("API KEY length: " + decryptedApiKey.length());

            if (decryptedApiKey.length() > 10) {
                listener.getLogger().println("Partial API KEY: " + decryptedApiKey.substring(0, 5)+"..."+decryptedApiKey.substring(decryptedApiKey.length()-5));
            }
            if (System.getenv().containsKey("APPLITOOLS_SHOW_API_KEY") || env.containsKey("APPLITOOLS_SHOW_API_KEY")) {
                listener.getLogger().println("APPLITOOLS_SHOW_API_KEY exists. API KEY: " + splitAndJoin(decryptedApiKey, 5, "-"));
            } else {
                listener.getLogger().println("APPLITOOLS_SHOW_API_KEY does not exist.");
            }

            try {
                sendBindBatchPointersRequest(serverURL, batchId, buildId, applitoolsApiKey, listener);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            batchId = buildId;
        }

        String filepath = ARTIFACT_PATHS.get(APPLITOOLS_ARTIFACT_PREFIX + "_" + APPLITOOLS_BATCH_ID);
        FilePath batchIdFilePath = workspace.child(filepath);
        if (isCustom.isTrue()) {
            try {
                batchIdFilePath.write(batchId, StandardCharsets.UTF_8.name());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            archiveArtifacts(build, workspace, launcher, listener);
        }
        String batchName = projectName;
        ApplitoolsEnvironmentUtil.outputVariables(listener, build, env, serverURL, batchName, batchId, projectName,
                applitoolsApiKey);
        try {
            batchIdFilePath.delete();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static String splitAndJoin(String input, int n, String joinChar) {
        List<String> parts = new ArrayList<>();
        int length = input.length();
        for (int i = 0; i < length; i += n) {
            parts.add(input.substring(i, Math.min(length, i + n)));
        }
        return String.join(joinChar, parts);
    }

    public static void archiveArtifacts(Run<?, ?> run, FilePath workspace, Launcher launcher,
                                        final TaskListener listener) {
        try {
            ArtifactManager artifactManager = run.getArtifactManager();
            artifactManager.archive(workspace, launcher, (BuildListener) listener, ARTIFACT_PATHS);
        } catch (InterruptedException | IOException ex) {
            listener.getLogger().println("Error archiving artifacts: " + ex.getMessage());
        }
    }

    public static Map<String, String> getEnv() {
        return env;
    }

    public static String getEnv(String key) {
        return env.get(key);
    }

    public static void closeBatch(Run<?, ?> run, TaskListener listener, String serverURL,
                                  boolean notifyOnCompletion, Secret applitoolsApiKey, boolean scmIntegrationEnabled)
            throws IOException {
        if (notifyOnCompletion && applitoolsApiKey != null && !applitoolsApiKey.getPlainText().isEmpty()) {
            String batchId = ApplitoolsStatusDisplayAction.generateBatchId(
                    env,
                    run.getParent().getDisplayName(),
                    run.getNumber(),
                    run.getTimestamp(),
                    ApplitoolsCommon.checkApplitoolsArtifacts(
                            run.getArtifacts(),
                            run.getArtifactManager().root()
                    ),
                    null, scmIntegrationEnabled
            );
            HttpClient httpClient = HttpClientBuilder.create().build();
            URI targetUrl = null;
            try {
                targetUrl = new URIBuilder(serverURL)
                        .setPath(String.format(BATCH_NOTIFICATION_PATH, batchId))
                        .addParameter("apiKey", applitoolsApiKey.getPlainText())
                        .build();
            } catch (URISyntaxException e) {
                logger.warning("Couldn't build URI: " + e.getMessage());
            }

            HttpUriRequest deleteRequest = new HttpDelete(targetUrl);
            try {
                listener.getLogger().printf("Batch notification called with %s%n", batchId);
                int statusCode = httpClient.execute(deleteRequest).getStatusLine().getStatusCode();
                listener.getLogger().println("Delete batch is done with " + statusCode + " status");
            } finally {
                deleteRequest.abort();
            }
        }

    }

    public static Map<String, String> checkApplitoolsArtifacts(List<? extends Run<?, ?>.Artifact> artifactList, VirtualFile file) {
        Map<String, String> result = new HashMap<>();
        if (!artifactList.isEmpty() && file != null) {
            for (Run<?, ?>.Artifact artifact : artifactList) {
                String artifactFileName = artifact.getFileName();
                Matcher m = artifactRegexp.matcher(artifactFileName);
                if (m.find()) {
                    String artifactName = m.group(1);
                    try {
                        InputStream stream = file.child(artifactFileName).open();
                        String value = IOUtils.toString(stream, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");
                        result.put(artifactName, value);
                    } catch (java.io.IOException e) {
                        logger.warning("Couldn't get artifact " + artifactFileName + "." + e.getMessage());
                    }
                }
            }
        }
        return result;
    }

    private static String pluginVersion = null;

    public static String getPluginVersion() {
        if (pluginVersion == null) {
            pluginVersion = ApplitoolsCommon.class.getPackage().getImplementationVersion();
            try {
                Properties p = new Properties();
                InputStream is = ApplitoolsCommon.class.getClassLoader().getResourceAsStream("my.properties");
                if (is != null) {
                    p.load(is);
                    pluginVersion = p.getProperty("version", "");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error getting plugin version", e);
            }
        }
        return pluginVersion;
    }
}
