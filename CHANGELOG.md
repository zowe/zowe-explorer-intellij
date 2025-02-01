# IntelliJ Plugin Changelog

All notable changes to the For Mainframe Plugin will be documented in this file.

## [Unreleased]

### Bugfixes

* Bugfix: Fixed connection not showing after creation ([b1cdf610](https://github.com/zowe/zowe-explorer-intellij/commit/b1cdf610))
* Bugfix: Fixed error when uploading local file to USS ([c5dcd7fa](https://github.com/zowe/zowe-explorer-intellij/commit/c5dcd7fa))
* Bugfix: Fixed issue when error message does not disappear after errors are corrected in a Job Filter ([64a6d209](https://github.com/zowe/zowe-explorer-intellij/commit/64a6d209))
* Bugfix: Privacy Policy was open for modifications ([cd917844](https://github.com/zowe/zowe-explorer-intellij/commit/cd917844))
* Bugfix: Fixed NullPointerException on cancel operation ([f8d08fd4](https://github.com/zowe/zowe-explorer-intellij/commit/f8d08fd4))

## [1.2.4-221] (2024-11-18)

### Bugfixes

* Bugfix: Fixed issue with cross-system folder copy with name conflicts ([c5345051](https://github.com/zowe/zowe-explorer-intellij/commit/c5345051))
* Bugfix: Clarified 401 error ([f36ec547](https://github.com/zowe/zowe-explorer-intellij/commit/f36ec547))
* Bugfix: Fixed issue with USS copy that would lead to incorrect encoding error ([b4cb705a](https://github.com/zowe/zowe-explorer-intellij/commit/b4cb705a))
* Bugfix: Fixed issue with jobs DDs display in console ([8a75f21a](https://github.com/zowe/zowe-explorer-intellij/commit/8a75f21a))
* Bugfix: Fixed issue with copying another user's folder in USS ([66676550](https://github.com/zowe/zowe-explorer-intellij/commit/66676550))
* Bugfix: Some encodings are made unsupported ([080c25ab](https://github.com/zowe/zowe-explorer-intellij/commit/080c25ab))
* Bugfix: Fixed issue during a file upload to a PDS ([954bf01f](https://github.com/zowe/zowe-explorer-intellij/commit/954bf01f))
* Bugfix: Fixed issue with URL field when typing incorrect information, the last slash at the end of https:// is deleted ([91ab962d](https://github.com/zowe/zowe-explorer-intellij/commit/91ab962d))

## [1.2.3-221] (2024-10-03)

### Features

* Feature: Correct alignment for fields in working set dialog ([745143cc](https://github.com/zowe/zowe-explorer-intellij/commit/745143cc))

### Bufixes

* Bugfix: Fixed unexpected freezes of IDE during work with USS part ([38f1f5b3](https://github.com/zowe/zowe-explorer-intellij/commit/38f1f5b3))
* Bugfix: Fixed error that was caused by members renaming ([238ebc08](https://github.com/zowe/zowe-explorer-intellij/commit/238ebc08))
* Bugfix: Fixed missed automatic translation to uppercase ([1b48de61](https://github.com/zowe/zowe-explorer-intellij/commit/1b48de61))
* Bugfix: Fixed synchronization errors ([f96d9302](https://github.com/zowe/zowe-explorer-intellij/commit/f96d9302))
* Bugfix: Fixed permissions change in opened file ([b04ed99e](https://github.com/zowe/zowe-explorer-intellij/commit/b04ed99e))
* Bugfix: Fixed copy-paste folder to another folder with name conflicts ([13d7d773](https://github.com/zowe/zowe-explorer-intellij/commit/13d7d773))
* Bugfix: Validation changes for LRECL field ([14e384a4](https://github.com/zowe/zowe-explorer-intellij/commit/14e384a4))
* Bugfix: Fixed issue when job was not visible in JesExplorerView when Job Filter is created by JOBID ([5bb17263](https://github.com/zowe/zowe-explorer-intellij/commit/5bb17263))

## [1.2.2-221] (2024-08-21)

### Features

* Feature: Removed "openApiUtils" functions that were duplicating the ones already implemented in IntelliJ IDEA platform ([fbf7db62](https://github.com/zowe/zowe-explorer-intellij/commit/fbf7db62))

### Bugfixes

* Bugfix: Fixed "/" and "/u" is not displayed correctly in USS Explorer ([547b9678](https://github.com/zowe/zowe-explorer-intellij/commit/547b9678))
* Bugfix: "UTF-16" encoding is now either LE or BE available only ([f4b7e9de](https://github.com/zowe/zowe-explorer-intellij/commit/f4b7e9de))
* Bugfix: The possibility to delete unrelated selected entities is disabled ([f7f9f019](https://github.com/zowe/zowe-explorer-intellij/commit/f7f9f019))
* Bugfix: Fixed cross-system copy-paste for Sequential to PDS ([fb1943e3](https://github.com/zowe/zowe-explorer-intellij/commit/fb1943e3))
* Bugfix: Fixed file permissions change is not reset after failure ([0ce02146](https://github.com/zowe/zowe-explorer-intellij/commit/0ce02146))
* Bugfix: Fixed error notification displayed on uploading USS file ([b84c8beb](https://github.com/zowe/zowe-explorer-intellij/commit/b84c8beb))
* Bugfix: Fixed colouring of entity when it was cut but the operation is changed to download ([3cdb982f](https://github.com/zowe/zowe-explorer-intellij/commit/3cdb982f))
* Bugfix: Fixed folder copying when both parent and child directories are selected ([d8a44d98](https://github.com/zowe/zowe-explorer-intellij/commit/d8a44d98))
* Bugfix: Fixed "Decide for Each -> Skip" option on Cut/Copy ([70089fcc](https://github.com/zowe/zowe-explorer-intellij/commit/70089fcc))
* Bugfix: Fixed rename of datasets was saving names in lowercase in local cache ([18ebcee2](https://github.com/zowe/zowe-explorer-intellij/commit/18ebcee2))
* Bugfix: Fixed downloading of USS files when there are conflicts on a local machine ([ccb48dda](https://github.com/zowe/zowe-explorer-intellij/commit/ccb48dda))
* Bugfix: Fixed copying folder with contents not displaying the contents right after it is copied ([82c7b7a6](https://github.com/zowe/zowe-explorer-intellij/commit/82c7b7a6))

## [1.2.1-221] (2024-06-12)

### Features
* Feature: Change dialogs as DoNotAskOption is going to be removed soon ([0d6b5d57](https://github.com/zowe/zowe-explorer-intellij/commit/0d6b5d57))
* Feature: Add 'Unknown' to fields in dataset properties dialog when there is no information for fields ([5b32a4df](https://github.com/zowe/zowe-explorer-intellij/commit/5b32a4df))
* Feature: Validate owner when receiving information from TSO ([9db42fb8](https://github.com/zowe/zowe-explorer-intellij/commit/9db42fb8))
* Feature: Conflicts dialog on copy/move improvement ([c8f4d525](https://github.com/zowe/zowe-explorer-intellij/commit/c8f4d525))

### Bugfixes
* Bugfix: Rename of the opened file causes exception ([09453311](https://github.com/zowe/zowe-explorer-intellij/commit/09453311))
* Bugfix: Refresh does not work on the first try ([7af1c668](https://github.com/zowe/zowe-explorer-intellij/commit/7af1c668))
* Bugfix: "File Cache Conflict" when downloading file ([3f84b02a](https://github.com/zowe/zowe-explorer-intellij/commit/3f84b02a))
* Bugfix: Error opening an alias ([5b6ee654](https://github.com/zowe/zowe-explorer-intellij/commit/5b6ee654))
* Bugfix: Duplicate error messages ([c8ae8ddc](https://github.com/zowe/zowe-explorer-intellij/commit/c8ae8ddc))
* Bugfix: Rename file/folder to the same name causes exception ([3587a87b](https://github.com/zowe/zowe-explorer-intellij/commit/3587a87b))
* Bugfix: SyncAction: AWT events are not allowed inside write action ([796a6b9b](https://github.com/zowe/zowe-explorer-intellij/commit/796a6b9b))
* Bugfix: Warning dialog for downloading shows unnecessary scroll ([e7dc7427](https://github.com/zowe/zowe-explorer-intellij/commit/e7dc7427))
* Bugfix: Local cache conflict when overwriting local files ([f3039ac6](https://github.com/zowe/zowe-explorer-intellij/commit/f3039ac6))
* Bugfix: java.lang.RuntimeException: Cannot invoke (class=, method=onCacheUpdated, topic=cacheUpdated) ([2fedc7f5](https://github.com/zowe/zowe-explorer-intellij/commit/2fedc7f5))
* Bugfix: "Allocate like" action causes exception when refresh on mask is started ([d13a1c72](https://github.com/zowe/zowe-explorer-intellij/commit/d13a1c72))

## [1.2.0-221] (2024-04-16)

### Features

* Feature: GitHub issue #171: TSO CLI PA1 button functionality added ([48834cac](https://github.com/zowe/zowe-explorer-intellij/commit/48834cac))
* Feature: GitHub issue #172: Allocation units clarification added ([1ff218e4](https://github.com/zowe/zowe-explorer-intellij/commit/1ff218e4))
* Feature: GitHub issue #173: Now after allocating a dataset, a notification is shown instead of a dialog window ([20343651](https://github.com/zowe/zowe-explorer-intellij/commit/20343651))
* Feature: GitHub issue #174: VFS_CHANGES topic rework for encoding purposes ([3adaded3](https://github.com/zowe/zowe-explorer-intellij/commit/3adaded3))
* Feature: GitHub issue #175: Close all files in editor, related to the file/folder/dataset/member being deleted ([8a0d9980](https://github.com/zowe/zowe-explorer-intellij/commit/8a0d9980))

### Bugfixes

* Bugfix: GitHub issue #159: Zowe config detection doesn't work correctly ([c73226f6](https://github.com/zowe/zowe-explorer-intellij/commit/c73226f6))
* Bugfix: Warning during working set creation without masks/job filters is missing ([4fd0b22c](https://github.com/zowe/zowe-explorer-intellij/commit/4fd0b22c))
* Bugfix: When opened file tabs bar is full, opening any dataset or USS file preserves only last 2 items Explorer items in the bar ([3a822fbb](https://github.com/zowe/zowe-explorer-intellij/commit/3a822fbb))
* Bugfix: Unclear error message for list datasets/jobs when password is expired ([74fe5e86](https://github.com/zowe/zowe-explorer-intellij/commit/74fe5e86))
* Bugfix: In dataset allocation dialog window, dataset name is reset when an error is received ([218f5a3a](https://github.com/zowe/zowe-explorer-intellij/commit/218f5a3a))
* Bugfix: Issue creating member with the same name as the existing one ([6ebae1a0](https://github.com/zowe/zowe-explorer-intellij/commit/6ebae1a0))
* Bugfix: "Overwrite for All" causes caches conflicts when dataset member is being copied to a USS with rewrite ([90b9ce17](https://github.com/zowe/zowe-explorer-intellij/commit/90b9ce17))
* Bugfix: "Cut/Paste" doesn't work when moving a sequential dataset to a partitioned dataset ([f1cf4a9d](https://github.com/zowe/zowe-explorer-intellij/commit/f1cf4a9d))
* Bugfix: Incorrect warning message when uploading a local file to a PDS ([6d9e5de3](https://github.com/zowe/zowe-explorer-intellij/commit/6d9e5de3))
* Bugfix: It is possible to create a dataset when a connection is removed or invalid ([3df02fde](https://github.com/zowe/zowe-explorer-intellij/commit/3df02fde))
* Bugfix: TSO EXEC command without operands causes the CLI to hang ([d071960a](https://github.com/zowe/zowe-explorer-intellij/commit/d071960a))
* Bugfix: Connection is not fully reset to the last successful state and it causes errors ([f6d5a72e](https://github.com/zowe/zowe-explorer-intellij/commit/f6d5a72e))

## [1.1.2-221] (2024-01-22)

### Bugfixes

* Bugfix: Sync action does not work after file download ([bfb125d7](https://github.com/zowe/zowe-explorer-intellij/commit/bfb125d7))
* Bugifx: "Skip This Files" doesn't work when uploading local file to PDS ([749b2d4b](https://github.com/zowe/zowe-explorer-intellij/commit/749b2d4b))
* Bugifx: "Use new name" doesn't work for copying partitioned dataset to USS folder ([26d865be](https://github.com/zowe/zowe-explorer-intellij/commit/26d865be))
* Bugifx: "Use new name" doesn't work for copying sequential dataset to partitioned dataset ([349c02e9](https://github.com/zowe/zowe-explorer-intellij/commit/349c02e9))
* Bugfix: "Use new name" doesn't work when uploading local file to PDS ([26d865be](https://github.com/zowe/zowe-explorer-intellij/commit/26d865be))
* Bugfix: Editing two members with the same name does not update the content for one of the members ([25606368](https://github.com/zowe/zowe-explorer-intellij/commit/25606368))
* Bugfix: Topics handling ([25606368](https://github.com/zowe/zowe-explorer-intellij/commit/25606368))
* Bugfix: Zowe config v2 handling ([fd79b908](https://github.com/zowe/zowe-explorer-intellij/commit/fd79b908))
* Bugfix: JES Explorer bug when ABEND job is being displayed ([614aa6cf](https://github.com/zowe/zowe-explorer-intellij/commit/614aa6cf))
* Bugfix: GitHub issue #167: Zowe explorer config is not converted ([b5eae7a2](https://github.com/zowe/zowe-explorer-intellij/commit/b5eae7a2))

## [1.1.1-221] (2023-11-23)

### Bugfixes

* Bugfix: Dataset color does not change when
  cutting ([fa6f6ae5](https://github.com/zowe/zowe-explorer-intellij/commit/fa6f6ae5))
* Bugfix: Spelling error in reload/convert
  dialog ([ec6d27da](https://github.com/zowe/zowe-explorer-intellij/commit/ec6d27da))
* Bugfix: Incorrect error messages when allocating
  dataset ([e1134061](https://github.com/zowe/zowe-explorer-intellij/commit/e1134061))
* Bugfix: Incorrect message if create connection with connection url ending with 2
  slashes ([632e66c5](https://github.com/zowe/zowe-explorer-intellij/commit/632e66c5))
* Bugfix: Sync action is not working during indexing process in 221 and
  222 ([df523764](https://github.com/zowe/zowe-explorer-intellij/commit/df523764))
* Bugfix: Mask cannot be created when the other connection is
  selected ([2188d9f3](https://github.com/zowe/zowe-explorer-intellij/commit/2188d9f3))
* Bugfix: Copy and cut error message if skip for all or overwrite for all is
  selected ([6313b773](https://github.com/zowe/zowe-explorer-intellij/commit/6313b773))
* Bugfix: GitHub issue #139: Can't allocate a dataset with record format
  U ([88d68cb9](https://github.com/zowe/zowe-explorer-intellij/commit/88d68cb9))
* Bugfix: GitHub issue #143: Incorrect behaviour of TSO
  CLI ([451dc01f](https://github.com/zowe/zowe-explorer-intellij/commit/451dc01f))
* Bugfix: GitHub issue #161: Conflict between Zowe Explorer and For
  Mainframe ([79a7fc50](https://github.com/zowe/zowe-explorer-intellij/commit/79a7fc50))

## [1.1.0-221] (2023-09-07)

### Features

* Feature: GitHub issue #14: UX: Edit WS
  mask ([0d358d0d](https://github.com/zowe/zowe-explorer-intellij/commit/0d358d0d))
* Feature: GitHub issue #23: Double click on a working set or
  connection ([e7f040d7](https://github.com/zowe/zowe-explorer-intellij/commit/e7f040d7))
* Feature: GitHub issue #49: Plugin logging ([76b6b175](https://github.com/zowe/zowe-explorer-intellij/commit/76b6b175))
* Feature: GitHub issue #52: Presets for creating
  datasets ([6b8a5ca6](https://github.com/zowe/zowe-explorer-intellij/commit/6b8a5ca6))
* Feature: GitHub issue #111: "Rename" in dialog window should be "Edit" for DS and USS
  masks ([c39f7c01](https://github.com/zowe/zowe-explorer-intellij/commit/c39f7c01))
* Feature: GitHub issue #112: Migrate all UI tests from real data usage to mock
  server ([531d0e94](https://github.com/zowe/zowe-explorer-intellij/commit/531d0e94))
* Feature: GitHub issue #113: Change user password
  feature ([0706abcb](https://github.com/zowe/zowe-explorer-intellij/commit/0706abcb))
* Feature: GitHub issue #122: "whoami" on connection
  creation ([23ad877b](https://github.com/zowe/zowe-explorer-intellij/commit/23ad877b))
* Feature: GitHub issue #123: Implement "No items found" for USS and DS
  masks ([762827d0](https://github.com/zowe/zowe-explorer-intellij/commit/762827d0))
* Feature: GitHub issue #124: Clarify DS
  organization ([361f5f0a](https://github.com/zowe/zowe-explorer-intellij/commit/361f5f0a))
* Feature: GitHub issue #125: 80 LRECL by
  default ([62598726](https://github.com/zowe/zowe-explorer-intellij/commit/62598726))
* Feature: GitHub issue #126: Copy + rename ([440b65d9](https://github.com/zowe/zowe-explorer-intellij/commit/440b65d9))
* Feature: GitHub issue #130: JDK search index broken in IntelliJ after dataset is open
* Feature: GitHub issue #136: CLEARTEXT communication not enabled for
  client ([a2958223](https://github.com/zowe/zowe-explorer-intellij/commit/a2958223))
* Feature: GitHub issue #140: Exception in Zowe Explorer (1.0.2-221) for Android Studio(Android Studio Flamingo |
  2022.2.1 Patch 2) ([3ce3813e](https://github.com/zowe/zowe-explorer-intellij/commit/3ce3813e))
* Feature: GitHub issue #144: Incorrect encoding should not be changed directly, until a user is decided to change it
  when we suggest ([02ae0c62](https://github.com/zowe/zowe-explorer-intellij/commit/02ae0c62))
* Feature: GitHub issue #145: Migrated dataset properties should not be visible if they are not
  available ([18b13ff1](https://github.com/zowe/zowe-explorer-intellij/commit/18b13ff1))
* Feature: GitHub issue #146: Hints for creating working sets after connection is
  created ([dcd92dfa](https://github.com/zowe/zowe-explorer-intellij/commit/dcd92dfa))
* Feature: GitHub issue #147: "Duplicate" for
  member ([1493f1e8](https://github.com/zowe/zowe-explorer-intellij/commit/1493f1e8))
* Feature: GitHub issue #148: Warning about incompatible
  encodings ([3bd29c7a](https://github.com/zowe/zowe-explorer-intellij/commit/3bd29c7a))
* Feature: Separate info and tso requests during connection
  test ([83aa6bf2](https://github.com/zowe/zowe-explorer-intellij/commit/83aa6bf2))
* Feature: Rework configs in the plug-in to accept new
  configurables ([d0ff5b3d](https://github.com/zowe/zowe-explorer-intellij/commit/d0ff5b3d))
* Feature: Rework file sync with MF ([c43b8198](https://github.com/zowe/zowe-explorer-intellij/commit/c43b8198))
* Feature: Presets: improvement ([7255dfce](https://github.com/zowe/zowe-explorer-intellij/commit/7255dfce))
* Feature: VFS_CHANGES to MF_VFS_CHANGES ([0dd8b1b4](https://github.com/zowe/zowe-explorer-intellij/commit/0dd8b1b4))
* Feature: Change XML and JSON comparison on different plugin
  versions ([7f480747](https://github.com/zowe/zowe-explorer-intellij/commit/7f480747))
* Feature: Substitute R2Z with Zowe Kotlin
  SDK ([df6d11c1](https://github.com/zowe/zowe-explorer-intellij/commit/df6d11c1))
* Feature: Enhance configuration for CICS
  connections ([cfa94f88](https://github.com/zowe/zowe-explorer-intellij/commit/cfa94f88))
* Feature: Unit
  tests ([019bef00](https://github.com/zowe/zowe-explorer-intellij/commit/019bef00), [fa903c1c](https://github.com/zowe/zowe-explorer-intellij/commit/fa903c1c), [709a5e2f](https://github.com/zowe/zowe-explorer-intellij/commit/709a5e2f), [07ecd283](https://github.com/zowe/zowe-explorer-intellij/commit/07ecd283), [93febb62](https://github.com/zowe/zowe-explorer-intellij/commit/93febb62), [517c2cff](https://github.com/zowe/zowe-explorer-intellij/commit/517c2cff), [29cd85e4](https://github.com/zowe/zowe-explorer-intellij/commit/29cd85e4), [3850fcec](https://github.com/zowe/zowe-explorer-intellij/commit/3850fcec), [5646963e](https://github.com/zowe/zowe-explorer-intellij/commit/5646963e), [be3b5086](https://github.com/zowe/zowe-explorer-intellij/commit/be3b5086), [5043f5da](https://github.com/zowe/zowe-explorer-intellij/commit/5043f5da), [fdf67e1c](https://github.com/zowe/zowe-explorer-intellij/commit/fdf67e1c))

### Bugfixes

* Bugfix: GitHub issue #138: Job is identified as successful while it ends with
  RC=12 ([99491858](https://github.com/zowe/zowe-explorer-intellij/commit/99491858))
* Bugfix: Tooltip on JES Working set shows 'Working
  set' ([9937c128](https://github.com/zowe/zowe-explorer-intellij/commit/9937c128))
* Bugfix: "debounce" test is failed
  sometimes ([8bb98da8](https://github.com/zowe/zowe-explorer-intellij/commit/8bb98da8))
* Bugfix: Allocate like strange behavior ([a0bf3da4](https://github.com/zowe/zowe-explorer-intellij/commit/a0bf3da4))
* Bugfix: Change permissions: incorrect permissions shown after change
  failure ([2143e4ba](https://github.com/zowe/zowe-explorer-intellij/commit/2143e4ba))
* Bugfix: Empty pds is not deleted after its move to
  pds ([e88f830f](https://github.com/zowe/zowe-explorer-intellij/commit/e88f830f))
* Bugfix: It shows that the job is still running after successfull
  purge ([d4a4289e](https://github.com/zowe/zowe-explorer-intellij/commit/d4a4289e))
* Bugfix: Errors for several actions in JES
  Explorer ([3496acc1](https://github.com/zowe/zowe-explorer-intellij/commit/3496acc1))
* Bugfix: Error if move empty PS to PDS ([15b262c0](https://github.com/zowe/zowe-explorer-intellij/commit/15b262c0))
* Bugfix: Password is changed only for one
  connection ([a52830c8](https://github.com/zowe/zowe-explorer-intellij/commit/a52830c8))
* Bugfix: Userid for new ws/jws is not changed in FileExplorer/JesExplorer after changes in corresponding
  connection ([ea0c9706](https://github.com/zowe/zowe-explorer-intellij/commit/ea0c9706))
* Bugfix: FileNotFoundException on configs search (The system cannot find the file
  specified) ([76badb81](https://github.com/zowe/zowe-explorer-intellij/commit/76badb81))
* Bugfix: Content of uss-file is changed to UTF-8 while copying it from remote to
  local ([ed86553e](https://github.com/zowe/zowe-explorer-intellij/commit/ed86553e))
* Bugfix: Copy/paste and DnD of PS dataset from one host to uss-folder on another host does not
  work ([1522e08a](https://github.com/zowe/zowe-explorer-intellij/commit/1522e08a))
* Bugfix: validateForGreaterValue should show correct
  message ([f841692e](https://github.com/zowe/zowe-explorer-intellij/commit/f841692e))
* Bugfix: JCL highlight does not work on mainframe
  files ([80c8288a](https://github.com/zowe/zowe-explorer-intellij/commit/80c8288a))
* Bugfix: IDE error when rename member/dataset to existing one/to the
  same ([e8dbaa8a](https://github.com/zowe/zowe-explorer-intellij/commit/e8dbaa8a))
* Bugfix: ClassCastException: class java.util.ArrayList cannot be cast to class
  com.intellij.openapi.vfs.VirtualFile ([cdc0a458](https://github.com/zowe/zowe-explorer-intellij/commit/cdc0a458))

## [1.0.2-221] (2023-06-13)

### Features

* Feature: Returned support for IntelliJ
  2022.1 ([6329c788](https://github.com/zowe/zowe-explorer-intellij/commit/6329c788))
* Feature: Focus on dataset name field in allocation
  dialog ([fccb77f9](https://github.com/zowe/zowe-explorer-intellij/commit/fccb77f9))

### Bugfixes

* Bugfix: Memory leak bug ([644c9fa1](https://github.com/zowe/zowe-explorer-intellij/commit/644c9fa1))
* Bugfix: GitHub issue #132: IDE internal error -
  NPE ([d4319c61](https://github.com/zowe/zowe-explorer-intellij/commit/d4319c61))
* Bugfix: Access denied error when copy from remote to local file when local has folder with the same
  name ([ec67210a](https://github.com/zowe/zowe-explorer-intellij/commit/ec67210a))
* Bugfix: Paste to dataset with LRECL does not move exceeding characters to a new
  line ([5b844ee6](https://github.com/zowe/zowe-explorer-intellij/commit/5b844ee6))
* Bugfix: USS file with 0 permissions is not accessible and no error message
  displayed ([b45ba4e0](https://github.com/zowe/zowe-explorer-intellij/commit/b45ba4e0))
* Bugfix: Refresh does not work for job filter with one job after
  purge ([a0461f25](https://github.com/zowe/zowe-explorer-intellij/commit/a0461f25))
* Bugfix: Name conflict message if move uss-file from folder to mask and then
  back ([2f1bdf51](https://github.com/zowe/zowe-explorer-intellij/commit/2f1bdf51))
* Bugfix: File cash conflict ([6405b42a](https://github.com/zowe/zowe-explorer-intellij/commit/6405b42a))
* Bugfix: Cancel button does not work for TSO connection test
  during ([5dbb6a4c](https://github.com/zowe/zowe-explorer-intellij/commit/5dbb6a4c))
* Bugfix: Unknown file type after delete member after
  move ([02b8090f](https://github.com/zowe/zowe-explorer-intellij/commit/02b8090f))

## [1.0.1] (2023-04-18)

### Features

* Feature: Support for IntelliJ 2023.1

### Bugfixes

* Bugfix: File is not displayed after folder moved inside another
  folder ([46e2fca8](https://github.com/zowe/zowe-explorer-intellij/commit/46e2fca8))
* Bugfix: IDE freeze after closing CLI during command execution with broken
  coonection ([1555d895](https://github.com/zowe/zowe-explorer-intellij/commit/1555d895))
* Bugfix: Last opened file remains active in
  editor ([419fe720](https://github.com/zowe/zowe-explorer-intellij/commit/419fe720))
* Bugfix: Duplicate widgets when installing For Mainframe and Zowe Explorer plugins
  together ([17a18f49](https://github.com/zowe/zowe-explorer-intellij/commit/17a18f49))
* Bugfix: Changed parameters in edit connection dialog do not reset after
  cancelation ([e28bf46e](https://github.com/zowe/zowe-explorer-intellij/commit/e28bf46e))
* Bugfix: Incorrect reloading on USS encoding
  change ([b3093e3f](https://github.com/zowe/zowe-explorer-intellij/commit/b3093e3f))
* Bugfix: println in TSO CLI ([70325f91](https://github.com/zowe/zowe-explorer-intellij/commit/70325f91))
* Bugfix: Copy DS member from one host to USS folder on another host does not
  work ([9e469698](https://github.com/zowe/zowe-explorer-intellij/commit/9e469698))
* Bugfix: Jobs filter is created with wrong default
  user ([33967d23](https://github.com/zowe/zowe-explorer-intellij/commit/33967d23))
* Bugfix: "Access is allowed from Event Dispatch Thread (EDT) only" on the plugin
  debug ([f5416f68](https://github.com/zowe/zowe-explorer-intellij/commit/f5416f68))
* Bugfix: SonarCloud compaint on Random ([294f3c11](https://github.com/zowe/zowe-explorer-intellij/commit/294f3c11))
* Bugfix: Autosync works strange ([8115f9dc](https://github.com/zowe/zowe-explorer-intellij/commit/8115f9dc))
* Bugfix: Strange behavior on copy paste from remote to
  local ([a193676c](https://github.com/zowe/zowe-explorer-intellij/commit/a193676c))
* Bugfix: Error while trying to move PS inside
  PDS ([ec94b39e](https://github.com/zowe/zowe-explorer-intellij/commit/ec94b39e))
* Bugfix: USS file empty after rename ([71c49b24](https://github.com/zowe/zowe-explorer-intellij/commit/71c49b24))

## [1.0.0] (2023-03-13)

### Breaking changes

* Breaking: Java 17 usage introduced. Plugin requires to use it with IntelliJ version >= 2022.3

### Features

* Feature: GitHub issue #31: Support for CHMOD
  operation ([3a166173](https://github.com/zowe/zowe-explorer-intellij/commit/3a166173))
* Feature: GitHub issue #40: TSO CLI ([5ed36ede](https://github.com/zowe/zowe-explorer-intellij/commit/5ed36ede))
* Feature: GitHub issue #41: Transfer USS files from any encoding, not just from
  EBCDIC ([06695658](https://github.com/zowe/zowe-explorer-intellij/commit/06695658))
* Feature: GitHub issue #69: Plugin struggles with opening big files and
  dataset ([1d0e9436](https://github.com/zowe/zowe-explorer-intellij/commit/1d0e9436))
* Feature: GitHub issue #5: JES Explorer tab has delete option disabled and missing edit
  option ([a4544e98](https://github.com/zowe/zowe-explorer-intellij/commit/a4544e98))
* Feature: Write documentation to
  DataOpsManagerImpl ([6a847a00](https://github.com/zowe/zowe-explorer-intellij/commit/6a847a00))
* Feature: Add support to all IBM encodings ([9e8bee51](https://github.com/zowe/zowe-explorer-intellij/commit/9e8bee51))
* Feature: UI regression tests ([3cd30136](https://github.com/zowe/zowe-explorer-intellij/commit/3cd30136))
* Feature: GitHub issue #39: Manual sync proceeded in main
  thread ([47042a81](https://github.com/zowe/zowe-explorer-intellij/commit/47042a81))
* Feature: GitHub issue #73: Migrate to Kotlin DSL
  v2 ([d37dbcb0](https://github.com/zowe/zowe-explorer-intellij/commit/d37dbcb0))
* Feature: GitHub issue #22: Support code page for
  TSO ([b1b57867](https://github.com/zowe/zowe-explorer-intellij/commit/b1b57867))
* Feature: Create WS and masks in twice
  place ([ae881fd0](https://github.com/zowe/zowe-explorer-intellij/commit/ae881fd0))
* Feature: GitHub issue #28: Fix build
  warnings ([0dcaa4de](https://github.com/zowe/zowe-explorer-intellij/commit/0dcaa4de))
* Feature: Rewrite RenameDialog to avoid calling validation setters in rename
  actions ([67e71974](https://github.com/zowe/zowe-explorer-intellij/commit/67e71974))
* Feature: GitHub issue #79: Expandable "+"
  sign ([b3261d30](https://github.com/zowe/zowe-explorer-intellij/commit/b3261d30))
* Feature: Config service test
  implementation ([faea6da9](https://github.com/zowe/zowe-explorer-intellij/commit/faea6da9))
* Feature: Purge job ([dc726a48](https://github.com/zowe/zowe-explorer-intellij/commit/dc726a48))
* Feature: Write unit tests: UtilsTestSpec pt.1 and
  CrudableTestSpec ([3ff0ab36](https://github.com/zowe/zowe-explorer-intellij/commit/3ff0ab36))
* Feature: Unit tests: jUnit to Kotest ([0fa8a1d6](https://github.com/zowe/zowe-explorer-intellij/commit/0fa8a1d6))

### Bugfixes

* Bugfix: Unit tests are broken ([e2f12c92](https://github.com/zowe/zowe-explorer-intellij/commit/e2f12c92))
* Bugfix: GitHub issue #55: Error in event log when copy member to PDS that does not have enough
  space ([84041284](https://github.com/zowe/zowe-explorer-intellij/commit/84041284))
* Bugfix: GitHub issue #83: The creating z/OS mask '*.*' is not
  blocked ([3782d84e](https://github.com/zowe/zowe-explorer-intellij/commit/3782d84e))
* Bugfix: GitHub issue #89: Impossible to rename USS directory whose name
  contains & ([7ae62611](https://github.com/zowe/zowe-explorer-intellij/commit/7ae62611))
* Bugfix: Problem with automatic refresh after creating new dataset/allocate
  like ([9e0ec70b](https://github.com/zowe/zowe-explorer-intellij/commit/9e0ec70b))
* Bugfix: Incorrect data encoding ([06695658](https://github.com/zowe/zowe-explorer-intellij/commit/06695658))
* Bugfix: GitHub issue #101: When dataset member is moved from one DS to another, load more appears instead of
  it ([aea142a9](https://github.com/zowe/zowe-explorer-intellij/commit/aea142a9))
* Bugfix: Strange behavior of automatic reload and
  batch_size ([b4c5c2a6](https://github.com/zowe/zowe-explorer-intellij/commit/b4c5c2a6))
* Bugfix: Skip files copy files anyway ([237061ea](https://github.com/zowe/zowe-explorer-intellij/commit/237061ea))
* Bugfix: GitHub issue #107: I can't edit USS
  files ([1ff98872](https://github.com/zowe/zowe-explorer-intellij/commit/1ff98872))
* Bugfix: GitHub issue #108: Incompatible with IntelliJ
  2022.3 ([9ae290d5](https://github.com/zowe/zowe-explorer-intellij/commit/9ae290d5))
* Bugfix: IDE error with UnsupportedEncodingException for some
  encodings ([06695658](https://github.com/zowe/zowe-explorer-intellij/commit/06695658))
* Bugfix: Impossible to close uss-file with write permission after changing
  encoding ([06695658](https://github.com/zowe/zowe-explorer-intellij/commit/06695658))
* Bugfix: There is no warning if copy/paste from remote to
  local ([11c4a90e](https://github.com/zowe/zowe-explorer-intellij/commit/11c4a90e))
* Bugfix: Synchronization is cycled in autosync mode after first opening for the
  file ([f44ae9e3](https://github.com/zowe/zowe-explorer-intellij/commit/f44ae9e3))
* Bugfix: IDE error with UndeclaredThrowableException while closing CLI when connection was
  broken ([604bab98](https://github.com/zowe/zowe-explorer-intellij/commit/604bab98))
* Bugfix: Cancel DnD several members from one host to another does not work
  properly ([6cd76917](https://github.com/zowe/zowe-explorer-intellij/commit/6cd76917))
* Bugfix: IDE error with NoSuchElementException if start CLI when there is no any
  connections ([b3261d30](https://github.com/zowe/zowe-explorer-intellij/commit/b3261d30))
* Bugfix: GitHub issue #53: Differences in the interface (field
  highlighting) ([12fe34cf](https://github.com/zowe/zowe-explorer-intellij/commit/12fe34cf))
* Bugfix: GitHub issue #82: Vertical scrollbar in 'Add Working Set/Edit Working Set' does not work properly if you add a
  lot of masks ([f73b70e1](https://github.com/zowe/zowe-explorer-intellij/commit/f73b70e1))
* Bugfix: Different colmn name JES vs Jobs Working
  Set ([2fa59937](https://github.com/zowe/zowe-explorer-intellij/commit/2fa59937))
* Bugfix: z/OS mask is created in lowercase if use dataset name in lowercase during allocate/allocate
  like ([263cc89c](https://github.com/zowe/zowe-explorer-intellij/commit/263cc89c))
* Bugfix: Sync data does not work correctly when the content has not
  changed ([52a8fadd](https://github.com/zowe/zowe-explorer-intellij/commit/52a8fadd))
* Bugfix: Missing warning if delete connection that has any jobs working
  set ([15d21619](https://github.com/zowe/zowe-explorer-intellij/commit/15d21619))
* Bugfix: Last mask/filter is created in wrong way in Edit Working Set/Edit Jobs Working Set dialogs via context
  menu ([f139b1d8](https://github.com/zowe/zowe-explorer-intellij/commit/f139b1d8))
* Bugfix: File upload icon is cycling when double-clicking again on an open
  file ([bec33017](https://github.com/zowe/zowe-explorer-intellij/commit/bec33017))
* Bugfix: Small typo in annotation ([d7b898bf](https://github.com/zowe/zowe-explorer-intellij/commit/d7b898bf))
* Bugfix: The button 'Ok' on Warning when delete connections with
  ws/jws ([ebf8d80e](https://github.com/zowe/zowe-explorer-intellij/commit/ebf8d80e))
* Bugfix: Operation is not supported for read-only collection while trying to create JES Working
  set ([80f2af26](https://github.com/zowe/zowe-explorer-intellij/commit/80f2af26))
* Bugfix: Exception during IDE startup with
  plugin ([5dbc9f27](https://github.com/zowe/zowe-explorer-intellij/commit/5dbc9f27))
* Bugfix: Typo in error message in Allocate Dataset
  dialog ([8504564c](https://github.com/zowe/zowe-explorer-intellij/commit/8504564c))
* Bugfix: Typo in message for incorrect directory quantity in allocate
  dataset ([8504564c](https://github.com/zowe/zowe-explorer-intellij/commit/8504564c))
* Bugfix: Unhandled error type for jobs ([47ea9f94](https://github.com/zowe/zowe-explorer-intellij/commit/47ea9f94))
* Bugfix: Automatic refresh does not work correctly for job filter after purge job via context
  menu ([47ea9f94](https://github.com/zowe/zowe-explorer-intellij/commit/47ea9f94))
* Bugfix: Missing '>' for input next several commands in CLI after programm running
  finished ([ed7cf0bc](https://github.com/zowe/zowe-explorer-intellij/commit/ed7cf0bc))
* Bugfix: Move member to another PDS refreshes only one
  PDS ([7c693076](https://github.com/zowe/zowe-explorer-intellij/commit/7c693076))
* Bugfix: Content encoding change after uss read only file
  reopened ([0a4d638f](https://github.com/zowe/zowe-explorer-intellij/commit/0a4d638f))
* Bugfix: Refresh does not work if copy-delete-copy one USS folder to another USS
  folder ([656fb37e](https://github.com/zowe/zowe-explorer-intellij/commit/656fb37e))
* Bugfix: IndexOutOfBoundsException if create JWS via context
  menu ([c40bb42a](https://github.com/zowe/zowe-explorer-intellij/commit/c40bb42a))
* Bugfix: Exception in Zowe Explorer when there is a configuration from For Mainframe plugin
  exist ([7cee23f3](https://github.com/zowe/zowe-explorer-intellij/commit/7cee23f3))
* Bugfix: Policy agreement is gone wild ([d600f58e](https://github.com/zowe/zowe-explorer-intellij/commit/d600f58e))
* Bugfix: Exception while opening TSO CLI ([540bfa80](https://github.com/zowe/zowe-explorer-intellij/commit/540bfa80))

## [0.7.1] (2022-11-30)

### Features

* Feature: Unit tests for utils
  module ([16fae1fb](https://github.com/zowe/zowe-explorer-intellij/commit/16fae1fb)) ([683185bb](https://github.com/zowe/zowe-explorer-intellij/commit/683185bb))

### Bugfixes

* Bugfix: DnD does not work properly ([e5dfa3a3](https://github.com/zowe/zowe-explorer-intellij/commit/e5dfa3a3))
* Bugfix: Copy DS member to USS folder does not
  work ([1e94ec48](https://github.com/zowe/zowe-explorer-intellij/commit/1e94ec48))
* Bugfix: Unknown type of file if copy-delete-copy the same PDS
  member ([21651646](https://github.com/zowe/zowe-explorer-intellij/commit/21651646))
* Bugfix: Ctrl+C/Ctrl+V does not work if copy file from remote to
  local ([e5601e7f](https://github.com/zowe/zowe-explorer-intellij/commit/e5601e7f))

## [0.7.0] (2022-10-31)

### Breaking changes

* Breaking: Kotlin DSL v2 usage introduced. Plugin requires to use it with IntelliJ version >= 2022.1

### Features

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

### Bugfixes

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

[1.2.4-221]: https://github.com/for-mainframe/For-Mainframe/compare/1.2.3-221...1.2.4-221
[1.2.3-221]: https://github.com/for-mainframe/For-Mainframe/compare/1.2.2-221...1.2.3-221
[1.2.2-221]: https://github.com/for-mainframe/For-Mainframe/compare/1.2.1-221...1.2.2-221
[1.2.1-221]: https://github.com/for-mainframe/For-Mainframe/compare/1.2.0-221...1.2.1-221
[1.2.0-221]: https://github.com/for-mainframe/For-Mainframe/compare/1.1.2-221...1.2.0-221
[1.1.2-221]: https://github.com/for-mainframe/For-Mainframe/compare/1.1.1-221...1.1.2-221
[1.1.1-221]: https://github.com/for-mainframe/For-Mainframe/compare/1.1.0-221...1.1.1-221
[1.1.0-221]: https://github.com/for-mainframe/For-Mainframe/compare/1.0.2-221...1.1.0-221
[1.0.2-221]: https://github.com/for-mainframe/For-Mainframe/compare/1.0.1...1.0.2-221
[1.0.1]: https://github.com/for-mainframe/For-Mainframe/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/for-mainframe/For-Mainframe/compare/0.7.1...1.0.0
[0.7.1]: https://github.com/for-mainframe/For-Mainframe/compare/0.7.0...0.7.1
[0.7.0]: https://github.com/for-mainframe/For-Mainframe/commits/0.7.0
