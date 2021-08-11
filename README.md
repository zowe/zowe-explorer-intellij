# For Mainframe
"For Mainframe" brings support for browsing, editing and creating data on z/OS via 
<a href="https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.4.0/com.ibm.zos.v2r4.izua700/V2R4/zosmf/izua700/IZUHPINFO_RESTServices.htm">z/OSMF REST API</a>.

Plugin in Marketplace: [link](https://plugins.jetbrains.com/plugin/16353-for-mainframe)

Using our plugin you will be able to:
* Start working with z/OS easily with no complex configurations.
* Organise datasets on z/OS, files on USS into Working Sets.
* Allocate datasets, create members, files and directories with different permissions.
* Perform renaming, copying and moving data in a modern way.
* Edit datasets, files and members. Smart auto-save will keep your content both in the editor and on the mainframe in-sync.
* Create multiple connections to different z/OS systems.
* All Intellij supported languages will be automatically highlighted and recognized once opened from the mainframe.

To start using the plugin:
* Install the plugin in your Intellij based IDE.
* Contact your RACF administrator so that your user is in the 
  <a href="https://www.ibm.com/support/knowledgecenter/SSLTBW_2.4.0/com.ibm.zos.v2r4.izua300/V2R4/zosmf/izua300/izulite_SecurityStructuresForZosmf.htm">IZUUSER RACF group</a>.
* You are ready to go! Start working with z/OS from the IDE.

**Note: z/OS 2.1 or higher is required**

# User Guide

## Introduction
"For Mainframe" is an IntelliJ plugin dedicated to facilitate working with z/OS datasets through providing a possibility to work with them in the IDE.

## Getting started
After installing plugin into your IDE from marketplace, the first thing you need to do is to create a connection and a working set.
Create, edit and delete a connection

### Create, edit and delete a connection
To create a connection press the "wrench" pictogram on the right side of your screen, or go to "Settings" (CTRL+ALT+S) and select "For Mainframe" on the left side of the panel.

In "Settings" switch to "z/OSMF connections" tab and press "+" at the bottom of the panel. A new panel "Add Connection" will appear.

Enter your desired connection name, connection URL, username and password into corresponding fields. The plugin provides a possibility to accept self-signed SSL-certificates, so check this box if necessary. Press "OK" when you're done. If the connection is created successfully you'll see it in the list in "Settings" and in the list on the right side of your screen after you've closed "Settings".

You can edit the connection in "Settings" by clicking on it and then on the "pencil" pictogram at the bottom of the panel.

You can delete working sets in "Settings" by clicking on the connection you'd like to delete and pressing "-" at the bottom of the panel.
### Create, edit and delete a working set
To add a working set press on the "wrench" pictogram on the right side of your screen, or go to "Settings" (CTRL+ALT+S) and select "For Mainframe" on the left side of the panel.

In "Settings" switch to "Working Sets" tab and press "+" at the bottom of the panel. A new panel "Add Working Set" will appear. In it you should enter your desired working set name, specify the existing connection, and add one or more data set masks. Press "OK" when you're done. You will see your newly connected working set in the list in "Settings" and on the right side of your screen after you've closed "Settings".

You can edit working sets in "Settings" by clicking on the desired set and then on the "pencil" pictogram at the bottom of the panel.

You can delete working sets in "Settings" by clicking on the working set you'd like to delete and pressing "-" at the bottom of the panel.

## Working with data sets
"For Mainframe" provides a number of features for working with z\OS data sets. With the plugin you can create a data set, add a member to a library-type data set, rename a data set or a data set member, view properties of a data set or a data set member, and delete a data set or a data set member.

### Add a data set
Click with the right mouse button on an existing working set in the working set tree on the right side of your screen. Click "New" → "Dataset". Input the desired parameters in the panel that pops up and press "OK". If the data set was created successfully you will see it in the working set tree on the right side of the screen. Library-type datasets are displayed as folders with their members as files. Other types of data sets are displayed as files.

The plugin doesn't provide a possibility to create PDSE data set.

### Add a member to a library data set
Click with the right mouse button on an existing library-type data set in the working set tree on the right side of your screen. Click "New" → "Member". Enter the desired member name in the window that pops up and press "OK". You should see your newly created member in the working set set tree under its containing data set.

### Rename a data set/data set member
Click with the right mouse button on the existing data set or data set member that you want to rename in the working set tree on the right side of your screen. Click "Rename". Enter the new name in the panel that pops up and press "OK". The data set/data set member you renamed should appear under the new name in the working set tree.

### View properties of a data set/data set member
Click with the right mouse button on the existing data set which properties you'd like to view. Click "Properties". A pop up window should appear where you would see all the available properties for the data set.

### Delete data set/data set member
Click with the right mouse button on the data set/data set member you'd like to delete in the working set tree on the right side of your screen. Click "Delete".Confirm your intention to delete a data set/data set member in the pop up window by pressing "Yes". After the deleting is complete you should no longer see the data set/data set member in the working set tree.

