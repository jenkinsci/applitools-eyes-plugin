package com.applitools.jenkins;
import hudson.model.*;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.Extension;
import org.kohsuke.stapler.StaplerRequest;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;

import java.io.IOException;
import java.util.HashMap;
import javax.annotation.Nonnull;
import com.google.inject.Inject;
import hudson.EnvVars;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
/**
 * Created by addihorowitz on 5/7/17.
 */
public class ApplitoolsStep extends AbstractStepImpl {
    private String serverURL;
    private boolean notifyByCompletion;
    private String applitoolsApiKey;

    @DataBoundConstructor
    public ApplitoolsStep(String serverURL, boolean notifyByCompletion, String applitoolsApiKey)
    {
        this.notifyByCompletion = notifyByCompletion;
        this.applitoolsApiKey = applitoolsApiKey;
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

    public boolean getNotifyByCompletion() { return this.notifyByCompletion; }

    public static class ApplitoolsStepExecution extends AbstractStepExecutionImpl {
        private static final long serialVersionUID = 1;
        @Inject(optional=true) private transient ApplitoolsStep step;
        @StepContextParameter private transient Run<?,?> run;
        @StepContextParameter private transient TaskListener listener;
        @StepContextParameter private transient EnvVars env;

        private BodyExecution body;

        @Override
        public boolean start() throws Exception {
            Job<?,?> job = run.getParent();
            if (!(job instanceof TopLevelItem)) {
                throw new Exception("should be top level job " + job);
            }
            HashMap<String,String> overrides = new HashMap();
//            final Map<String, String> applitoolsArtifacts = ApplitoolsBuildWrapper.getApplitoolsArtifactList(run, listener);
            ApplitoolsCommon.buildEnvVariablesForExternalUsage(overrides, run, listener, step.getServerURL(), step.getApplitoolsApiKey());

            body = getContext().newBodyInvoker()
                    .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class), new ApplitoolsEnvironmentExpander(overrides)))
                    .withCallback(new BodyExecutionCallback() {
                        @Override
                        public void onStart(StepContext context) {
                            try {
                                ApplitoolsCommon.integrateWithApplitools(run, step.getServerURL(), step.getNotifyByCompletion(), step.getApplitoolsApiKey());
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
                                ApplitoolsCommon.closeBatch(run, listener, step.getServerURL(), step.getNotifyByCompletion(), step.getApplitoolsApiKey());
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
        public void stop(@Nonnull Throwable cause) throws Exception {
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

        @Override public String getDisplayName() {
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
        public ApplitoolsStep newInstance(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            return new ApplitoolsStep(formData.getString("serverURL"), formData.getBoolean("notifyByCompletion"), formData.getString("applitoolsApiKey"));
        }

    }
}
