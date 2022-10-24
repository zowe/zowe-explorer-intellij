# IntelliJ Plugin Changelog

All notable changes to the Zowe IntelliJ Plugin will be documented in this file.

## `0.7.0 (2022-10-25)`

<!--* Feature: test ([f3aeafa](https://github.com/zowe/api-layer/commit/f3aeafa)), closes [#2554](https://github.com/zowe/api-layer/issues/2554)-->
* Feature: Configurable batch size to load filter smoothly 
* Feature: Job Purge operation
* Feature: Job Edit operation
* Feature: Copy local to remote
* Feature: GitHub issue #10: Edit Working sets directly from Tool Window
* Feature: GitHub issue #70: Add date and time to JES Explorer
* Feature: GitHub issue #41: Transfer USS files from any encoding, not just from EBCDIC
* Feature: JES filter improvement
* Feature: Copy remote to local: clarify warning
* Feature: GitHub issue #67: Allocate like for datasets with BLK will be with warning
* Feature: Move the file attribute conversion to a separate thread
* Feature: Source code documentation added

<!--* Bugfix: snakeyml update, scheme validation fix (#2577) ([ae48669](https://github.com/zowe/api-layer/commit/ae48669)), closes [#2577](https://github.com/zowe/api-layer/issues/2577)-->
* Bugfix: File cache conflict if open JCL to edit it in JES explorer second time
* Bugfix: GitHub issue #86: Incorrect error message if mask length > 44
* Bugfix: GitHub issue #87: Masks type autodetection does not work in Add/Edit Working Set dialogs
* Bugfix: Problem with automatic refresh after creating new members/deleting members from dataset
* Bugfix: GitHub issue #89: Impossible to rename USS directory whose name contains &
* Bugfix: Confusing dialog title 'Rename Directory' when renaming USS mask from context menu
* Bugfix: GitHub issue #81: There is no difference between upper and lower cases when create USS masks from context menu
* Bugfix: GitHub issue #88: Lower case is not changed to upper case during Job Filter creation
* Bugfix: GitHub issue #44: 'Sync data' button does not work properly when multiple changes in USS file
* Bugfix: GitHub issue #30: Create new member in dataset that does not have enough space creates empty member despite of warning
* Bugfix: GitHub issue #54: Accumulation of errors in WS that breaks WS
* Bugfix: USS file cannot be deleted in
* Bugfix: z/OS version specified in connection information doesn't match the z/OS version returned from z/OSMF
* Bugfix: GitHub issue #16: Error creating zOSMF connection
