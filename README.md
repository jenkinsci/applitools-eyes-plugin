This project contains the source code for the Applitools Eyes Jenkins/Hudson plugin.

[![Build Status](https://jenkins.ci.cloudbees.com/job/plugins/job/applitools-eyes/badge/icon)](https://jenkins.ci.cloudbees.com/job/plugins/job/applitools-eyes-plugin/) [![codecov.io](https://codecov.io/github/applitools/jenkins-applitools-eyes-plugin/coverage.svg?branch=master)](https://codecov.io/github/applitools/eyes.jenkins?branch=master)

To build the version, execute

    mvn clean; mvn package
    
The built module appears in the file://./target/applitools-eyes.jar    
    
To install built package to your Jenkins, use menu Jenkins => Manage Jenkins => Manage Plugins. On the 'Advanced' tab download the applitools-eye.jar
For more details see the [guide](https://www.jenkins.io/doc/book/managing/plugins/)      

The Applitools support is available on project basis. To enable Applitools support for a project, check the 'Applitools support' on the **build environment** section on the projects' configuration page. [More details are available here](https://plugins.jenkins.io/applitools-eyes/) 

The plugin exports a set of environment variables, which are used by an Applitools test, and displays the Applitools tests results on the status page.
The APPLITOOLS_BATCH_ID environment variable might be overridden during the build. To override the APPLITOOLS_BATCH_ID environment variable place the new value to the file ./.applitools/BATCH_ID (In the build root folder). If the file ./.applitools/BATCH_ID exists, the module will read it and export its value as APPLITOOLS_BATCH_ID environment variable. Also, it will be stored as a build artifact, and its value will be used to display the Applitools test results.   
