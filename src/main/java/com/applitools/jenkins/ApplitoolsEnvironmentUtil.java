package com.applitools.jenkins;

import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.Map;

/**
 * Utility class for environment variables.
 */
public class ApplitoolsEnvironmentUtil {
    public static final String APPLITOOLS_BATCH_NAME = "BATCH_NAME";
    public static final String APPLITOOLS_BATCH_ID = "BATCH_ID";
    public static final String APPLITOOLS_PROJECT_SERVER_URL = "SERVER_URL";
    public static final String APPLITOOLS_BATCH_SEQUENCE = "BATCH_SEQUENCE";
    public static final String APPLITOOLS_DONT_CLOSE_BATCHES = "DONT_CLOSE_BATCHES";
    public static final String EYES_SCM_INTEGRATION_ENABLED = "SCM_INTEGRATION_ENABLED";
    public static final String APPLITOOLS_API_KEY = "API_KEY";
    public static final String TRUE_VALUE = "true";

    public static void outputVariables(final TaskListener listener, Run<?, ?> build,
                                       Map<String, String> env, String serverURL, String batchName,
                                       String batchId, String projectName, String applitoolsApiKey) {
        listener.getLogger().println("Creating Applitools environment variables:");

        outputEnvironmentVariable(listener, env, APPLITOOLS_DONT_CLOSE_BATCHES, TRUE_VALUE, true);

        if (serverURL != null && !serverURL.isEmpty()) {
            outputEnvironmentVariable(listener, env, APPLITOOLS_PROJECT_SERVER_URL, serverURL, true);
        }

        if (batchId != null && !batchId.isEmpty()) {
            outputEnvironmentVariable(listener, env, APPLITOOLS_BATCH_ID, batchId, true);
        }

        if (batchName != null && !batchName.isEmpty()) {
//            String customersBatchId = process.env
            outputEnvironmentVariable(listener, env, APPLITOOLS_BATCH_NAME, batchName, true);
        }

        if (projectName != null) {
            outputEnvironmentVariable(listener, env, APPLITOOLS_BATCH_SEQUENCE, projectName, true);
        }

        if (applitoolsApiKey != null && !applitoolsApiKey.isEmpty()) {
            outputEnvironmentVariable(listener, env, APPLITOOLS_API_KEY, applitoolsApiKey, true);
        }
    }

    public static void outputEnvironmentVariable(final TaskListener listener, Map<String, String> env, String key, String value, boolean overwrite) {
        String prefix = "APPLITOOLS_";

        if (env.get(key) == null || overwrite) {
            String keyName = prefix + key;
            env.put(keyName, value);
            listener.getLogger().println(keyName + " = '" + value + "'");
        }
    }

}
