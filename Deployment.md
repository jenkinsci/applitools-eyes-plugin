# Jenkins Plugin Deployment

1. In `pom.xml` make sure you update the `version` tag.
2. In `ApplitoolsStatusDisplayAction` make sure to update the 
   version sent in the `agentId` query part of the url to the same version.
3. Make sure you have `~/.m2/settings.xml` filled with the content you get from
   the `curl` command:
   ```
    curl -u applitools:<password> https://repo.jenkins-ci.org/setup/settings.xml
   ```
   If you don't have the password, ask someone who have it, or look for it in the
   vault in Azure: *JenkinsPluginKV :: Secrets :: jenkins-plugin-password*
4. Run `mvn clean install deploy` (or click *deploy* in the maven lifecycle menu in IntelliJ IDEA IDE)
5. Notice it might take a couple of hours until you see the updated plugin in the plugins page.