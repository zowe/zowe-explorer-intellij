# IntelliJ Plugin Changelog

All notable changes to the Zowe IntelliJ Plugin will be documented in this file.

## `0.7.0 (2022-10-25)`

* Feature: Configurable batch size to load filter smoothly ([928baba](https://github.com/zowe/zowe-explorer-intellij/commit/928baba))
* Feature: Job Purge operation ([95bf7eb](https://github.com/zowe/zowe-explorer-intellij/commit/95bf7eb))
* Feature: Job Edit operation  ([b3962de](https://github.com/zowe/zowe-explorer-intellij/commit/b3962de))
* Feature: Copy local to remote ([d9dcdd2](https://github.com/zowe/zowe-explorer-intellij/commit/d9dcdd2), [ea92b4b](https://github.com/zowe/zowe-explorer-intellij/commit/ea92b4b))
* Feature: Copy remote to remote ([586a7f2](https://github.com/zowe/zowe-explorer-intellij/commit/586a7f2))
* Feature: GitHub issue #10: Edit Working sets directly from Tool Window ([63a2e4f](https://github.com/zowe/zowe-explorer-intellij/commit/63a2e4f))
* Feature: GitHub issue #70: Add date and time to JES Explorer ([eaea2cc](https://github.com/zowe/zowe-explorer-intellij/commit/eaea2cc))
* Feature: Copy remote to local: clarify warning ([26bbb2c](https://github.com/zowe/zowe-explorer-intellij/commit/26bbb2c))
* Feature: GitHub issue #67: Allocate like for datasets with BLK will be with warning ([7ae6261](https://github.com/zowe/zowe-explorer-intellij/commit/7ae6261))
* Feature: Move the file attribute conversion to a separate thread ([975f75d](https://github.com/zowe/zowe-explorer-intellij/commit/975f75d))
* Feature: Source code documentation added ([636411e](https://github.com/zowe/zowe-explorer-intellij/commit/636411e), [11bb7dd](https://github.com/zowe/zowe-explorer-intellij/commit/11bb7dd))


* Bugfix: File cache conflict if open JCL to edit it in JES explorer second time ([b3962de](https://github.com/zowe/zowe-explorer-intellij/commit/b3962de))
* Bugfix: GitHub issue #86: Incorrect error message if mask length > 44 ([cfb4ab6](https://github.com/zowe/zowe-explorer-intellij/commit/cfb4ab6))
* Bugfix: GitHub issue #87: Masks type autodetection does not work in Add/Edit Working Set dialogs ([49fc53a](https://github.com/zowe/zowe-explorer-intellij/commit/49fc53a))
* Bugfix: Problem with automatic refresh after creating new members/deleting members from dataset ([928baba](https://github.com/zowe/zowe-explorer-intellij/commit/928baba))
* Bugfix: Confusing dialog title 'Rename Directory' when renaming USS mask from context menu ([1e1a147](https://github.com/zowe/zowe-explorer-intellij/commit/1e1a147))
* Bugfix: GitHub issue #81: There is no difference between upper and lower cases when create USS masks from context menu ([f8ea3e9](https://github.com/zowe/zowe-explorer-intellij/commit/f8ea3e9))
* Bugfix: GitHub issue #88: Lower case is not changed to upper case during Job Filter creation ([c2f5b01](https://github.com/zowe/zowe-explorer-intellij/commit/c2f5b01))
* Bugfix: GitHub issue #44: 'Sync data' button does not work properly when multiple changes in USS file ([27f9c6a](https://github.com/zowe/zowe-explorer-intellij/commit/27f9c6a))
* Bugfix: GitHub issue #30: Create new member in dataset that does not have enough space creates empty member despite of warning ([7a649e6](https://github.com/zowe/zowe-explorer-intellij/commit/7a649e6))
* Bugfix: GitHub issue #54: Accumulation of errors in WS that breaks WS ([8648da2](https://github.com/zowe/zowe-explorer-intellij/commit/8648da2))
* Bugfix: USS file cannot be deleted in development branch ([8886770](https://github.com/zowe/zowe-explorer-intellij/commit/8886770))
* Bugfix: z/OS version specified in connection information doesn't match the z/OS version returned from z/OSMF ([1148e10](https://github.com/zowe/zowe-explorer-intellij/commit/1148e10))
* Bugfix: IDE error with ReadOnlyModificationException when set 'use binary mode' for read only uss-file ([c2ebf6a](https://github.com/zowe/zowe-explorer-intellij/commit/c2ebf6a))
* Bugfix: GitHub issue #94: SYSPRINT I looked at first always opens in JES explorer for a job with multiple steps ([301012a](https://github.com/zowe/zowe-explorer-intellij/commit/301012a))
* Bugfix: IDE error with CallException when try to open uss-file to which you have no access ([78650b9](https://github.com/zowe/zowe-explorer-intellij/commit/78650b9))
* Bugfix: The content of sequential dataset/member is changed anyway even if you choose do not sync data with mainframe ([559b05e](https://github.com/zowe/zowe-explorer-intellij/commit/559b05e))
* Bugfix: IDE error while retrieving job list in JES Explorer ([e3dfe93](https://github.com/zowe/zowe-explorer-intellij/commit/e3dfe93))
* Bugfix: Extra item 'Rename' is active in the context menu if click on 'loading...'/'load more' in file explorer ([78ab43f](https://github.com/zowe/zowe-explorer-intellij/commit/78ab43f)) 
* Bugfix: GitHub issue #16: Error creating zOSMF connection
* Bugfix: GitHub issue #85: The windows 'Add Working Set'/'Edit Working Set' are automatically resized if z/OSMF connection with very long name is added
