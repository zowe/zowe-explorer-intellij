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
  id("org.jetbrains.intellij") version "1.13.0"
  kotlin("jvm") version "1.7.10"
  java
  jacoco
}

val sonarLinksCi: String by project

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")
apply(from = "gradle/sonar.gradle")

group = "eu.ibagroup"
version = "1.0.0"
val remoteRobotVersion = "0.11.16"

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
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
  implementation(group = "com.squareup.retrofit2", name = "retrofit", version = "2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
  implementation("com.squareup.okhttp3:okhttp:4.10.0")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.20")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.20")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("org.jgrapht:jgrapht-core:1.5.1")
  implementation("eu.ibagroup:r2z:1.3.0")
  implementation("com.segment.analytics.java:analytics:+")
  implementation("com.ibm.mq:com.ibm.mq.allclient:9.3.0.0")
  testImplementation("io.mockk:mockk:1.13.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
  testImplementation("io.kotest:kotest-assertions-core:5.5.2")
  testImplementation("io.kotest:kotest-runner-junit5:5.5.2")
  testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
  testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.0")
}

intellij {
  version.set("2022.3")
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_17.toString()
      languageVersion = org.jetbrains.kotlin.config.LanguageVersion.LATEST_STABLE.versionString
    }
  }

  patchPluginXml {
    sinceBuild.set("223.7571")
    untilBuild.set("223.*")
    changeNotes.set(
      """
      <b>WARNING: </b> version 1.0 introduces breaking change. You won't be able to use the plugin with IntelliJ version less than 2022.3
      <br>
      <br>
      <b>New features:</b>
      <ul>
        <li>TSO CLI</li>
        <li>Different encodings support (all IBM encodings are supported)</li>
        <li>Support for CHMOD operation</li>
        <li>Support for big files and datasets</li>
        <li>JES Explorer: purge, edit, view job</li>
        <li>Added "+" sign expandability: now it is possible to create connection / working set / mask through the revealing context menu on "+" click</li>
        <li>Migrate to Kotlin DSL v2</li>
        <li>All the code is documented now</li>
      </ul>
      <br>
      <b>Minor changes:</b>
      <ul>
        <li>Manual sync was proceeding in main thread</li>
        <li>Codepage selection is removed from connection dialog</li>
        <li>Added UI regression tests and Unit tests</li>
        <li>Unit tests are written in Kotest now</li>
        <li>Build warnings fixed</li>
        <li>Other plugin's stability issues</li>
      </ul>
      <br>
      <b>Fixed bugs:</b>
      <ul>
        <li>Error in event log when copy member to PDS that does not have enough space</li>
        <li>The creating z/OS mask '*.*' is not blocked</li>
        <li>Impossible to rename USS directory whose name contains &</li>
        <li>Problem with automatic refresh after creating new dataset/allocate like</li>
        <li>Incorrect data encoding</li>
        <li>When dataset member is moved from one DS to another, load more appears instead of it</li>
        <li>Strange behavior of automatic reload and batch_size</li>
        <li>USS files are not edible</li>
        <li>Incompatible with IntelliJ 2022.3</li>
        <li>IDE error with UnsupportedEncodingException for some encodings</li>
        <li>IDE error with ReadOnlyModificationException when change encoding for read only file</li>
        <li>Impossible to close uss-file with write permission after changing encoding</li>
        <li>There is no warning if copy/paste from remote to local</li>
        <li>Synchronization is cycled in autosync mode after first opening for the file</li>
        <li>IDE error with UndeclaredThrowableException while closing CLI when connection was broken</li>
        <li>Cancel DnD several members from one host to another does not work properly</li>
        <li>IDE error with NoSuchElementException if start CLI when there is no any connections</li>
        <li>Differences in the interface (field highlighting)</li>
        <li>Vertical scrollbar in 'Add Working Set/Edit Working Set' does not work properly if you add a lot of masks</li>
        <li>Different colmn name JES vs Jobs Working Set</li>
        <li>z/OS mask is created in lowercase if use dataset name in lowercase during allocate/allocate like</li>
        <li>Sync data does not work correctly when the content has not changed</li>
        <li>Missing warning if delete connection that has any jobs working set</li>
        <li>Last mask/filter is created in wrong way in Edit Working Set/Edit Jobs Working Set dialogs via context menu</li>
        <li>File upload icon is cycling when double-clicking again on an open file</li>
        <li>Small typo in annotation</li>
        <li>The button 'Ok' on Warning when delete connections with ws/jws</li>
        <li>Typo in error message in Allocate Dataset dialog</li>
        <li>Typo in release note for 1.0.0</li>
        <li>Typo in message for incorrect directory quantity in allocate dataset</li>
        <li>Unhandled error type for jobs</li>
        <li>Missing '>' for input next several commands in CLI after programm running finished</li>
        <li>Move member to another PDS refreshes only one PDS</li>
        <li>Content encoding change after uss read only file reopened</li>
        <li>Refresh does not work if copy-delete-copy one USS folder to another USS folder</li>
        <li>IndexOutOfBoundsException if create JWS via context menu</li>
        <li>Automatic refresh does not work correctly for job filter after purge job via context menu</li>
        <li>Exception in Zowe Explorer when there is a configuration from For Mainframe plugin exist</li>
        <li>Policy agreement is gone wild</li>
        <li>Exception while opening TSO CLI</li>
        <li>Exception during IDE startup with plugin</li>
        <li>Operation is not supported for read-only collection while trying to create JES Working set</li>
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
    reports {
      xml.required.set(true)
      html.required.set(false)
      xml.outputLocation.set(File("${buildDir}/reports/jacoco.xml"))
    }
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

tasks.test {
  systemProperty("idea.force.use.core.classloader", "true")
  systemProperty("idea.use.core.classloader.for.plugin.path", "true")
}
