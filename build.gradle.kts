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
import org.jetbrains.changelog.Changelog
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
fun dateValue(pattern: String): String =
  LocalDate.now(ZoneId.of("Europe/Warsaw")).format(DateTimeFormatter.ofPattern(pattern))

buildscript {
  dependencies {
    classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.1.1")
  }
}

plugins {
  id("org.sonarqube") version "3.3"
  id("org.jetbrains.intellij") version "1.17.2"
  id("org.jetbrains.changelog") version "2.2.0"
  kotlin("jvm") version "1.9.22"
  java
  id("org.jetbrains.kotlinx.kover") version "0.6.1"
  id("org.owasp.dependencycheck") version "9.1.0"
}

val sonarLinksCi: String by project

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")
apply(from = "gradle/sonar.gradle")

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()
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
  version.set(properties("platformVersion").get())
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
  version.set(properties("pluginVersion").get())
  header.set(provider { "${version.get()} (${dateValue("yyyy-MM-dd")})" }.get())
  groups.set(listOf("Breaking changes", "Features", "Bugfixes", "Deprecations", "Security"))
  keepUnreleasedSection.set(false)
  itemPrefix.set("*")
  repositoryUrl.set(properties("pluginRepositoryUrl").get())
  sectionUrlBuilder.set { repositoryUrl, currentVersion, previousVersion, isUnreleased: Boolean ->
    repositoryUrl + when {
      isUnreleased -> when (previousVersion) {
        null -> "/commits"
        else -> "/compare/$previousVersion...HEAD"
      }

      previousVersion == null -> "/commits/$currentVersion"
      else -> "/compare/$previousVersion...$currentVersion"
    }
  }
}

tasks {
  wrapper {
    gradleVersion = properties("gradleVersion").get()
  }

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
    version.set(properties("pluginVersion").get())
    sinceBuild.set(properties("pluginSinceBuild").get())
    untilBuild.set(properties("pluginUntilBuild").get())

    val changelog = project.changelog // local variable for configuration cache compatibility
    // Get the latest available change notes from the changelog file
    changeNotes.set(
      properties("pluginVersion")
        .map { pluginVersion ->
          with(changelog) {
            renderItem(
              (getOrNull(pluginVersion) ?: getUnreleased())
                .withHeader(false)
                .withEmptySections(false),
              Changelog.OutputType.HTML,
            )
          }
        }
        .get()
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

  signPlugin {
    certificateChain.set(environment("INTELLIJ_SIGNING_CERTIFICATE_CHAIN").map { it })
    privateKey.set(environment("INTELLIJ_SIGNING_PRIVATE_KEY").map { it })
    password.set(environment("INTELLIJ_SIGNING_PRIVATE_KEY_PASSWORD").map { it })
  }

  publishPlugin {
    dependsOn("patchChangelog")
    token.set(environment("INTELLIJ_SIGNING_PUBLISH_TOKEN").map { it })
    // The pluginVersion is based on the SemVer (https://semver.org)
    // Read more: https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    channels.set(
      properties("pluginVersion")
        .map {
          listOf(
            it.substringAfter('-', "")
              .substringAfter('-', "")
              .substringBefore('.')
              .ifEmpty { "stable" }
          )
        }
        .map { it })
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
