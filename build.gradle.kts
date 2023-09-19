/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

import kotlinx.kover.api.KoverTaskExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.1.1")
  }
}

plugins {
  id("org.sonarqube") version "3.3"
  id("org.jetbrains.intellij") version "1.14.2"
  kotlin("jvm") version "1.8.10"
  java
  id("org.jetbrains.kotlinx.kover") version "0.6.1"
}

val sonarLinksCi: String by project

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")
apply(from = "gradle/sonar.gradle")

group = "org.zowe"
version = "1.1.1-223"
val remoteRobotVersion = "0.11.19"
val okHttp3Version = "4.10.0"
val kotestVersion = "5.6.2"

repositories {
  mavenCentral()
  flatDir {
    dirs("libs")
  }
  maven {
    url = uri("https://zowe.jfrog.io/zowe/libs-release")
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
  implementation("com.squareup.okhttp3:okhttp:$okHttp3Version")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.20")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.20")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("org.jgrapht:jgrapht-core:1.5.1")
  implementation("com.starxg:java-keytar:1.0.0")
  implementation("org.zowe.sdk:zowe-kotlin-sdk:0.4.0")
  implementation("com.ibm.mq:com.ibm.mq.allclient:9.3.3.0")
  testImplementation("io.mockk:mockk:1.13.5")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
  testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
  testImplementation("com.squareup.okhttp3:mockwebserver:$okHttp3Version")
  testImplementation("com.squareup.okhttp3:okhttp-tls:$okHttp3Version")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.2")
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
      <b>New features:</b>
      <ul>
        <li>GitHub issue #14: UX: Edit WS mask</li>
        <li>GitHub issue #23: Double click on a working set or connection</li>
        <li>GitHub issue #49: Plugin logging</li>
        <li>GitHub issue #52: Presets for creating datasets</li>
        <li>GitHub issue #111: "Rename" in dialog window should be "Edit" for DS and USS masks</li>
        <li>GitHub issue #112: Migrate all UI tests from real data usage to mock server</li>
        <li>GitHub issue #113: Change user password feature</li>
        <li>GitHub issue #122: "whoami" on connection creation</li>
        <li>GitHub issue #123: Implement "No items found" for USS and DS masks</li>
        <li>GitHub issue #124: Clarify DS organization</li>
        <li>GitHub issue #125: 80 LRECL by default</li>
        <li>GitHub issue #126: Copy + rename</li>
        <li>GitHub issue #130: JDK search index broken in IntelliJ after dataset is open</li>
        <li>GitHub issue #136: CLEARTEXT communication not enabled for client</li>
        <li>GitHub issue #140: Exception in Zowe Explorer (1.0.2-221) for Android Studio(Android Studio Flamingo | 2022.2.1 Patch 2)</li>
        <li>GitHub issue #144: Incorrect encoding should not be changed directly, until a user is decided to change it when we suggest</li>
        <li>GitHub issue #145: Migrated dataset properties should not be visible if they are not available</li>
        <li>GitHub issue #146: Hints for creating working sets after connection is created</li>
        <li>GitHub issue #147: "Duplicate" for member</li>
        <li>GitHub issue #148: Warning about incompatible encodings</li>
        <li>Separate info and tso requests during connection test</li>
        <li>Rework configs in the plug-in to accept new configurables</li>
        <li>Rework file sync with MF</li>
        <li>Presets: improvement</li>
        <li>VFS_CHANGES to MF_VFS_CHANGES</li>
        <li>Change XML and JSON comparison on different plugin versions</li>
        <li>Substitute R2Z with Zowe Kotlin SDK</li>
        <li>Enhance configuration for CICS connections</li>
        <li>Unit tests</li>
      </ul>
      <br>
      <b>Fixed bugs:</b>
      <ul>
        <<li>GitHub issue #138: Job is identified as successful while it ends with RC=12</li>
        <li>Tooltip on JES Working set shows 'Working set'</li>
        <li>"debounce" test is failed sometimes</li>
        <li>Allocate like strange behavior</li>
        <li>Change permissions: incorrect permissions shown after change failure</li>
        <li>Empty pds is not deleted after its move to pds</li>
        <li>It shows that the job is still running after successfull purge</li>
        <li>Errors for several actions in JES Explorer</li>
        <li>Error if move empty PS to PDS</li>
        <li>Password is changed only for one connection</li>
        <li>Userid for new ws/jws is not changed in FileExplorer/JesExplorer after changes in corresponding connection</li>
        <li>FileNotFoundException on configs search (The system cannot find the file specified)</li>
        <li>Content of uss-file is changed to UTF-8 while copying it from remote to local</li>
        <li>Copy/paste and DnD of PS dataset from one host to uss-folder on another host does not work</li>
        <li>validateForGreaterValue should show correct message</li>
        <li>JCL highlight does not work on mainframe files</li>
        <li>IDE error when rename member/dataset to existing one/to the same</li>
        <li>ClassCastException: class java.util.ArrayList cannot be cast to class com.intellij.openapi.vfs.VirtualFile</li>
      </ul>"""
    )
  }

  test {
    useJUnitPlatform()
    testLogging {
      events("passed", "skipped", "failed")
    }

//    ignoreFailures = true

    finalizedBy("koverHtmlReport")
    finalizedBy("koverXmlReport")
    systemProperty("idea.force.use.core.classloader", "true")
    systemProperty("idea.use.core.classloader.for.plugin.path", "true")
    systemProperty("java.awt.headless", "true")

    afterSuite(
      KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
        if (desc.parent == null) { // will match the outermost suite
          val output =
            "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, " +
                "${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
          val fileName = "./build/reports/tests/${result.resultType}.txt"
          File(fileName).writeText(output)
        }
      })
    )
  }

  val createOpenApiSourceJar by registering(Jar::class) {
    // Java sources
    from(sourceSets.main.get().java) {
      include("**/org/zowe/explorer/**/*.java")
    }
    // Kotlin sources
    from(kotlin.sourceSets.main.get().kotlin) {
      include("**/org/zowe/explorer/**/*.kt")
    }
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveClassifier.set("src")
  }

  buildPlugin {
    dependsOn(createOpenApiSourceJar)
    from(createOpenApiSourceJar) { into("lib/src") }
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
    excludeTags("SmokeTest")
  }
  testLogging {
    events("passed", "skipped", "failed")
  }
  extensions.configure(KoverTaskExtension::class) {
    // Set to true to disable instrumentation of this task,
    // Kover reports will not depend on the results of its execution
    isDisabled.set(true)
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
  extensions.configure(KoverTaskExtension::class) {
    // set to true to disable instrumentation of this task,
    // Kover reports will not depend on the results of its execution
    isDisabled.set(true)
  }
}

/**
 * Runs the smoke ui test
 */
val SmokeUiTest = task<Test>("smokeUiTest") {
  description = "Gets rid of license agreement, etc."
  group = "verification"
  testClassesDirs = sourceSets["uiTest"].output.classesDirs
  classpath = sourceSets["uiTest"].runtimeClasspath
  useJUnitPlatform() {
    includeTags("SmokeTest")
  }
  testLogging {
    events("passed", "skipped", "failed")
  }
  extensions.configure(KoverTaskExtension::class) {
    // set to true to disable instrumentation of this task,
    // Kover reports will not depend on the results of its execution
    isDisabled.set(true)
  }
}

tasks {
  withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}

tasks.downloadRobotServerPlugin {
  version.set(remoteRobotVersion)
}

tasks.runIdeForUiTests {
  systemProperty("idea.trust.all.projects", "true")
  systemProperty("ide.show.tips.on.startup.default.value", "false")
}
