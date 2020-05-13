package com.applitools.jenkins;

import hudson.model.Build;
import hudson.model.JobProperty;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.util.VirtualFile;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;


/**
 * Common methods to be used other parts of the plugin
 */
public class ApplitoolsCommon {

    public final static String APPLITOOLS_DEFAULT_URL = "https://eyes.applitools.com";
    public final static boolean NOTIFY_BY_COMPLETION = true;
    public final static String BATCH_NOTIFICATION_PATH = "/api/sessions/batches/%s/close/bypointerid";
    public final static String APPLITOOLS_ARTIFACT_FOLDER = ".applitools";
    public final static String APPLITOOLS_ARTIFACT_PREFIX = "APPLITOOLS";
    public static Pattern artifactRegexp = Pattern.compile(ApplitoolsCommon.APPLITOOLS_ARTIFACT_PREFIX + "_(.*)");
    private static final Logger logger = Logger.getLogger(ApplitoolsStatusDisplayAction.class.getName());

    public static void integrateWithApplitools(Run run, String serverURL, boolean notifyByCompletion, String applitoolsApiKey
    ) throws IOException
    {
        updateProjectProperties(run, serverURL, notifyByCompletion, applitoolsApiKey);
        addApplitoolsActionToBuild(run);
        run.save();
    }
    private static void updateProjectProperties(Run run, String serverURL, boolean notifyByCompletion, String applitoolsApiKey
                                               ) throws IOException
    {
        boolean found = false;
        for (Object property:run.getParent().getAllProperties())
        {
            if (property instanceof ApplitoolsProjectConfigProperty)
            {
                ((ApplitoolsProjectConfigProperty)property).setServerURL(serverURL);
                ((ApplitoolsProjectConfigProperty)property).setNotifyByCompletion(notifyByCompletion);
                ((ApplitoolsProjectConfigProperty)property).setApplitoolsApiKey(applitoolsApiKey);
                found = true;
                break;
            }
        }
        if (!found)
        {
            JobProperty jp = new ApplitoolsProjectConfigProperty(serverURL, notifyByCompletion, applitoolsApiKey);
            run.getParent().addProperty(jp);
        }
        run.getParent().save();
    }

    private static void addApplitoolsActionToBuild(final Run build)
    {
        ApplitoolsStatusDisplayAction buildAction = build.getAction(ApplitoolsStatusDisplayAction.class);
        if (buildAction == null) {
            buildAction = new ApplitoolsStatusDisplayAction(build);
            build.addAction(buildAction);
        }
    }

    public static void buildEnvVariablesForExternalUsage(Map<String, String> env, final Run build, final TaskListener listener, String serverURL, String applitoolsApiKey) {
        buildEnvVariablesForExternalUsage(env, build, listener, serverURL, applitoolsApiKey, null);
    }

    public static void buildEnvVariablesForExternalUsage(Map<String, String> env, final Run build, final TaskListener listener, String serverURL, String applitoolsApiKey, Map<String, String> artifacts)
    {
        String projectName = build.getParent().getDisplayName();
        String batchId = ApplitoolsStatusDisplayAction.generateBatchId(projectName, build.getNumber(), build.getTimestamp(), artifacts);
        String batchName = projectName;
        ApplitoolsEnvironmentUtil.outputVariables(listener, env, serverURL, batchName, batchId, projectName, applitoolsApiKey);
    }

    public static void closeBatch(Run run, TaskListener listener, String serverURL, boolean notifyByCompletion, String applitoolsApiKey) throws IOException {
        if (notifyByCompletion && applitoolsApiKey != null && !applitoolsApiKey.isEmpty()) {
            String batchId = ApplitoolsStatusDisplayAction.generateBatchId(run.getParent().getDisplayName(), run.getNumber(), run.getTimestamp(), ApplitoolsCommon.checkApplitoolsArtifacts(run.getArtifacts(), run.getArtifactManager().root()));
            HttpClient httpClient = new HttpClient();
            URI targetUrl = new URI(serverURL, false);
            targetUrl.setPath(String.format(BATCH_NOTIFICATION_PATH, batchId));
            targetUrl.setQuery("apiKey=" + applitoolsApiKey);

            DeleteMethod deleteRequest = new DeleteMethod(targetUrl.toString());
            try {
                listener.getLogger().println(String.format("Batch notification called with %s", batchId));
                int statusCode = httpClient.executeMethod(deleteRequest);
                listener.getLogger().println("Delete batch is done with " + Integer.toString(statusCode) + " status");
            } finally {
                deleteRequest.releaseConnection();
            }

        }

    }

    public static Map<String, String> checkApplitoolsArtifacts(List<Run.Artifact> artifactList, VirtualFile file) {
        Map<String, String> result = new HashMap();
        if (artifactList.size() > 0 && file != null) {
            for (Run.Artifact artifact : artifactList) {
                String artifactFileName = artifact.getFileName();
                Matcher m = artifactRegexp.matcher(artifactFileName);
                if (m.find()) {
                    String artifactName = m.group(1);
                    try {
                        InputStream stream = file.child(artifactFileName).open();
                        String value = IOUtils.toString(stream, StandardCharsets.UTF_8.name()).replaceAll(System.getProperty("line.separator"), "");
                        result.put(artifactName, value);
                    } catch (java.io.IOException e) {
                        logger.warning("Could't get artifact " + artifactFileName + "." + e.getMessage());
                    }
                }
            }
        }
        return result;
    }

}
