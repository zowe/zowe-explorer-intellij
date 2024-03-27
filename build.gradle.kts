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
  id("org.jetbrains.intellij") version "1.17.2"
  kotlin("jvm") version "1.9.22"
  java
  id("org.jetbrains.kotlinx.kover") version "0.6.1"
}

val sonarLinksCi: String by project

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")
apply(from = "gradle/sonar.gradle")

group = "eu.ibagroup"
version = "1.2.0-221"
val remoteRobotVersion = "0.11.22"
val okHttp3Version = "4.12.0"
val kotestVersion = "5.8.1"

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
  implementation("com.squareup.okhttp3:okhttp:$okHttp3Version")
  implementation("org.jgrapht:jgrapht-core:1.5.2")
  implementation("org.zowe.sdk:zowe-kotlin-sdk:0.4.0")
  implementation("com.segment.analytics.java:analytics:3.5.0")
  implementation("com.ibm.mq:com.ibm.mq.allclient:9.3.4.1")
  testImplementation("io.mockk:mockk:1.13.9")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
  testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
  testImplementation("com.squareup.okhttp3:mockwebserver:$okHttp3Version")
  testImplementation("com.squareup.okhttp3:okhttp-tls:$okHttp3Version")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.1")
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

  withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }

  patchPluginXml {
    sinceBuild.set("221.5080")
    untilBuild.set("222.*")
    changeNotes.set(
      """
      <b>New features:</b>
      <ul>
        <li>None at the moment</li>
      </ul>
      <br>
      <b>Fixed bugs:</b>
      <ul>
        <li>Sync action does not work after file download</li>
        <li>"Skip This Files" doesn't work when uploading local file to PDS</li>
        <li>"Use new name" doesn't work for copying partitioned dataset to USS folder</li>
        <li>"Use new name" doesn't work for copying sequential dataset to partitioned dataset</li>
        <li>"Use new name" doesn't work when uploading local file to PDS</li>
        <li>Editing two members with the same name does not update the content for one of the members</li>
        <li>Topics handling</li>
        <li>Zowe config v2 handling</li>
        <li>JES Explorer bug when ABEND job is being displayed</li>
        <li>GitHub issue #167: Zowe explorer config is not converted</li>
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
      include("**/eu/ibagroup/formainframe/**/*.java")
    }
    // Kotlin sources
    from(kotlin.sourceSets.main.get().kotlin) {
      include("**/eu/ibagroup/formainframe/**/*.kt")
    }
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveClassifier.set("src")
  }

  buildPlugin {
    dependsOn(createOpenApiSourceJar)
    from(createOpenApiSourceJar) { into("lib/src") }
  }

  downloadRobotServerPlugin {
    version.set(remoteRobotVersion)
  }

  runIdeForUiTests {
    systemProperty("idea.trust.all.projects", "true")
    systemProperty("ide.show.tips.on.startup.default.value", "false")
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
    // set to true to disable instrumentation of this task,
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
