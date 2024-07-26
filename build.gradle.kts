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
import org.jetbrains.intellij.tasks.ClasspathIndexCleanupTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
fun dateValue(pattern: String): String =
  LocalDate.now(ZoneId.of("Europe/Warsaw")).format(DateTimeFormatter.ofPattern(pattern))

plugins {
  id("org.jetbrains.intellij") version "1.17.2"
  id("org.jetbrains.changelog") version "2.2.0"
  kotlin("jvm") version "1.9.22"
  java
  id("org.jetbrains.kotlinx.kover") version "0.6.1"
  id("org.owasp.dependencycheck") version "9.1.0"
}

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()
val remoteRobotVersion = "0.11.22"
val okHttp3Version = "4.12.0"
val kotestVersion = "5.8.1"
val retrofit2Vertion = "2.11.0"
val junitVersion = "5.10.2"
val mockkVersion = "1.13.10"
val ibmMqVersion = "9.3.5.0"
val jGraphTVersion = "1.5.2"
val zoweKotlinSdkVersion = "0.5.0-rc.8"
val javaAnalyticsVersion = "3.5.1"

repositories {
  mavenCentral()
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
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
  implementation(group = "com.squareup.retrofit2", name = "retrofit", version = retrofit2Vertion)
  implementation("com.squareup.retrofit2:converter-gson:$retrofit2Vertion")
  implementation("com.squareup.retrofit2:converter-scalars:$retrofit2Vertion")
  implementation("com.squareup.okhttp3:okhttp:$okHttp3Version")
  implementation("org.jgrapht:jgrapht-core:$jGraphTVersion")
  implementation("org.zowe.sdk:zowe-kotlin-sdk:$zoweKotlinSdkVersion")
  implementation("com.segment.analytics.java:analytics:$javaAnalyticsVersion")
  implementation("com.ibm.mq:com.ibm.mq.allclient:$ibmMqVersion")
  implementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
  testImplementation("io.mockk:mockk:$mockkVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
  testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
  testImplementation("com.squareup.okhttp3:mockwebserver:$okHttp3Version")
  testImplementation("com.squareup.okhttp3:okhttp-tls:$okHttp3Version")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$junitVersion")
}

data class PluginDescriptor(
  val since: String, // earliest version string this is compatible with
  val until: String, // latest version string this is compatible with, can be wildcard like 202.*
  // https://github.com/JetBrains/gradle-intellij-plugin#intellij-platform-properties
  val sdkVersion: String, // the version string passed to the intellij sdk gradle plugin
  val sourceFolder: String, // used as the source root for specifics of this build
  val deps: List<String> // dependent plugins of this plugin
)

val plugins = listOf(
  PluginDescriptor(
    since = properties("pluginSinceBuild").get(),
    until = "232.*",
    sdkVersion = "IC-2023.1",
    sourceFolder = "IC-231",
    deps = listOf("java", "org.jetbrains.plugins.gradle", "org.jetbrains.kotlin")
  ),
  PluginDescriptor(
    since = "233.11799",
    until = properties("pluginUntilBuild").get(),
    sdkVersion = "IC-2023.3",
    sourceFolder = "IC-233",
    deps = listOf("java", "org.jetbrains.plugins.gradle", "org.jetbrains.kotlin")
  )
)
val productName = System.getenv("PRODUCT_NAME") ?: "IC-231"
val descriptor = plugins.first { it.sourceFolder == productName }

intellij {
  version.set(descriptor.sdkVersion)
  plugins.addAll(*descriptor.deps.toTypedArray())
  // !Development only!
  // downloadSources.set(true)
  // In Settings | Advanced Settings enable option Download sources in section Build Tools. Gradle.
  // Then invoke Reload All Gradle Projects action from the Gradle tool window.
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
      jvmTarget = JavaVersion.VERSION_17.toString()
      languageVersion = org.jetbrains.kotlin.config.LanguageVersion.LATEST_STABLE.versionString
    }
  }

  withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }

  patchPluginXml {
    version.set("${properties("pluginVersion").get()}-${descriptor.since.substringBefore(".")}")
    sinceBuild.set(descriptor.since)
    untilBuild.set(descriptor.until)

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

  withType<Test> {
    withType<ClasspathIndexCleanupTask> {
      dependsOn(compileTestKotlin)
    }
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
    archiveClassifier.set(descriptor.sdkVersion)
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
              .ifEmpty { "stable" }
          )
        }
        .map { it }
    )
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
  main {
    java {
      srcDir("src/${descriptor.sourceFolder}/kotlin")
    }
    resources {
      srcDir("src/${descriptor.sourceFolder}/resources")
    }
  }
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
