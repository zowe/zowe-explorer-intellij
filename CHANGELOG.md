# IntelliJ Plugin Changelog

All notable changes to the Zowe IntelliJ Plugin will be documented in this file.

## `1.0.0 (2023-03-13)`

* Breaking: Java 17 usage introduced. Plugin requires to use it with IntelliJ version >= 2022.3


* Feature: GitHub issue #31: Support for CHMOD operation ([3a166173](https://github.com/zowe/zowe-explorer-intellij/commit/3a166173))
* Feature: GitHub issue #40: TSO CLI ([5ed36ede](https://github.com/zowe/zowe-explorer-intellij/commit/5ed36ede))
* Feature: GitHub issue #41: Transfer USS files from any encoding, not just from EBCDIC ([06695658](https://github.com/zowe/zowe-explorer-intellij/commit/06695658))
* Feature: GitHub issue #69: Plugin struggles with opening big files and dataset ([1d0e9436](https://github.com/zowe/zowe-explorer-intellij/commit/1d0e9436))
* Feature: GitHub issue #5: JES Explorer tab has delete option disabled and missing edit option ([a4544e98](https://github.com/zowe/zowe-explorer-intellij/commit/a4544e98))
* Feature: Write documentation to DataOpsManagerImpl ([6a847a00](https://github.com/zowe/zowe-explorer-intellij/commit/6a847a00))
* Feature: Add support to all IBM encodings ([9e8bee51](https://github.com/zowe/zowe-explorer-intellij/commit/9e8bee51))
* Feature: UI regression tests ([3cd30136](https://github.com/zowe/zowe-explorer-intellij/commit/3cd30136))
* Feature: GitHub issue #39: Manual sync proceeded in main thread ([47042a81](https://github.com/zowe/zowe-explorer-intellij/commit/47042a81))
* Feature: GitHub issue #73: Migrate to Kotlin DSL v2 ([d37dbcb0](https://github.com/zowe/zowe-explorer-intellij/commit/d37dbcb0))
* Feature: GitHub issue #22: Support code page for TSO ([b1b57867](https://github.com/zowe/zowe-explorer-intellij/commit/b1b57867))
* Feature: Create WS and masks in twice place ([ae881fd0](https://github.com/zowe/zowe-explorer-intellij/commit/ae881fd0))
* Feature: GitHub issue #28: Fix build warnings ([0dcaa4de](https://github.com/zowe/zowe-explorer-intellij/commit/0dcaa4de))
* Feature: Rewrite RenameDialog to avoid calling validation setters in rename actions ([67e71974](https://github.com/zowe/zowe-explorer-intellij/commit/67e71974))
* Feature: GitHub issue #79: Expandable "+" sign ([b3261d30](https://github.com/zowe/zowe-explorer-intellij/commit/b3261d30))
* Feature: Config service test implementation ([faea6da9](https://github.com/zowe/zowe-explorer-intellij/commit/faea6da9))
* Feature: Purge job ([dc726a48](https://github.com/zowe/zowe-explorer-intellij/commit/dc726a48))
* Feature: Write unit tests: UtilsTestSpec pt.1 and CrudableTestSpec ([3ff0ab36](https://github.com/zowe/zowe-explorer-intellij/commit/3ff0ab36))
* Feature: Unit tests: jUnit to Kotest ([0fa8a1d6](https://github.com/zowe/zowe-explorer-intellij/commit/0fa8a1d6))


* Bugfix: Unit tests are broken ([e2f12c92](https://github.com/zowe/zowe-explorer-intellij/commit/e2f12c92))
* Bugfix: GitHub issue #55: Error in event log when copy member to PDS that does not have enough space ([84041284](https://github.com/zowe/zowe-explorer-intellij/commit/84041284))
* Bugfix: GitHub issue #83: The creating z/OS mask '*.*' is not blocked ([3782d84e](https://github.com/zowe/zowe-explorer-intellij/commit/3782d84e))
* Bugfix: GitHub issue #89: Impossible to rename USS directory whose name contains & ([7ae62611](https://github.com/zowe/zowe-explorer-intellij/commit/7ae62611))
* Bugfix: Problem with automatic refresh after creating new dataset/allocate like ([9e0ec70b](https://github.com/zowe/zowe-explorer-intellij/commit/9e0ec70b))
* Bugfix: Incorrect data encoding ([06695658](https://github.com/zowe/zowe-explorer-intellij/commit/06695658))
* Bugfix: GitHub issue #101: When dataset member is moved from one DS to another, load more appears instead of it ([aea142a9](https://github.com/zowe/zowe-explorer-intellij/commit/aea142a9))
* Bugfix: Strange behavior of automatic reload and batch_size ([b4c5c2a6](https://github.com/zowe/zowe-explorer-intellij/commit/b4c5c2a6))
* Bugfix: Skip files copy files anyway ([237061ea](https://github.com/zowe/zowe-explorer-intellij/commit/237061ea))
* Bugfix: GitHub issue #107: I can't edit USS files ([1ff98872](https://github.com/zowe/zowe-explorer-intellij/commit/1ff98872))
* Bugfix: GitHub issue #108: Incompatible with IntelliJ 2022.3 ([9ae290d5](https://github.com/zowe/zowe-explorer-intellij/commit/9ae290d5))
* Bugfix: IDE error with UnsupportedEncodingException for some encodings ([06695658](https://github.com/zowe/zowe-explorer-intellij/commit/06695658))
* Bugfix: Impossible to close uss-file with write permission after changing encoding ([06695658](https://github.com/zowe/zowe-explorer-intellij/commit/06695658))
* Bugfix: There is no warning if copy/paste from remote to local ([11c4a90e](https://github.com/zowe/zowe-explorer-intellij/commit/11c4a90e))
* Bugfix: Synchronization is cycled in autosync mode after first opening for the file ([f44ae9e3](https://github.com/zowe/zowe-explorer-intellij/commit/f44ae9e3))
* Bugfix: IDE error with UndeclaredThrowableException while closing CLI when connection was broken ([604bab98](https://github.com/zowe/zowe-explorer-intellij/commit/604bab98))
* Bugfix: Cancel DnD several members from one host to another does not work properly ([6cd76917](https://github.com/zowe/zowe-explorer-intellij/commit/6cd76917))
* Bugfix: IDE error with NoSuchElementException if start CLI when there is no any connections ([b3261d30](https://github.com/zowe/zowe-explorer-intellij/commit/b3261d30))
* Bugfix: GitHub issue #53: Differences in the interface (field highlighting) ([12fe34cf](https://github.com/zowe/zowe-explorer-intellij/commit/12fe34cf))
* Bugfix: GitHub issue #82: Vertical scrollbar in 'Add Working Set/Edit Working Set' does not work properly if you add a lot of masks ([f73b70e1](https://github.com/zowe/zowe-explorer-intellij/commit/f73b70e1))
* Bugfix: Different colmn name JES vs Jobs Working Set ([2fa59937](https://github.com/zowe/zowe-explorer-intellij/commit/2fa59937))
* Bugfix: z/OS mask is created in lowercase if use dataset name in lowercase during allocate/allocate like ([263cc89c](https://github.com/zowe/zowe-explorer-intellij/commit/263cc89c))
* Bugfix: Sync data does not work correctly when the content has not changed ([52a8fadd](https://github.com/zowe/zowe-explorer-intellij/commit/52a8fadd))
* Bugfix: Missing warning if delete connection that has any jobs working set ([15d21619](https://github.com/zowe/zowe-explorer-intellij/commit/15d21619))
* Bugfix: Last mask/filter is created in wrong way in Edit Working Set/Edit Jobs Working Set dialogs via context menu ([f139b1d8](https://github.com/zowe/zowe-explorer-intellij/commit/f139b1d8))
* Bugfix: File upload icon is cycling when double-clicking again on an open file ([bec33017](https://github.com/zowe/zowe-explorer-intellij/commit/bec33017))
* Bugfix: Small typo in annotation ([d7b898bf](https://github.com/zowe/zowe-explorer-intellij/commit/d7b898bf))
* Bugfix: The button 'Ok' on Warning when delete connections with ws/jws ([ebf8d80e](https://github.com/zowe/zowe-explorer-intellij/commit/ebf8d80e))
* Bugfix: Operation is not supported for read-only collection while trying to create JES Working set ([80f2af26](https://github.com/zowe/zowe-explorer-intellij/commit/80f2af26))
* Bugfix: Exception during IDE startup with plugin ([5dbc9f27](https://github.com/zowe/zowe-explorer-intellij/commit/5dbc9f27))
* Bugfix: Typo in error message in Allocate Dataset dialog ([8504564c](https://github.com/zowe/zowe-explorer-intellij/commit/8504564c))
* Bugfix: Typo in message for incorrect directory quantity in allocate dataset ([8504564c](https://github.com/zowe/zowe-explorer-intellij/commit/8504564c))
* Bugfix: Typo in message for incorrect directory quantity in allocate dataset ([8504564c](https://github.com/zowe/zowe-explorer-intellij/commit/8504564c))
* Bugfix: Unhandled error type for jobs ([47ea9f94](https://github.com/zowe/zowe-explorer-intellij/commit/47ea9f94))
* Bugfix: Automatic refresh does not work correctly for job filter after purge job via context menu ([47ea9f94](https://github.com/zowe/zowe-explorer-intellij/commit/47ea9f94))
* Bugfix: Missing '>' for input next several commands in CLI after programm running finished ([ed7cf0bc](https://github.com/zowe/zowe-explorer-intellij/commit/ed7cf0bc))
* Bugfix: Move member to another PDS refreshes only one PDS ([7c693076](https://github.com/zowe/zowe-explorer-intellij/commit/7c693076))
* Bugfix: Content encoding change after uss read only file reopened ([0a4d638f](https://github.com/zowe/zowe-explorer-intellij/commit/0a4d638f))
* Bugfix: Refresh does not work if copy-delete-copy one USS folder to another USS folder ([656fb37e](https://github.com/zowe/zowe-explorer-intellij/commit/656fb37e))
* Bugfix: IndexOutOfBoundsException if create JWS via context menu ([c40bb42a](https://github.com/zowe/zowe-explorer-intellij/commit/c40bb42a))
* Bugfix: Exception in Zowe Explorer when there is a configuration from For Mainframe plugin exist ([7cee23f3](https://github.com/zowe/zowe-explorer-intellij/commit/7cee23f3))
* Bugfix: Policy agreement is gone wild ([d600f58e](https://github.com/zowe/zowe-explorer-intellij/commit/d600f58e))
* Bugfix: Exception while opening TSO CLI ([540bfa80](https://github.com/zowe/zowe-explorer-intellij/commit/540bfa80))
* Bugfix: Exception during IDE startup with plugin ([5dbc9f27](https://github.com/zowe/zowe-explorer-intellij/commit/5dbc9f27))


## `0.7.1 (2022-11-30)`

* Feature: Unit tests for utils module ([16fae1fb](https://github.com/zowe/zowe-explorer-intellij/commit/16fae1fb)) ([683185bb](https://github.com/zowe/zowe-explorer-intellij/commit/683185bb))


* Bugfix: DnD does not work properly ([e5dfa3a3](https://github.com/zowe/zowe-explorer-intellij/commit/e5dfa3a3))
* Bugfix: Copy DS member to USS folder does not work ([1e94ec48](https://github.com/zowe/zowe-explorer-intellij/commit/1e94ec48))
* Bugfix: Unknown type of file if copy-delete-copy the same PDS member ([21651646](https://github.com/zowe/zowe-explorer-intellij/commit/21651646))
* Bugfix: Ctrl+C/Ctrl+V does not work if copy file from remote to local ([e5601e7f](https://github.com/zowe/zowe-explorer-intellij/commit/e5601e7f))

## `0.7.0 (2022-10-31)`

* Breaking: Kotlin DSL v2 usage introduced. Plugin requires to use it with IntelliJ version >= 2022.1


* Feature: Configurable batch size to load filter
  smoothly ([928baba](https://github.com/zowe/zowe-explorer-intellij/commit/928baba))
* Feature: Job Purge operation ([95bf7eb](https://github.com/zowe/zowe-explorer-intellij/commit/95bf7eb))
* Feature: Job Edit operation  ([b3962de](https://github.com/zowe/zowe-explorer-intellij/commit/b3962de))
* Feature: Copy local to remote ([d9dcdd2](https://github.com/zowe/zowe-explorer-intellij/commit/d9dcdd2)
  , [ea92b4b](https://github.com/zowe/zowe-explorer-intellij/commit/ea92b4b))
* Feature: Copy remote to remote ([586a7f2](https://github.com/zowe/zowe-explorer-intellij/commit/586a7f2))
* Feature: GitHub issue #10: Edit Working sets directly from Tool
  Window ([63a2e4f](https://github.com/zowe/zowe-explorer-intellij/commit/63a2e4f))
* Feature: GitHub issue #70: Add date and time to JES
  Explorer ([eaea2cc](https://github.com/zowe/zowe-explorer-intellij/commit/eaea2cc))
* Feature: Copy remote to local: clarify
  warning ([26bbb2c](https://github.com/zowe/zowe-explorer-intellij/commit/26bbb2c))
* Feature: GitHub issue #67: Allocate like for datasets with BLK will be with
  warning ([7ae6261](https://github.com/zowe/zowe-explorer-intellij/commit/7ae6261))
* Feature: Move the file attribute conversion to a separate
  thread ([975f75d](https://github.com/zowe/zowe-explorer-intellij/commit/975f75d))
* Feature: Source code documentation added ([636411e](https://github.com/zowe/zowe-explorer-intellij/commit/636411e)
  , [11bb7dd](https://github.com/zowe/zowe-explorer-intellij/commit/11bb7dd))


* Bugfix: File cache conflict if open JCL to edit it in JES explorer second
  time ([b3962de](https://github.com/zowe/zowe-explorer-intellij/commit/b3962de))
* Bugfix: GitHub issue #86: Incorrect error message if mask length >
  44 ([cfb4ab6](https://github.com/zowe/zowe-explorer-intellij/commit/cfb4ab6))
* Bugfix: GitHub issue #87: Masks type autodetection does not work in Add/Edit Working Set
  dialogs ([49fc53a](https://github.com/zowe/zowe-explorer-intellij/commit/49fc53a))
* Bugfix: Problem with automatic refresh after creating new members/deleting members from
  dataset ([928baba](https://github.com/zowe/zowe-explorer-intellij/commit/928baba))
* Bugfix: Confusing dialog title 'Rename Directory' when renaming USS mask from context
  menu ([1e1a147](https://github.com/zowe/zowe-explorer-intellij/commit/1e1a147))
* Bugfix: GitHub issue #81: There is no difference between upper and lower cases when create USS masks from context
  menu ([f8ea3e9](https://github.com/zowe/zowe-explorer-intellij/commit/f8ea3e9))
* Bugfix: GitHub issue #88: Lower case is not changed to upper case during Job Filter
  creation ([c2f5b01](https://github.com/zowe/zowe-explorer-intellij/commit/c2f5b01))
* Bugfix: GitHub issue #44: 'Sync data' button does not work properly when multiple changes in USS
  file ([27f9c6a](https://github.com/zowe/zowe-explorer-intellij/commit/27f9c6a))
* Bugfix: GitHub issue #30: Create new member in dataset that does not have enough space creates empty member despite of
  warning ([7a649e6](https://github.com/zowe/zowe-explorer-intellij/commit/7a649e6))
* Bugfix: GitHub issue #54: Accumulation of errors in WS that breaks
  WS ([8648da2](https://github.com/zowe/zowe-explorer-intellij/commit/8648da2))
* Bugfix: USS file cannot be deleted in development
  branch ([8886770](https://github.com/zowe/zowe-explorer-intellij/commit/8886770))
* Bugfix: z/OS version specified in connection information doesn't match the z/OS version returned from
  z/OSMF ([1148e10](https://github.com/zowe/zowe-explorer-intellij/commit/1148e10))
* Bugfix: IDE error with ReadOnlyModificationException when set 'use binary mode' for read only
  uss-file ([c2ebf6a](https://github.com/zowe/zowe-explorer-intellij/commit/c2ebf6a))
* Bugfix: GitHub issue #94: SYSPRINT I looked at first always opens in JES explorer for a job with multiple
  steps ([301012a](https://github.com/zowe/zowe-explorer-intellij/commit/301012a))
* Bugfix: IDE error with CallException when try to open uss-file to which you have no
  access ([78650b9](https://github.com/zowe/zowe-explorer-intellij/commit/78650b9))
* Bugfix: The content of sequential dataset/member is changed anyway even if you choose do not sync data with
  mainframe ([559b05e](https://github.com/zowe/zowe-explorer-intellij/commit/559b05e))
* Bugfix: IDE error while retrieving job list in JES
  Explorer ([e3dfe93](https://github.com/zowe/zowe-explorer-intellij/commit/e3dfe93))
* Bugfix: Extra item 'Rename' is active in the context menu if click on 'loading...'/'load more' in file
  explorer ([78ab43f](https://github.com/zowe/zowe-explorer-intellij/commit/78ab43f))
* Bugfix: Impossible to open any file/dataset second
  time ([9dc62ef](https://github.com/zowe/zowe-explorer-intellij/commit/9dc62ef))
* Bugfix: The job is marked with green icon as passed despite it finished with
  abend ([773a252](https://github.com/zowe/zowe-explorer-intellij/commit/773a252))
* Bugfix: GitHub issue #16: Error creating zOSMF connection
* Bugfix: GitHub issue #85: The windows 'Add Working Set'/'Edit Working Set' are automatically resized if z/OSMF
  connection with very long name is added
