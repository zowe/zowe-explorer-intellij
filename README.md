# For Mainframe

"For Mainframe" brings support for browsing, editing and creating data on z/OS
via [z/OSMF REST API](https://www.ibm.com/docs/en/zos/2.4.0?topic=guide-using-zosmf-rest-services)

Plugin in Marketplace: [link](https://plugins.jetbrains.com/plugin/16353-for-mainframe)

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

# User Guide

## Introduction

"For Mainframe" is an IntelliJ plugin dedicated to facilitate working with z/OS datasets through providing a possibility
to work with them in the IDE.

## Getting started

After installing plugin into your IDE from marketplace, the first thing you need to do is to create a connection and a
working set.
Create, edit and delete a connection.

### Create, edit and delete a connection

To create a connection press the "wrench" pictogram on the right side of your screen, or go to "Settings" (CTRL+ALT+S)
and select "For Mainframe" on the left side of the panel.

In "Settings" switch to "z/OSMF connections" tab and press "+" at the bottom of the panel. A new panel "Add Connection"
will appear.

Enter your desired connection name, connection URL, username and password into corresponding fields. The plugin provides
a possibility to accept self-signed SSL-certificates, so check this box if necessary. Press "OK" when you're done. If
the connection is created successfully you'll see it in the list in "Settings" and in the list on the right side of your
screen after you've closed "Settings".

You can edit the connection in "Settings" by clicking on it and then on the "pencil" pictogram at the bottom of the
panel.

You can delete working sets in "Settings" by clicking on the connection you'd like to delete and pressing "-" at the
bottom of the panel.

### Create, edit and delete a working set

To add a working set press on the "wrench" pictogram on the right side of your screen, or go to "Settings" (CTRL+ALT+S)
and select "For Mainframe" on the left side of the panel.

In "Settings" switch to "Working Sets" tab and press "+" at the bottom of the panel. A new panel "Add Working Set" will
appear. In it you should enter your desired working set name, specify the existing connection, and add one or more data
set masks. Press "OK" when you're done. You will see your newly connected working set in the list in "Settings" and on
the right side of your screen after you've closed "Settings".

You can edit working sets in "Settings" by clicking on the desired set and then on the "pencil" pictogram at the bottom
of the panel.

You can delete working sets in "Settings" by clicking on the working set you'd like to delete and pressing "-" at the
bottom of the panel.

## Working with data sets

"For Mainframe" provides a number of features for working with z\OS data sets. With the plugin you can create a data
set, add a member to a library-type data set, rename a data set or a data set member, view properties of a data set or a
data set member, and delete a data set or a data set member.

### Add a data set

Click with the right mouse button on an existing working set in the working set tree on the right side of your screen.
Click "New" → "Dataset". Input the desired parameters in the panel that pops up and press "OK". If the data set was
created successfully you will see it in the working set tree on the right side of the screen. Library-type datasets are
displayed as folders with their members as files. Other types of data sets are displayed as files.

The plugin doesn't provide a possibility to create PDSE data set.

### Add a member to a library data set

Click with the right mouse button on an existing library-type data set in the working set tree on the right side of your
screen. Click "New" → "Member". Enter the desired member name in the window that pops up and press "OK". You should see
your newly created member in the working set set tree under its containing data set.

### Rename a data set/data set member

Click with the right mouse button on the existing data set or data set member that you want to rename in the working set
tree on the right side of your screen. Click "Rename". Enter the new name in the panel that pops up and press "OK". The
data set/data set member you renamed should appear under the new name in the working set tree.

### View properties of a data set/data set member

Click with the right mouse button on the existing data set which properties you'd like to view. Click "Properties". A
pop up window should appear where you would see all the available properties for the data set.

### Delete data set/data set member

Click with the right mouse button on the data set/data set member you'd like to delete in the working set tree on the
right side of your screen. Click "Delete". Confirm your intention to delete a data set/data set member in the pop up
window by pressing "Yes". After the deleting is complete you should no longer see the data set/data set member in the
working set tree.

# Developer guide

## Requirements

- IntelliJ IDEA version 2022.3 and later (Community will be enough)
- Java SDK 17 (IntelliJ built-in)

## Setup steps

- Clone the project repo:

``git clone git@github.com:for-mainframe/For-Mainframe.git``

- Three options of working with the plugin are available:
    - **Run plugin** - run the plugin in development mode
    - **Package plugin** - make a zip portable package to install it in IntelliJ IDEA or publish somewhere
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
2. run the script uiTest.sh
3. once IdeForUiTests started make it as main window on the screen and do not touch mouse anymore

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
--add-opens=java.desktop/java.awt=ALL-UNNAMED
--add-opens=java.desktop/sun.awt=ALL-UNNAMED
--add-opens=java.desktop/java.awt.event=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED 
```

After the "Kotest" is set up, just click on the green arrow near the test you want to run.

**In case of any troubles during plugin usage, feel free to contact us.**

# How to contribute

If you want to help the project, improve some functionality, resolve bug or add some new feature, please, refer to
the [contribution guide](CONTRIBUTING.md).

# How to obtain and provide feedback

If you have any questions, related to the project development, further plans or something else, you can reach as out by
some of the communication chanels:

* [For Mainframe Slack channel in IBA workspace](https://iba-mainframe-tools.slack.com/archives/C01V4MZL9DH)
* [Zowe Explorer IntelliJ Slack channel in Open Mainframe Project workspace](https://openmainframeproject.slack.com/archives/C020BGPSU0M)
* [For Mainframe GitHub (create or review issues)](https://github.com/for-mainframe/For-Mainframe/issues)
* [Zowe Explorer IntelliJ GitHub (create or review issues)](https://github.com/zowe/zowe-explorer-intellij/issues)
* Email to: <a href="mailto:ukalesnikau@ibagroup.eu">Uladzislau Kalesnikau (Team Lead of the IJMP)</a>

**Note: GitHub issue is the preferred way of communicating in case of creating some bug/feature/request for enhancement.
If you need direct consulting or you have any related questions, please, reach us out using Slack channels or E-mail**
