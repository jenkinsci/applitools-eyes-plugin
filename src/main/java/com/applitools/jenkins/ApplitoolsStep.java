package com.applitools.jenkins;

import com.google.inject.Inject;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import static com.applitools.jenkins.ApplitoolsBuildWrapper.isCustomBatchId;

/**
 * Created by addihorowitz on 5/7/17.
 */
public class ApplitoolsStep extends AbstractStepImpl {
    private String serverURL;
    private final boolean notifyOnCompletion;
    private final String applitoolsApiKey;
    private final boolean dontCloseBatches;
    private final boolean eyesScmIntegrationEnabled;

    @DataBoundConstructor
    public ApplitoolsStep(String serverURL,
                          boolean notifyOnCompletion,
                          String applitoolsApiKey,
                          boolean dontCloseBatches,
                          boolean eyesScmIntegrationEnabled)
    {
        super(true);
        this.notifyOnCompletion = notifyOnCompletion;
        this.applitoolsApiKey = applitoolsApiKey;
        this.dontCloseBatches = dontCloseBatches;
        this.eyesScmIntegrationEnabled = eyesScmIntegrationEnabled;
        if (serverURL != null && !serverURL.isEmpty())
            this.serverURL = serverURL;
    }

    public String getServerURL() {
        if (serverURL != null && !serverURL.isEmpty())
            return serverURL;
        return ApplitoolsCommon.APPLITOOLS_DEFAULT_URL;
    }

    public String getApplitoolsApiKey() {
        return this.applitoolsApiKey;
    }

    public boolean getNotifyOnCompletion() {
        return this.notifyOnCompletion;
    }

    public boolean getDontCloseBatches() {
        return this.dontCloseBatches;
    }
    public boolean getEyesScmIntegrationEnabled() {
        return this.eyesScmIntegrationEnabled;
    }

    public static class ApplitoolsStepExecution extends AbstractStepExecutionImpl {
        private static final long serialVersionUID = 1;
        @Inject(optional=true) private transient ApplitoolsStep step;
        private transient Run<?,?> run;
        private transient TaskListener listener;
        private transient FilePath workspace;
        private transient Launcher launcher;

        private BodyExecution body;

        @Override
        public boolean start() throws Exception {
            run = getContext().get(Run.class);
            listener = getContext().get(TaskListener.class);
            workspace = getContext().get(FilePath.class);
            launcher = getContext().get(Launcher.class);
            EnvVars env = getContext().get(EnvVars.class);

            Job<?,?> job = run.getParent();
            if (!(job instanceof TopLevelItem)) {
                throw new Exception("should be top level job " + job);
            }

            HashMap<String,String> overrides = new HashMap<>();
            if (env != null) {
                overrides.putAll(env);
            }
            final Map<String, String> applitoolsArtifacts = ApplitoolsBuildWrapper.getApplitoolsArtifactList(
                    getContext().get(FilePath.class), listener);

            ApplitoolsCommon.buildEnvVariablesForExternalUsage(overrides, run, listener, workspace, launcher,
                    step.getServerURL(), step.getApplitoolsApiKey(), applitoolsArtifacts,
                    step.getEyesScmIntegrationEnabled());

            body = getContext().newBodyInvoker()
                    .withContext(
                            EnvironmentExpander.merge(
                                    getContext().get(EnvironmentExpander.class),
                                    new ApplitoolsEnvironmentExpander(overrides)
                            )
                    ).withCallback(new BodyExecutionCallback() {
                        @Override
                        public void onStart(StepContext context) {
                            try {
                                ApplitoolsCommon.integrateWithApplitools(run,
                                        step.getServerURL(),
                                        step.getNotifyOnCompletion(),
                                        step.getApplitoolsApiKey(),
                                        step.getDontCloseBatches(),
                                        step.getEyesScmIntegrationEnabled());
                            } catch (Exception ex) {
                                listener.getLogger().println("Failed to update properties");
                            }
                        }

                        @Override
                        public void onSuccess(StepContext context, Object result) {
                            closeBatch();
                            context.onSuccess(result);
                        }

                        @Override
                        public void onFailure(StepContext context, Throwable t) {
                            closeBatch();
                            context.onFailure(t);
                        }

                        public void closeBatch() {
                            try {
                                if (isCustomBatchId) {
                                    ApplitoolsCommon.archiveArtifacts(run, workspace, launcher, listener);
                                }

                                if (!step.dontCloseBatches){
                                    ApplitoolsCommon.closeBatch(
                                            run, listener,
                                            step.getServerURL(),
                                            step.getNotifyOnCompletion(),
                                            step.getApplitoolsApiKey(),
                                            step.getEyesScmIntegrationEnabled());
                                }
                            }
                            catch (IOException ex) {
                                listener.getLogger().println("Error closing batch: " + ex.getMessage());
                            }
                        }
                    })
                    .withCallback(BodyExecutionCallback.wrap(getContext()))
                    .start();

            return false;
        }

        @Override
        public void stop(@NonNull Throwable cause) {
            if (body!=null) {
                body.cancel(cause);
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(ApplitoolsStepExecution.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Applitools Support";
        }

        @Override public String getFunctionName() {
            return "Applitools";
        }

        @Override public boolean takesImplicitBlockArgument() {
            return true;
        }


        @Override
        public Set<Class<?>> getProvidedContext() {
            return Collections.<Class<?>>singleton(ApplitoolsEnv.class);
        }

        @Override
        public ApplitoolsStep newInstance(StaplerRequest req, JSONObject formData) {
            return new ApplitoolsStep(
                    formData.getString("serverURL"),
                    formData.getBoolean("notifyOnCompletion"),
                    formData.getString("applitoolsApiKey"),
                    formData.getBoolean("dontCloseBatches"),
                    formData.getBoolean("eyesScmIntegrationEnabled")
                    );
        }

    }
}
