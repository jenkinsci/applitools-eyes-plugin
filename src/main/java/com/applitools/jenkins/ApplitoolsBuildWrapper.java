package com.applitools.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import jenkins.util.VirtualFile;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import hudson.util.FormValidation;
import org.kohsuke.stapler.QueryParameter;

import java.net.URL;
import java.util.regex.Matcher;

/**
 * Code for the build page.
 */
public class ApplitoolsBuildWrapper extends BuildWrapper implements Serializable {
    public final static String BATCH_NOTIFICATION_PATH = "/api/sessions/batches/%s/close/bypointerid";
    public String serverURL;
    public boolean notifyByCompletion;
    public String applitoolsApiKey;

    public static final Map<String, String> ARTIFACT_PATHS = new HashMap();

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
    public ApplitoolsBuildWrapper(String serverURL, boolean notifyByCompletion, String applitoolsApiKey) {
        this.applitoolsApiKey = applitoolsApiKey;
        this.notifyByCompletion = notifyByCompletion;
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
    public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {

        runPreBuildActions(build, listener);

        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                build.pickArtifactManager().archive(build.getWorkspace(), launcher, listener, ARTIFACT_PATHS);
                ApplitoolsCommon.closeBatch(build, listener, serverURL, notifyByCompletion, applitoolsApiKey);
                return true;
            }

            @Override
            public void buildEnvVars(Map<String, String> env) {
                Map <String, String> applitoolsArtifacts = getApplitoolsArtifactList(build, listener);
                ApplitoolsCommon.buildEnvVariablesForExternalUsage(env, build, listener, serverURL, applitoolsApiKey, applitoolsArtifacts);
            }
        };
    }

    public static Map<String, String> getApplitoolsArtifactList(AbstractBuild build, TaskListener listener) {
        Map<String, String> applitoolsArtifacts = new HashMap();
        VirtualFile rootDir = build.getWorkspace().toVirtualFile();
        for(Map.Entry<String, String> apath : ARTIFACT_PATHS.entrySet()) {
            try {
                InputStream stream = rootDir.child(apath.getValue()).open();
                String value = IOUtils.toString(stream, StandardCharsets.UTF_8.name()).replaceAll(System.getProperty("line.separator"), "");
                Matcher m = ApplitoolsCommon.artifactRegexp.matcher(apath.getKey());
                if (m.find()) {
                    applitoolsArtifacts.put(m.group(1), value);
                }
            } catch (IOException e) {
                listener.getLogger().println(String.format("Custom BATCH_ID is not defined: %s", rootDir.child(apath.getValue())));
            }
        }
        return applitoolsArtifacts;
    }

    private void runPreBuildActions(final Run build, final BuildListener listener) throws IOException, InterruptedException
    {
        listener.getLogger().println("Starting Applitools Eyes pre-build (server URL is '" + this.serverURL + "') apiKey is " + this.applitoolsApiKey);

        ApplitoolsCommon.integrateWithApplitools(build, this.serverURL, this.notifyByCompletion, this.applitoolsApiKey);

        listener.getLogger().println("Finished Applitools Eyes pre-build");
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
        public static String APPLITOOLS_DEFAULT_URL="https://eyes.applitools.com";
        public static boolean NOTIFY_BY_COMPLETION=true;

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types
            return true;
        }

        protected static boolean validURL(String url)
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
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Applitools Support";
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            return new ApplitoolsBuildWrapper(formData.getString("serverURL"), formData.getBoolean("notifyByCompletion"), formData.getString("applitoolsApiKey"));
        }
    }
}

