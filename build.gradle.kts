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
  kotlin("jvm") version "1.7.10"
  java
  id("org.jetbrains.kotlinx.kover") version "0.6.1"
}

val sonarLinksCi: String by project

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")
apply(from = "gradle/sonar.gradle")

group = "org.zowe"
version = "1.1.0-231"
val remoteRobotVersion = "0.11.18"
val okHttp3Version = "4.10.0"
val kotestVersion = "5.5.5"

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
  implementation("com.ibm.mq:com.ibm.mq.allclient:9.3.0.0")
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
  version.set("2023.1")
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_17.toString()
      languageVersion = org.jetbrains.kotlin.config.LanguageVersion.LATEST_STABLE.versionString
    }
  }

  patchPluginXml {
    sinceBuild.set("231.8109")
    untilBuild.set("231.*")
    changeNotes.set(
      """
      <b>WARNING: </b> version 1.0 introduces breaking change. You won't be able to use the plugin with IntelliJ IDEA™ version less than 2022.1
      <br>
      <br>
      <b>Minor changes:</b>
      <ul>
        <li>The plug-in's description and name update</li>
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
// TODO: setup Kover
//    reports {
//      xml.required.set(true)
//      xml.outputLocation.set(File("${buildDir}/reports/jacoco.xml"))
//    }
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
