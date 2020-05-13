package com.applitools.jenkins;

import hudson.model.AbstractBuild;
import hudson.model.Run;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hudson.model.JobProperty;
import jenkins.model.ArtifactManager;
import jenkins.tasks.SimpleBuildWrapper;
import jenkins.util.VirtualFile;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.graph.FlowNode;


/**
 * Encapsulates the Applitools' status display action.
 */
public class ApplitoolsStatusDisplayAction extends AbstractApplitoolsStatusDisplayAction {
    private static final String TIMESTAMP_PATTERN = "yyyyMMddHHmmss";
    private String projectName;
    private int buildNumber;
    private Calendar buildTimestamp;
    private String serverURL;
    private Run build;
    private Map<String, String> applitoolsValuesFromArtifacts;
    private static final Logger logger = Logger.getLogger(ApplitoolsStatusDisplayAction.class.getName());

    public ApplitoolsStatusDisplayAction(Run build) {
        this.projectName = build.getParent().getDisplayName();
        this.buildNumber = build.getNumber();
        this.buildTimestamp = build.getTimestamp();
        this.serverURL = null;
        this.build = build;
        this.applitoolsValuesFromArtifacts = new HashMap();
        for (Object property : build.getParent().getAllProperties()) {
            if (property instanceof ApplitoolsProjectConfigProperty) {
                this.serverURL = ((ApplitoolsProjectConfigProperty) property).getServerURL();
                break;
            }
        }
//        this.iframeText = getIframeText();
    }


    @Override
    public String getIframeText() {
        this.applitoolsValuesFromArtifacts = ApplitoolsCommon.checkApplitoolsArtifacts(this.build.getArtifacts(), this.build.getArtifactManager().root());
        try{
            String iframeURL = generateIframeURL();
            if (iframeURL == null)
            {
                // In case Applitools support has been removed from the project,
                // remove iframes from old reports
                return "";
            }

            return "<iframe id=\"frame\" src=\"" + iframeURL +
                    "\" style=\"overflow:hidden;overflow-x:hidden;overflow-y:hidden;height:710px;width:1024px;max-width:100%;resize:vertical;\"></iframe>\n";
        }
        catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.warning(sw.toString());
            return "";
        }
    }

    private String generateBatchId()
    {
        return generateBatchId(this.projectName, this.buildNumber, this.buildTimestamp, this.applitoolsValuesFromArtifacts);
    }

    private String generateIframeURL()
    {
        if (serverURL == null || serverURL.isEmpty())
        {
            // In case Applitools support has been removed from the project
            return null;
        }

        return serverURL + "/app/batchesnoauth/?startInfoBatchId=" + generateBatchId() + "&hideBatchList=true&intercom=false";
    }

//    private void checkApplitoolsArtifacts() {
//        this.applitoolsValuesFromArtifacts = new HashMap();
//        ArtifactManager artifactManager = build.getArtifactManager();
//        List<Run.Artifact> artifactList = build.getArtifacts();
//        if (artifactList.size() > 0 && artifactManager != null) {
//            for (Run.Artifact artifact : artifactList) {
//                String artifactFileName = artifact.getFileName();
//                Matcher m = artifactRegexp.matcher(artifactFileName);
//                if (m.find()) {
//                  String artifactName = m.group(1);
//                  try {
//                      InputStream stream = artifactManager.root().child(artifactFileName).open();
//                      String value = IOUtils.toString(stream, StandardCharsets.UTF_8.name()).replaceAll(System.getProperty("line.separator"), "");
//                      this.applitoolsValuesFromArtifacts.put(artifactName, value);
//                  } catch (java.io.IOException e) {
//                      logger.warning("Could't get artifact " + artifactFileName + "." + e.getMessage());
//                  }
//                }
//            }
//        }
//    }

    public static String generateBatchId(String projectName, int buildNumber, Calendar buildTimestamp) {
      return generateBatchId(projectName, buildNumber, buildTimestamp, null);
    }

    public static String generateBatchId(String projectName, int buildNumber, Calendar buildTimestamp, Map<String, String> applitoolsValuesFromArtifacts)
    {
        if (applitoolsValuesFromArtifacts != null && applitoolsValuesFromArtifacts.containsKey(ApplitoolsEnvironmentUtil.APPLITOOLS_BATCH_ID)) {
            return applitoolsValuesFromArtifacts.get(ApplitoolsEnvironmentUtil.APPLITOOLS_BATCH_ID);
        } else {
            final String BATCH_ID_PREFIX = "jenkins";
            SimpleDateFormat buildDate = new SimpleDateFormat(TIMESTAMP_PATTERN);
            buildDate.setTimeZone(buildTimestamp.getTimeZone());

            return BATCH_ID_PREFIX + "-" + projectName + "-" + buildNumber + "-" + buildDate.format(buildTimestamp.getTime());
        }
    }


}

