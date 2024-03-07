## Applitools Eyes Plugin for Jenkins

This plugin adds [Applitools Eyes](https://applitools.com) test results to your Jenkins build report.

### Adding the Applitools Eyes plugin to your Jenkins CI:     
To add the Applitools plugin to Jenkins, use menu 

*Jenkins* => *Manage Jenkins* => *Manage Plugins*

then choose "Available Plugins" and search for "Applitools".

For more details see the [guide](https://www.jenkins.io/doc/book/managing/plugins/).      

### In case of a freestyle project
To enable Applitools support for a freestyle project, check the 'Applitools support'
on the **Build Environment** section on the projects' configuration page.

[More details are available here](https://plugins.jenkins.io/applitools-eyes/)

If you are using a dedicated Applitools Eyes server, update the Applitools URL accordingly 
(If you don't know if you're using a dedicated server, you are not using one).

### In case of a pipeline project

To use the Applitools plugin in a pipeline project, you need to add the Applitools() directive and put your run code in a block. Following is a script example:
```
node {
    stage('Applitools build') {
        Applitools() {
            sh 'mvn clean test'
        }
    }
}
```
If you are using a dedicated Applitools Eyes server, you should update the Applitools URL accordingly inside the Applitools directive. For example:

```
node {
    stage('Applitools build') {
        Applitools('https://myprivateserver.com') {
           sh 'mvn clean test'
        }
    }
}
```

### Special Arguments

* **`serverUrl`**: Defines a custom Eyes server URL (in case of a private cloud or on-prem Eyes)
* **`applitoolsApiKey`**: Define the API key for the Eyes account. Best practice is to use an env-var with `credentials` like this:
    ```
    environment{
        APPLITOOLS_API_KEY = credentials('APPLITOOLS_API_KEY')
    }
    ...
    stage{
        Applitools(applitoolsApiKey: "$APPLITOOLS_API_KEY")
    }
 
    ```
    You can find more info about setting Jenkins credentials [here](https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#secret-text)
* **`dontCloseBatches`**: If set to `true` the batch will not be closed, allowing other tests to be added to the same batch. If set to `false` the batch will be closed, and any other test with the same batch ID will be added to a new batch.
* **`eyesScmIntegrationEnabled`**: If set to `true`, the desired batch ID will be used internally, but a generated batch ID will be used externally. Use this if you want to use the SCM Integration feature in Eyes, which means you'll need to set your batch ID to the commit SHA. In a Jenkins Pipeline this can be done like this:
    ```
    environment {
        APPLITOOLS_BATCH_ID = "${env.GIT_COMMIT}"
    }
    ```
    or leave it unset alltogether which will look for the `GIT_COMMIT` env var for you.
* **`notifyOnCompletion`**: If set to `true`, will tell Eyes server that the batch was closed and it can send notifications about it as configured in the Eyes Dashboard Admin Panel.

### Updating Your Tests Code
Jenkins exports the batch ID to the APPLITOOLS_BATCH_ID environment variable. You need to update your tests code to use this ID in order for your tests to appear in the Applitools window report in Jenkins.

In addition, Jenkins exports a suggested batch name to the APPLITOOLS_BATCH_NAME environment variable. Using this batch name is optional (the batch name is used for display purposes only).

Following is a Java code example:
```
BatchInfo batchInfo = new BatchInfo(System.getenv("APPLITOOLS_BATCH_NAME"));

// If the test runs via Jenkins, set the batch ID accordingly.
String batchId = System.getenv("APPLITOOLS_BATCH_ID");
if (batchId != null) {
    batchInfo.setId(batchId);
}
eyes.setBatch(batchInfo);
```

If you have any questions or need any assistance in using the plugin, feel free to contact Applitools support at: support [at] applitools dot com.


### Explicitly setting the BATCH ID / Name 
The plugin exports a set of environment variables which are used by the Applitools SDK when a test is run, 
and later by the plugin itself to present the Applitools results in the build status page.
  
These environment variables can overriden during the build.
For example, to override the APPLITOOLS_BATCH_ID environment variable, place a value to the file ./.applitools/BATCH_ID (in the build root folder). 
If the file ./.applitools/BATCH_ID exists, the module will read it and export its value as APPLITOOLS_BATCH_ID environment variable. 
Also, it will be stored as a build artifact, which will later be will be used to display the Applitools test results.

The same goes for any other Applitools environment variable.   


### Building the plugin from source

To build the version, execute

```
mvn clean; mvn package
```

The built module appears in the ./target/applitools-eyes.hpi

To load this module go to the "Manage Plugins" page, then to "Advanced" and select this file.
