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

group = "org.zowe"
version = "1.0.1"
val remoteRobotVersion = "0.11.16"

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
  implementation("com.squareup.okhttp3:okhttp:4.10.0")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.20")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.20")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("org.jgrapht:jgrapht-core:1.5.1")
  implementation("com.starxg:java-keytar:1.0.0")
  implementation("org.zowe.sdk:zowe-kotlin-sdk:0.4.0")
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
        <li>Support for IntelliJ 2023.1</li>
      </ul>
      <br>
      <b>Fixed bugs:</b>
      <ul>
        <li>Runtime Exception in Zowe Explorer when delete dataset</li>
        <li>File is not displayed after folder moved inside another folder</li>
        <li>IDE freeze after closing CLI during command execution with broken coonection</li>
        <li>Last opened file remains active in editor</li>
        <li>Duplicate widgets when installing For Mainframe and Zowe Explorer plugins together</li>
        <li>Changed parameters in edit connection dialog do not reset after cancelation</li>
        <li>Incorrect reloading on USS encoding change</li>
        <li>println in TSO CLI</li>
        <li>Copy DS member from one host to USS folder on another host does not work</li>
        <li>Jobs filter is created with wrong default user</li>
        <li>"Access is allowed from Event Dispatch Thread (EDT) only" on the plugin debug</li>
        <li>SonarCloud compaint on Random</li>
        <li>Autosync works strange</li>
        <li>Strange behavior on copy paste from remote to local</li>
        <li>Error while trying to move PS inside PDS</li>
        <li>USS file empty after rename</li>
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
