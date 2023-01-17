# Zowe Explorer

[![Downloads](https://img.shields.io/jetbrains/plugin/d/18688-zowe-explorer)](https://plugins.jetbrains.com/plugin/18688-zowe-explorer)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/18688-zowe-explorer)](https://plugins.jetbrains.com/plugin/18688-zowe-explorer)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=zowe_zowe-explorer-intellij&metric=coverage)](https://sonarcloud.io/dashboard?id=zowe_zowe-explorer-intellij)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=zowe_zowe-explorer-intellij&metric=alert_status)](https://sonarcloud.io/dashboard?id=zowe_zowe-explorer-intellij)

"Zowe Explorer" brings support for browsing, editing and creating data on z/OS
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

* Install the plugin in your Intellij based IDE.
* Contact your RACF administrator so that your user is in
  the [IZUUSER RACF group](https://www.ibm.com/docs/en/zos/2.4.0?topic=guide-security-structures-zosmf)
* You are ready to go! Start working with z/OS from the IDE.

**Note: z/OS 2.1 or higher is required**

## Where to find the docs

[Zowe IntelliJ plug-in FAQ](https://docs.zowe.org/stable/getting-started/zowe_faq#zowe-intellij-plug-in-incubator-faq)

[Installing Zowe IntelliJ plug-in](https://docs.zowe.org/stable/user-guide/intellij-install)

[Configuring Zowe IntelliJ plug-in](https://docs.zowe.org/stable/user-guide/intellij-configure)

[Using Zowe IntelliJ plug-in](https://docs.zowe.org/stable/user-guide/intellij-using)

[Troubleshooting Zowe IntelliJ plug-in](https://docs.zowe.org/stable/troubleshoot/troubleshoot-intellij)

[Contribution Guidelines](https://github.com/zowe/zowe-explorer-intellij/blob/main/CONTRIBUTING.md)

[Changelog](https://github.com/zowe/zowe-explorer-intellij/blob/main/CONTRIBUTING.md)

## Developer guide

### Requirements
- IntelliJ IDEA version 2022.3 and later (Community will be enough)
- Java SDK 17 (IntelliJ built-in)

### Setup steps
- Clone the project repo:

``git clone git@github.com:zowe/zowe-explorer-intellij.git``

- Three options of working with the plugin are available:
    - **Run plugin** - run the plugin in development mode
    - **Package plugin** - make a zip portable package to install it in IntelliJ IDEA or publish somewhere
    - **Run tests** - run plugin tests to check the codebase with automated tests
- Proceed to [Contribution Guidelines](#how-to-contribute) to develop some new functionality for the project.

## How to run tests

We have two options of tests:

1. UI tests - run with open IDE, make test of user-like interaction with the plugin;
2. Unit tests - automated headless bundle to test plugin functions as if they were a separate pieces.

### To run unit tests:

"Unit tests" Gradle task: just run it as a configuration option.
If you want to run a separate unit test, you should consider to use "Kotest" plugin.
Firstly, you need to download it. Then, go to "Edit Configurations..." -> "Edit configuration templates..." -> "Kotest".
In there, you need to enable VM options and add the following options:

```
-Didea.force.use.core.classloader=true --add-exports java.base/jdk.internal.vm=ALL-UNNAMED
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
* [Zowe Explorer IntelliJ Slack channel in Open Mainframe Project workspace](https://openmainframeproject.slack.com/archives/C020BGPSU0M)
* [For Mainframe GitHub (create or review issues)](https://github.com/for-mainframe/For-Mainframe/issues)
* [Zowe Explorer IntelliJ GitHub (create or review issues)](https://github.com/zowe/zowe-explorer-intellij/issues)
* Email to: <a href="mailto:ukalesnikau@ibagroup.eu">Uladzislau Kalesnikau (Team Lead of the IJMP)</a>
* Email to: <a href="mailto:vkrus@ibagroup.eu">Valiantsin Krus (Tech Lead of the IJMP)</a>

**Note: GitHub issue is the preferred way of communicating in case of creating some bug/feature/request for enhancement.
If you need direct consulting or you have any related questions, please, reach us out using Slack channels or E-mail**
