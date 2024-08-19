# Zowe™ Explorer plug-in for IntelliJ IDEA™

[![Downloads](https://img.shields.io/jetbrains/plugin/d/18688-zowe-explorer)](https://plugins.jetbrains.com/plugin/18688-zowe-explorer)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/18688-zowe-explorer)](https://plugins.jetbrains.com/plugin/18688-zowe-explorer)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=zowe_zowe-explorer-intellij&metric=coverage)](https://sonarcloud.io/dashboard?id=zowe_zowe-explorer-intellij)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=zowe_zowe-explorer-intellij&metric=alert_status)](https://sonarcloud.io/dashboard?id=zowe_zowe-explorer-intellij)

"Zowe™ Explorer" brings support for browsing, editing and creating data on z/OS
via [z/OSMF REST API](https://www.ibm.com/docs/en/zos/2.4.0?topic=guide-using-zosmf-rest-services).

Plugin in Marketplace: [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/18688-zowe-explorer)

Using our plugin you will be able to:

* Start working with z/OS easily with no complex configurations.
* Organize datasets on z/OS, files on USS into Working Sets.
* Allocate datasets, create members, files and directories with different permissions.
* Perform renaming, copying and moving data in a modern way.
* Edit datasets, files and members. Smart auto-save will keep your content both in the editor and on the mainframe
  in-sync.
* Create multiple connections to different z/OS systems.
* Perform all available operations with Jobs.
* Highlight all IntelliJ supported languages automatically and recognize them once opened from the mainframe.

To start using the plugin:

* Install the plugin in your IntelliJ IDEA™ platform based IDE.
* Contact your RACF administrator so that your user is in
  the [IZUUSER RACF group](https://www.ibm.com/docs/en/zos/2.4.0?topic=guide-security-structures-zosmf)
* You are ready to go! Start working with z/OS from the IDE.

**Note: z/OS 2.1 or higher is required**

## Where to find the docs

[Zowe™ Explorer plug-in for IntelliJ IDEA™ FAQ](https://docs.zowe.org/stable/getting-started/zowe_faq#zowe-intellij-plug-in-incubator-faq)

[Installing Zowe™ Explorer plug-in for IntelliJ IDEA™](https://docs.zowe.org/stable/user-guide/intellij-install)

[Configuring Zowe™ Explorer plug-in for IntelliJ IDEA™](https://docs.zowe.org/stable/user-guide/intellij-configure)

[Using Zowe™ Explorer plug-in for IntelliJ IDEA™](https://docs.zowe.org/stable/user-guide/intellij-using)

[Troubleshooting Zowe™ Explorer plug-in for IntelliJ IDEA™](https://docs.zowe.org/stable/troubleshoot/troubleshoot-intellij)

[Contribution Guidelines](https://github.com/zowe/zowe-explorer-intellij/blob/main/CONTRIBUTING.md)

[Changelog](https://github.com/zowe/zowe-explorer-intellij/blob/main/CONTRIBUTING.md)

## Developer guide

- IntelliJ IDEA™ platform IDE version 2022.3 and later (Community will be enough)
- Java SDK 17 (IntelliJ IDEA™ platform IDE's built-in)

### Setup steps
- Clone the project repo:

``git clone git@github.com:zowe/zowe-explorer-intellij.git``

- Three options of working with the plugin are available:
    - **Run plugin** - run the plugin in development mode
    - **Package plugin** - make a zip portable package to install it in IntelliJ IDEA™ platform IDE or publish somewhere
    - **Run tests** - run plugin tests to check the codebase with automated tests
- Proceed to [Contribution Guidelines](#how-to-contribute) to develop some new functionality for the project.

## How to run tests

We have two options of tests:

1. UI tests - run with open IDE, make test of user-like interaction with the plugin;
2. Unit tests - automated headless bundle to test plugin functions as if they were a separate pieces.

### Environment configurations for UI tests:

1. In IntelliJ Idea change Settings => Tools => Terminal Shell path parameter from PowerShell to Git Bash. Example: "C:
   \Program Files\Git\usr\bin\bash.exe" --login -i
2. Make Java version 17 available from command line (add to PATH)

### To run UI tests:

1. change values for ZOS_USERID, ZOS_PWD, CONNECTION_URL in src/uiTest/kotlin/auxiliary/utils.kt
2. run ./gradlew buildPlugin in console
4. run the script uiTest.sh
5. once IdeForUiTests started make it as main window on the screen and do not touch mouse anymore
6. on first launch a ide_for_launch folder will be created and ide will be downloaded

UI tests results: build/reports/tests/uiTest/index.html

### To run smoke test:

1. change values for ZOS_USERID, ZOS_PWD, CONNECTION_URL in src/uiTest/kotlin/auxiliary/utils.kt
2. run the script smokeTest.sh
3. if unit tests fail, smoke ui test will be skipped. When unit tests are successful, IdeForUiTests will be run
4. once IdeForUiTests started make it as main window on the screen and do not touch mouse anymore

Smoke test results: build/reports/tests/test/index.html with report for unit tests,
build/reports/tests/SUCCESS(FAILURE).txt with quick summary for unit test run (file name depends on test run result),
build/reports/tests/smokeUiTest/index.html with report for smoke UI test

### To run unit tests:

"Unit tests" Gradle task: just run it as a configuration option.
If you want to run a separate unit test, you should consider to use "Kotest" plugin.
Firstly, you need to download it. Then, go to "Edit Configurations..." -> "Edit configuration templates..." -> "Kotest".
In there, you need to enable VM options and add the following lines:

```
-Didea.force.use.core.classloader=true 
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.nio.file=ALL-UNNAMED
--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED
--add-opens=java.desktop/java.awt=ALL-UNNAMED
--add-opens=java.desktop/java.awt.event=ALL-UNNAMED
--add-opens=java.desktop/javax.swing=ALL-UNNAMED
--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED
--add-opens=java.desktop/sun.awt=ALL-UNNAMED
--add-opens=java.desktop/sun.font=ALL-UNNAMED
```

After the "Kotest" is set up, just click on the green arrow near the test you want to run.

**In case of any troubles during plugin usage, feel free to contact us.**

## How to contribute

If you want to help the project, improve some functionality, resolve bug or add some new feature, please, refer to
the [contribution guide](CONTRIBUTING.md).

## How to obtain and provide feedback

If you have any questions, related to the project development, further plans or something else, you can reach as out by
some of the communication chanels:

* [For Mainframe Slack channel in IBA workspace](https://iba-mainframe-tools.slack.com/archives/C01V4MZL9DH)
* [Zowe Explorer IntelliJ team Slack channel in Open Mainframe Project workspace](https://openmainframeproject.slack.com/archives/C020BGPSU0M)
* [For Mainframe GitHub (create or review issues)](https://github.com/for-mainframe/For-Mainframe/issues)
* [Zowe Explorer plug-in for IntelliJ IDEA GitHub (create or review issues)](https://github.com/zowe/zowe-explorer-intellij/issues)
* Email to: <a href="mailto:ukalesnikau@ibagroup.eu">Uladzislau Kalesnikau (Team Lead of the Zowe Explorer IntelliJ squad)</a>
* Email to: <a href="mailto:aburak@ibagroup.eu">Alex Burak (our project manager)</a>

**Note: GitHub issue is the preferred way of communicating in case of creating some bug/feature/request for enhancement.
If you need direct consulting or you have any related questions, please, reach us out using Slack channels or E-mail**
