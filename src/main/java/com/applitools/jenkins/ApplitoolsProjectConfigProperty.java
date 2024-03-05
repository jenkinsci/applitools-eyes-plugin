package com.applitools.jenkins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Job;

import java.io.Serializable;

/**
 * Encapsulates Applitools plugin configuration.
 */
public class ApplitoolsProjectConfigProperty extends JobProperty<AbstractProject<?, ?>> implements Serializable{
    private String serverURL;
    private boolean notifyOnCompletion;
    private String applitoolsApiKey;
    private boolean dontCloseBatches;
    private boolean eyesScmIntegrationEnabled;

    public ApplitoolsProjectConfigProperty(String serverURL, boolean notifyOnCompletion, String applitoolsApiKey,
                                           boolean dontCloseBatches, boolean eyesScmIntegrationEnabled) {
        this.serverURL = serverURL;
        this.notifyOnCompletion = notifyOnCompletion;
        this.applitoolsApiKey = applitoolsApiKey;
        this.dontCloseBatches = dontCloseBatches;
        this.eyesScmIntegrationEnabled = eyesScmIntegrationEnabled;
    }

    public String getApplitoolsApiKey() {
        return applitoolsApiKey;
    }

    public void setApplitoolsApiKey(String value) {
        this.applitoolsApiKey = value;
    }

    public String getServerURL()
    {
        return this.serverURL;
    }

    public void setServerURL(String serverURL)
    {
        this.serverURL = serverURL;
    }

    public boolean getNotifyOnCompletion() { return this.notifyOnCompletion; }

    public void setNotifyOnCompletion(boolean value) { this.notifyOnCompletion = value; }

    public boolean getDontCloseBatches() {
        return dontCloseBatches;
    }

    public void setDontCloseBatches(boolean dontCloseBatches) {
        this.dontCloseBatches = dontCloseBatches;
    }

    public boolean getEyesScmIntegrationEnabled() {
        return eyesScmIntegrationEnabled;
    }

    public void setEyesScmIntegrationEnabled(boolean eyesScmIntegrationEnabled) {
        this.eyesScmIntegrationEnabled = eyesScmIntegrationEnabled;
    }

    @Override
    public JobPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    /* even though we are not displaying it, it should be there*/
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    public static class DescriptorImpl extends JobPropertyDescriptor {

        public DescriptorImpl() {
            super(ApplitoolsProjectConfigProperty.class);
            load();
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Set Applitools URL";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }
    }

}
