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
version = "1.0.2-221"
val remoteRobotVersion = "0.11.18"

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
      <b>WARNING: </b> version 1.0 introduces breaking change. You won't be able to use the plugin with IntelliJ version less than 2022.1
      <br>
      <br>
      <b>New features:</b>
      <ul>
        <li>Returned support for IntelliJ 2022.1</li>
        <li>Focus on dataset name field in allocation dialog</li>
      </ul>
      <br>
      <b>Fixed bugs:</b>
      <ul>
        <li>Memory leak bug</li>
        <li>GitHub issue #132: IDE internal error - NPE</li>
        <li>Access denied error when copy from remote to local file when local has folder with the same name</li>
        <li>Paste to dataset with LRECL does not move exceeding characters to a new line</li>
        <li>USS file with 0 permissions is not accessible and no error message displayed</li>
        <li>Refresh does not work for job filter with one job after purge</li>
        <li>Name conflict message if move uss-file from folder to mask and then back</li>
        <li>File cash conflict</li>
        <li>Cancel button does not work for TSO connection test during</li>
        <li>Unknown file type after delete member after move</li>
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
