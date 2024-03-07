package com.applitools.jenkins;

import hudson.model.Run;
import org.apache.commons.lang.mutable.MutableBoolean;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Encapsulates the Applitools' status display action.
 */
public class ApplitoolsStatusDisplayAction extends AbstractApplitoolsStatusDisplayAction {
    private static final String TIMESTAMP_PATTERN = "yyyyMMddHHmmss";
    private final String projectName;
    private final int buildNumber;
    private final Calendar buildTimestamp;
    private String serverURL;
    @SuppressWarnings("rawtypes")
    private final Run build;
    private Map<String, String> applitoolsValuesFromArtifacts;
    private static final Logger logger = Logger.getLogger(ApplitoolsStatusDisplayAction.class.getName());
    private boolean scmIntegrationEnabled;

    @SuppressWarnings("rawtypes")
    public ApplitoolsStatusDisplayAction(Run build) {
        this.projectName = build.getParent().getDisplayName();
        this.buildNumber = build.getNumber();
        this.buildTimestamp = build.getTimestamp();
        this.serverURL = null;
        this.build = build;
        this.applitoolsValuesFromArtifacts = new HashMap<>();
        for (Object property : build.getParent().getAllProperties()) {
            if (property instanceof ApplitoolsProjectConfigProperty) {
                this.serverURL = ((ApplitoolsProjectConfigProperty) property).getServerURL();
                this.scmIntegrationEnabled = ((ApplitoolsProjectConfigProperty) property).getEyesScmIntegrationEnabled();
                break;
            }
        }
    }


    @Override
    public String getIframeText() {
        this.applitoolsValuesFromArtifacts =
                ApplitoolsCommon.checkApplitoolsArtifacts(
                        this.build.getArtifacts(),
                        this.build.getArtifactManager().root());
        try {
            String iframeURL = generateIframeURL();
            if (iframeURL == null) {
                // In case Applitools support has been removed from the project,
                // remove iframes from old reports
                return "";
            }

            return "<iframe id=\"frame\" src=\"" + iframeURL +
                    "\" style=\"overflow:hidden;overflow-x:hidden;overflow-y:hidden;height:710px;width:1024px;max-width:100%;resize:vertical;\"></iframe>\n";
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.warning(sw.toString());
            return "";
        }
    }

    private String generateBatchId() {
        return generateBatchId(ApplitoolsCommon.getEnv(), this.projectName, this.buildNumber,
                this.buildTimestamp, this.applitoolsValuesFromArtifacts, null, this.scmIntegrationEnabled);
    }

    private String generateIframeURL() {
        if (serverURL == null || serverURL.isEmpty()) {
            // In case Applitools support has been removed from the project
            return null;
        }

        return serverURL + "/app/batchesnoauth/?startInfoBatchId=" + generateBatchId() + "&hideBatchList=true&intercom=false&agentId=eyes-jenkins-" + ApplitoolsCommon.getPluginVersion();
    }

    public static String generateBatchId(Map<String, String> env, String projectName, int buildNumber,
                                         Calendar buildTimestamp, Map<String, String> applitoolsValuesFromArtifacts) {
        return generateBatchId(env, projectName, buildNumber, buildTimestamp, applitoolsValuesFromArtifacts,
                null, false);
    }

    public static String generateBatchId(Map<String, String> env, String projectName, int buildNumber,
                                         Calendar buildTimestamp, Map<String, String> applitoolsValuesFromArtifacts,
                                         MutableBoolean isCustom, boolean scmIntegrationEnabled) {
        if (!scmIntegrationEnabled) {
            if (applitoolsValuesFromArtifacts != null &&
                    applitoolsValuesFromArtifacts.containsKey(ApplitoolsEnvironmentUtil.APPLITOOLS_BATCH_ID)) {
                return applitoolsValuesFromArtifacts.get(ApplitoolsEnvironmentUtil.APPLITOOLS_BATCH_ID);
            } else if (env != null) {
                String batchId = env.get("APPLITOOLS_BATCH_ID");
                if (batchId != null) {
                    ApplitoolsBuildWrapper.isCustomBatchId = true;
                    if (isCustom != null) {
                        isCustom.setValue(true);
                        if (applitoolsValuesFromArtifacts != null) {
                            applitoolsValuesFromArtifacts.put(ApplitoolsEnvironmentUtil.APPLITOOLS_BATCH_ID, batchId);
                        }
                    }
                    return batchId;
                }
            }
        }

        final String BATCH_ID_PREFIX = "jenkins";
        SimpleDateFormat buildDate = new SimpleDateFormat(TIMESTAMP_PATTERN);
        buildDate.setTimeZone(buildTimestamp.getTimeZone());

        return BATCH_ID_PREFIX + "-" + projectName + "-" + buildNumber + "-" + buildDate.format(buildTimestamp.getTime());
    }
}

