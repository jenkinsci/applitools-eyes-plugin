package com.applitools.jenkins;

import hudson.Extension;
import hudson.util.Secret;
import javaposse.jobdsl.dsl.helpers.wrapper.WrapperContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

@Extension(optional = true)
public class ApplitoolsJobDsl extends ContextExtensionPoint {
    @DslExtensionMethod(context = WrapperContext.class)
    public Object applitools(String serverUrl) {
        return applitools(serverUrl, ApplitoolsCommon.NOTIFY_ON_COMPLETION);
    }

    @DslExtensionMethod(context = WrapperContext.class)
    public Object applitools(String serverUrl, boolean notifyOnCompletion) {
        return new ApplitoolsBuildWrapper(
                serverUrl, notifyOnCompletion, null, false, false);
    }

    @DslExtensionMethod(context = WrapperContext.class)
    public Object applitools(String serverUrl, boolean notifyOnCompletion, Secret apiKey) {
        return new ApplitoolsBuildWrapper(
                serverUrl, notifyOnCompletion, apiKey, false, false);
    }


    @DslExtensionMethod(context = WrapperContext.class)
    public Object applitools(String serverUrl, Secret apiKey) {
        return new ApplitoolsBuildWrapper(
                serverUrl, true, apiKey, false, false);
    }

    @DslExtensionMethod(context = WrapperContext.class)
    public Object applitools() {
        return new ApplitoolsBuildWrapper(
                ApplitoolsCommon.APPLITOOLS_DEFAULT_URL,
                ApplitoolsCommon.NOTIFY_ON_COMPLETION,
                null, false, false);
    }

}
