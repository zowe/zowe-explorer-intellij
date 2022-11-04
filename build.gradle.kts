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

buildscript {
  dependencies {
    classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.1.1")
  }
}

plugins {
  id("org.sonarqube") version "3.3"
  id("org.jetbrains.intellij") version "1.9.0"
  kotlin("jvm") version "1.6.21"
  java
  jacoco
}

val sonarLinksCi: String by project

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")
apply(from = "gradle/sonar.gradle")

group = "org.zowe"
version = "0.3.0"
val remoteRobotVersion = "0.11.14"

repositories {
  mavenCentral()
  flatDir {
    dirs("libs")
  }
  maven {
    url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
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
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
  implementation("org.jgrapht:jgrapht-core:1.5.1")
  implementation("com.starxg:java-keytar:1.0.0")
  implementation("org.zowe:kotlinsdk:0.3.0")
  testImplementation("io.mockk:mockk:1.12.4")
  testImplementation("org.mock-server:mockserver-netty:5.13.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
  testImplementation("io.kotest:kotest-assertions-core:5.3.1")
  testImplementation("io.kotest:kotest-runner-junit5:5.3.1")
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
    sinceBuild.set("221.5080")
    untilBuild.set("222.*")
    changeNotes.set(
      """
      <b>WARNING: </b> version 0.3 introduces breaking change. You won't be able to use the plugin with IntelliJ version < 2022.1
      <br>
      <b>New features:</b>
      <ul>
        <li>Configurable batch size to load filter smoothly</li>
        <li>Job Purge operation</li>
        <li>Job Edit operation</li>
        <li>Copy local to remote</li>
        <li>Copy remote to remote</li>
        <li>GitHub issue #10: Edit Working sets directly from Tool Window</li>
        <li>GitHub issue #70: Add date and time to JES Explorer</li>
      </ul>
      <br>
      <b>Minor changes:</b>
      <ul>
        <li>Copy remote to local: clarify warning</li>
        <li>GitHub issue #67: Allocate like for datasets with BLK will be with warning</li>
        <li>Move the file attribute conversion to a separate thread</li>
        <li>Source code documentation added</li>
      </ul>
      <br>
      <b>Fixed bugs:</b>
      <ul>
        <li>File cache conflict if open JCL to edit it in JES explorer second time</li>
        <li>GitHub issue #86: Incorrect error message if mask length > 44</li>
        <li>GitHub issue #87: Masks type autodetection does not work in Add/Edit Working Set dialogs</li>
        <li>Problem with automatic refresh after creating new members/deleting members from dataset</li>
        <li>Confusing dialog title 'Rename Directory' when renaming USS mask from context menu</li>
        <li>GitHub issue #81: There is no difference between upper and lower cases when create USS masks from context menu</li>
        <li>GitHub issue #88: Lower case is not changed to upper case during Job Filter creation</li>
        <li>GitHub issue #44: 'Sync data' button does not work properly when multiple changes in USS file</li>
        <li>GitHub issue #30: Create new member in dataset that does not have enough space creates empty member despite of warning</li>
        <li>GitHub issue #54: Accumulation of errors in WS that breaks WS</li>
        <li>USS file cannot be deleted in development branch</li>
        <li>z/OS version specified in connection information doesn't match the z/OS version returned from z/OSMF</li>
        <li>Zowe config connection test always failed</li>
        <li>IDE error with ReadOnlyModificationException when set 'use binary mode' for read only uss-file</li>
        <li>GitHub issue #94: SYSPRINT I looked at first always opens in JES explorer for a job with multiple steps</li>
        <li>IDE error with CallException when try to open uss-file to which you have no access</li>
        <li>The content of sequential dataset/member is changed anyway even if you choose do not sync data with mainframe</li>
        <li>IDE error while retrieving job list in JES Explorer</li>
        <li>Extra item 'Rename' is active in the context menu if click on 'loading...'/'load more' in file explorer</li>
        <li>GitHub issue #16: Error creating zOSMF connection</li>
        <li>GitHub issue #85: The windows 'Add Working Set'/'Edit Working Set' are automatically resized if z/OSMF connection with very long name is added</li>
        <li>Impossible to open any file/dataset second time</li>
        <li>The job is marked with green icon as passed despite it finished with abend</li>
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
  systemProperty("ide.show.tips.on.startup.default.value", "false")
}

tasks {
  withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}
