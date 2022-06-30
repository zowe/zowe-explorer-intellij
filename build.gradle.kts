/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.intellij") version "1.6.0"
  kotlin("jvm") version "1.6.21"
  java
  jacoco
}

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")

group = "eu.ibagroup"
version = "0.6.1"
val remoteRobotVersion = "0.11.14"

repositories {
  mavenCentral()
  maven {
    url = uri("http://10.221.23.186:8082/repository/internal/")
    isAllowInsecureProtocol = true
    credentials {
      username = "admin"
      password = "password123"
    }
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    flatDir {
      dir("libs")
    }
    metadataSources {
      mavenPom()
      artifact()
      ignoreGradleMetadataRedirection()
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
  implementation(group = "com.squareup.retrofit2", name = "retrofit", version = "2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.20")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.20")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
  implementation("org.jgrapht:jgrapht-core:1.5.1")
  implementation("eu.ibagroup:r2z:1.0.27")
  implementation("com.segment.analytics.java:analytics:+")
  testImplementation("io.mockk:mockk:1.10.2")
  testImplementation("org.mock-server:mockserver-netty:5.11.1")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
  testImplementation("io.kotest:kotest-assertions-core:5.3.0")
  testImplementation("io.kotest:kotest-runner-junit5:5.3.0")
  testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
  testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.8.2")
}

intellij {
  version.set("2022.1")
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_11.toString()
      languageVersion = org.jetbrains.kotlin.config.LanguageVersion.LATEST_STABLE.versionString
    }
  }

  patchPluginXml {
    sinceBuild.set("203.5981")
    untilBuild.set("221.*")
    changeNotes.set(
      """
      <b>New features:</b>
      <ul>
        <li>Added ability to copy from remote host to a local machine</li>
        <li>Added Job Console log view. Actions on Jobs are being developed</li>
        <li>Some major fixes and minor features. See in detailed report</li>
      </ul>
      <b>Detailed changes list:</b>
      <ul>
        <li>Copy from remote to local</li>
        <li>Change lower case to upper case during DS mask creation</li>
        <li>Implemented Control panel in Job Console log view to operate with job</li>
        <li>Job Release operation runner</li>
        <li>Job Hold operation runner</li>
        <li>Job Cancel operation runner</li>
        <li>Autosync rework</li>
        <li>ScrollPanel in "Allocate Dataset" frame</li>
        <li>Separate icons for Datasets and USS folders</li>
        <li>Force rename option</li>
        <li>Implemented ability to add Job Filter in 'New' popup menu clicking on JobWorkingSet</li>
        <li>z/OSMF 2.5 support</li>
        <li>log4j is removed</li>
        <li>Username/Password validatiion during connection creation</li>
      </ul>
      <b>Bugs fixed:</b>
      <ul>
        <li>The creation of masks with three or more asterisks is not blocked</li>
        <li>NPE when notification was closed</li>
        <li>Error when the prefix length for JobFilter is more than 8 characters or Prefix contains unacceptable symbols</li>
        <li>Wrong Directory allocation restrictions</li>
        <li>Error 404 on try to create a dataset</li>
        <li>Block size is being removed on error</li>
        <li>Missing UI error message for z/OS mask with length more than 44 chars</li>
        <li>Adding job filter through context menu item in JES Explorer doesn't work correctly</li>
        <li>Creating z/OS mask with first digit in HLQ is not blocked</li>
        <li>Right click->New->WorkingSet on JES Explorer page opens dialog to create WorkingSet instead of JobsWorkingSet</li>
        <li>Creating mask with length=44 chars should be available</li>
        <li>The error message contains java exception when uncheck "Accept self-signed SSL certiаficates" in the connection</li>
        <li>Delete option does not work for Working Sets in File Explorer</li>
        <li>Renaming an uss folder/file and then renaming it back to its original name causes an error</li>
        <li>No message details when renaming dataset to existing name</li>
        <li>Error message for failed dataset allocation should be duplicated in Dataset Allocation window</li>
        <li>Impossible to close job's output</li>
        <li>IDE Fatal errors if delete the connection that has working set</li>
        <li>Use binary mode changes file contents</li>
        <li>NullPointerException when tree opens by user with incorrect password</li>
        <li>Impossible to close the file located locally on PC (the file was opened not from plugin)</li>
        <li>Fixed file content corruption after chaging file mode</li>
        <li>InfoOperation requires state instead of connection settings</li>
        <li>"Error" message if connection is deleted</li>
        <li>The job is marked as finished in job console if close tab for any another job</li>
        <li>Hold/release only works for first selected jobid</li>
        <li>Validation of directory blocks when creating dataset</li>
        <li>File cache conflict message if create new member in PDS and open it right after creation</li>
        <li>Validate jobId when creating job filter</li>
        <li>CredentialsNotFoundForConnection exception in plugin</li>
        <li>Creating Job Filters in configuration table with prefix/owner/job id length >8 is not blocked</li>
        <li>'Test passed' message in the jobs console</li>
        <li>Allocate like does not save some parameters</li>
        <li>GitHub issue #42</li>
      </ul>"""
    )
  }

  test {
    useJUnitPlatform()
    testLogging {
      events("passed", "skipped", "failed")
    }

    configure<JacocoTaskExtension> {
      isIncludeNoLocationClasses = true
      excludes = listOf("jdk.internal.*")
    }

    finalizedBy("jacocoTestReport")
  }

  jacocoTestReport {
    classDirectories.setFrom(
      files(classDirectories.files.map {
        fileTree(it) {
          exclude("${buildDir}/instrumented/**")
        }
      })
    )
  }
}

/**
 * Adds uiTest source sets
 */
sourceSets {
  create("uiTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
    java.srcDirs("src/uiTest/java", "src/uiTest/kotlin")
    resources.srcDirs("src/uiTest/resources")
  }
}

/**
 * configures the UI Tests to inherit the testImplementation in dependencies
 */
val uiTestImplementation by configurations.getting {
  extendsFrom(configurations.testImplementation.get())
}

/**
 * configures the UI Tests to inherit the testRuntimeOnly in dependencies
 */
configurations["uiTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

/**
 * runs UI tests
 */
val uiTest = task<Test>("uiTest") {
  description = "Runs the integration tests for UI."
  group = "verification"
  testClassesDirs = sourceSets["uiTest"].output.classesDirs
  classpath = sourceSets["uiTest"].runtimeClasspath
  useJUnitPlatform() {
    excludeTags("FirstTime")
  }
  testLogging {
    events("passed", "skipped", "failed")
  }
}

/**
 * Runs the first UI test, which agrees to the license agreement
 */
val firstTimeUiTest = task<Test>("firstTimeUiTest") {
  description = "Gets rid of license agreement, etc."
  group = "verification"
  testClassesDirs = sourceSets["uiTest"].output.classesDirs
  classpath = sourceSets["uiTest"].runtimeClasspath
  useJUnitPlatform() {
    includeTags("FirstTime")
  }
  testLogging {
    events("passed", "skipped", "failed")
  }
}

tasks.downloadRobotServerPlugin {
  version.set(remoteRobotVersion)
}

tasks.runIdeForUiTests {
  systemProperty("idea.trust.all.projects", "true")
  systemProperty("ide.show.tips.on.startup.default.value","false")
}

tasks {
  withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}
