package com.applitools.jenkins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.util.FormValidation;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import jenkins.util.VirtualFile;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Code for the build page.
 */
public class ApplitoolsBuildWrapper extends BuildWrapper implements Serializable {
    public final static String BATCH_NOTIFICATION_PATH = "/api/sessions/batches/%s/close/bypointerid";
    public String serverURL;
    public boolean notifyOnCompletion;
    public String applitoolsApiKey;
    public boolean dontCloseBatches;
    public boolean eyesScmIntegrationEnabled;
    static boolean isCustomBatchId = false;

    static final Map<String, String> ARTIFACT_PATHS = new HashMap<>();

    static {
        ARTIFACT_PATHS.put(
                ApplitoolsCommon.APPLITOOLS_ARTIFACT_PREFIX +
                        "_" +
                        ApplitoolsEnvironmentUtil.APPLITOOLS_BATCH_ID,
                ApplitoolsCommon.APPLITOOLS_ARTIFACT_FOLDER +
                        "/" +
                        ApplitoolsEnvironmentUtil.APPLITOOLS_BATCH_ID
        );
    }

    @DataBoundConstructor
    public ApplitoolsBuildWrapper(String serverURL, boolean notifyOnCompletion,
                                  String applitoolsApiKey, boolean dontCloseBatches, boolean eyesScmIntegrationEnabled) {
        this.applitoolsApiKey = applitoolsApiKey;
        this.notifyOnCompletion = notifyOnCompletion;
        this.dontCloseBatches = dontCloseBatches;
        this.eyesScmIntegrationEnabled = eyesScmIntegrationEnabled;
        if (serverURL != null && !serverURL.isEmpty())
        {
            if (DescriptorImpl.validURL(serverURL))
            {
                this.serverURL = serverURL.trim();
            }
        } else {
            this.serverURL = ApplitoolsCommon.APPLITOOLS_DEFAULT_URL;
        }
    }

    @Override
    public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException {

        runPreBuildActions(build, listener);

        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                if (isCustomBatchId) {
                    build.pickArtifactManager().archive(build.getWorkspace(), launcher, listener, ARTIFACT_PATHS);
                }
                if (!dontCloseBatches) {
                    ApplitoolsCommon.closeBatch(
                            build, listener, serverURL, notifyOnCompletion, applitoolsApiKey, eyesScmIntegrationEnabled);
                }
                return true;
            }

            @Override
            public void buildEnvVars(Map<String, String> env) {
                FilePath workspace = build.getWorkspace();
                if (workspace != null) {
                    Map <String, String> applitoolsArtifacts = getApplitoolsArtifactList(workspace, listener);
                    ApplitoolsCommon.buildEnvVariablesForExternalUsage(env, build, listener, workspace, launcher,
                            serverURL, applitoolsApiKey, applitoolsArtifacts, eyesScmIntegrationEnabled);
                }
            }
        };
    }

    public static Map<String, String> getApplitoolsArtifactList(FilePath workspace, TaskListener listener) {
        Map<String, String> applitoolsArtifacts = new HashMap<>();
        if (workspace != null) {
            for (Map.Entry<String, String> apath : ARTIFACT_PATHS.entrySet()) {
                try {
                    VirtualFile rootDir = workspace.absolutize().toVirtualFile();
                    listener.getLogger().println("Workspace absolute path: " + workspace.absolutize());

                    InputStream stream = rootDir.child(apath.getValue()).open();
                    String value = IOUtils.toString(stream, StandardCharsets.UTF_8).replaceAll(System.lineSeparator(), "");
                    Matcher m = ApplitoolsCommon.artifactRegexp.matcher(apath.getKey());
                    if (m.find()) {
                        listener.getLogger().println("Found custom batch id: " + value);
                        applitoolsArtifacts.put(m.group(1), value);
                        isCustomBatchId = true;
                    }
                } catch (IOException e) {
                    isCustomBatchId = false;
                    listener.getLogger().printf("Custom BATCH_ID is not defined in: %s%n", workspace.toVirtualFile());
                } catch (InterruptedException e) {
                    isCustomBatchId = false;
                    listener.getLogger().println("Invalid workspace path. Skipping check for applitools artifacts");
                }
            }
        } else {
            listener.getLogger().println("build.getWorkspace() returned null, skipping check for applitools artifacts.");
        }
        return applitoolsArtifacts;
    }

    private void runPreBuildActions(final Run<?,?> build, final BuildListener listener) throws IOException
    {
        listener.getLogger().println("Starting Applitools Eyes pre-build (server URL is '" + this.serverURL + "') apiKey is " + this.applitoolsApiKey);

        ApplitoolsCommon.integrateWithApplitools(build,
                this.serverURL, this.notifyOnCompletion, this.applitoolsApiKey, this.dontCloseBatches, this.eyesScmIntegrationEnabled);

        listener.getLogger().println("Finished Applitools Eyes pre-build");
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
        public static String APPLITOOLS_DEFAULT_URL = "https://eyes.applitools.com";
        public static boolean NOTIFY_ON_COMPLETION = true;
        public static boolean EYES_SCM_INTEGRATION_ENABLED = false;

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject<?,?>> aClass) {
            // indicates that this builder can be used with all kinds of project types
            return true;
        }

        private static boolean validURL(String url)
        {
            // Just making sure the URL is valid.
            try {
                new URL(url);
            } catch (Exception ex) {
                return false;
            }
            return true;
        }

        public FormValidation doCheckServerURL(@QueryParameter String value)
        {
            if (validURL(value))
            {
                return FormValidation.ok();
            }
            else
            {
                return FormValidation.error("Not a valid URL. Please make sure to use the following format https://<server>");
            }
        }

        /**
         * This human-readable name is used in the configuration screen.
         */
        @NonNull
        public String getDisplayName() {
            return "Applitools Support";
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) {
            return new ApplitoolsBuildWrapper(
                    formData.getString("serverURL"),
                    formData.getBoolean("notifyOnCompletion"),
                    formData.getString("applitoolsApiKey"),
                    formData.getBoolean("dontCloseBatches"),
                    formData.getBoolean("eyesScmIntegrationEnabled"));
        }
    }
}

