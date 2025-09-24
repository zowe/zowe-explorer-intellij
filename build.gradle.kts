/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

//!IMPORTANT!: to refer "libs", use ./gradle/libs.versions.toml

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
fun dateValue(pattern: String): String =
  LocalDate.now(ZoneId.of("Europe/Warsaw")).format(DateTimeFormatter.ofPattern(pattern))

// https://github.com/kotest/kotest-intellij-plugin/blob/master/build.gradle.kts
data class PluginDescriptor(
  val jvmTargetVersion: JavaVersion, // the Java version to use during the plugin build
  val since: String, // earliest version string this is compatible with
  val getUntil: () -> Provider<String>, // latest version string this is compatible with, can be wildcard like 202.*
  // https://github.com/JetBrains/gradle-intellij-plugin#intellij-platform-properties
  val sdkVersion: String, // the version string passed to the intellij sdk gradle plugin
  val sourceFolder: String // used as the source root for specifics of this build
)

val plugins = listOf(
  PluginDescriptor(
    jvmTargetVersion = JavaVersion.VERSION_17,
    since = properties("pluginSinceBuild").get(),
    getUntil = { provider { "232.*" } },
    sdkVersion = "2023.1.7",
    sourceFolder = "IC-231"
  ),
  PluginDescriptor(
    jvmTargetVersion = JavaVersion.VERSION_17,
    since = "233.11799",
    getUntil = { provider { "241.*" } },
    sdkVersion = "2023.3",
    sourceFolder = "IC-233"
  ),
  PluginDescriptor(
    jvmTargetVersion = JavaVersion.VERSION_21,
    since = "242.20224",
    getUntil = { provider { "242.*" } },
    sdkVersion = "2024.2",
    sourceFolder = "IC-242"
  ),
  PluginDescriptor(
    jvmTargetVersion = JavaVersion.VERSION_21,
    since = "243.12818",
    getUntil = { provider { null } },
    sdkVersion = "2024.3",
    sourceFolder = "IC-243"
  )
)
val productName = System.getenv("PRODUCT_NAME") ?: "IC-231"
val descriptor = plugins.first { it.sourceFolder == productName }

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

plugins {
  alias(libs.plugins.gradle) // IntelliJ Platform Gradle Plugin
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.sonarqube)
  alias(libs.plugins.changelog)
  alias(libs.plugins.kover)
  alias(libs.plugins.dependencycheck)
  java
}

repositories {
  mavenCentral()
  flatDir {
    dirs("libs")
  }
  maven {
    url = uri("https://zowe.jfrog.io/zowe/libs-release")
    flatDir {
      dir("libs")
    }
  }
  intellijPlatform {
    defaultRepositories()
    jetbrainsRuntime()
  }
}

java {
  sourceCompatibility = descriptor.jvmTargetVersion
  targetCompatibility = descriptor.jvmTargetVersion
}

kotlin {
  compilerOptions {
    jvmToolchain(JavaLanguageVersion.of(descriptor.jvmTargetVersion.toString()).asInt())
  }
}

/** Source sets configuration */
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
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.test.get().compileClasspath
    runtimeClasspath += output + compileClasspath
  }
}

configurations["uiTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())
configurations["uiTestImplementation"].extendsFrom(configurations.testImplementation.get())

dependencies {
  intellijPlatform {
//    intellijIdeaCommunity(descriptor.sdkVersion)
//    TO TEST EAP:
    intellijIdeaCommunity(descriptor.sdkVersion, useInstaller = false)
    jetbrainsRuntime()
    pluginVerifier()
    testFramework(TestFrameworkType.Plugin.Java)
    zipSigner()
    testFramework(TestFrameworkType.Starter, configurationName = "uiTestImplementation")
    zipSigner()
  }
  implementation(libs.retrofit2)
  implementation(libs.retrofit2.converter.gson)
  implementation(libs.retrofit2.converter.scalars)
  implementation(libs.okhttp3)
  implementation(libs.jgrapht.core)
  implementation(libs.java.keytar)
  implementation(libs.zowe.kotlin.sdk)
  testImplementation(libs.mockk)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.runner.junit5)
}

intellijPlatform {
  pluginConfiguration {
    version = "${properties("pluginVersion").get()}-${descriptor.since.substringBefore(".")}"
    ideaVersion {
      sinceBuild = descriptor.since
      untilBuild = descriptor.getUntil()
    }
  }
  pluginVerification {
    ides {
      recommended()
    }
  }
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

kover {
  currentProject {
    instrumentation {
      /* exclude Gradle test tasks */
      disabledForTestTasks.addAll("uiTest", "firstTimeUiTest", "smokeUiTest")
    }
  }
  reports {
    filters {
      includes {
        classes(providers.provider { "org.zowe.explorer.*" })
      }
      excludes {
        classes(providers.provider { "org.zowe.explorer.vfs.MFVFileCreateEvent" })
        classes(providers.provider { "org.zowe.explorer.vfs.MFVFilePropertyChangeEvent" })
        classes(providers.provider { "org.zowe.explorer.utils.KoverIgnore" })
        annotatedBy("org.zowe.explorer.utils.KoverIgnore")
      }
    }
  }
}

dependencyCheck {
  analyzers.apply {
    // Analyze only first-level dependencies, not internal JARs
    archiveEnabled = false
  }
  formats = listOf(
    "HTML",
    "SARIF",
    "JSON",
    "XML"
  )
  suppressionFiles = listOf("$projectDir/owasp-dependency-check-suppression.xml")
}

tasks {
  wrapper {
    gradleVersion = properties("gradleVersion").get()
  }

  withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }

  patchPluginXml {
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

  withType<KotlinCompile>().configureEach {
    if (name == "compileUiTestKotlin") {
      onlyIf {
        gradle.startParameter.taskNames.any { it.contains("uiTests") }
      }
    }
  }

  test {
    useJUnitPlatform()

    testLogging {
      events("passed", "skipped", "failed")
      // showStandardStreams = true
    }

    //  ignoreFailures = true

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

  koverHtmlReport {
    finalizedBy("koverXmlReport")
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
    archiveClassifier.set(descriptor.sdkVersion)
    from(createOpenApiSourceJar) { into("lib/src") }
  }

  signPlugin {
    certificateChain.set(environment("INTELLIJ_SIGNING_CERTIFICATE_CHAIN").map { it })
    privateKey.set(environment("INTELLIJ_SIGNING_PRIVATE_KEY").map { it })
    password.set(environment("INTELLIJ_SIGNING_PRIVATE_KEY_PASSWORD").map { it })
  }

  publishPlugin {
    token.set(environment("ZOWE_INTELLIJ_MARKET_TOKEN").map { it })
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
}

/**
 * UI tests task. Describes setup that provides the tools to run UI tests.
 * This task is available only for IDEs since version 242
 */
// https://github.com/JetBrains/intellij-community/blob/master/platform/remote-driver/README.md
val uiTests by intellijPlatformTesting.testIdeUi.registering {
  task {
    timeout.set(Duration.ofMinutes(60))
    enabled = gradle.startParameter.taskNames.any { it.contains("uiTests") } && descriptor.since >= "242"
    archiveFile.set(tasks.buildPlugin.flatMap { it.archiveFile })
    testClassesDirs = sourceSets["uiTest"].output.classesDirs
    classpath = sourceSets["uiTest"].runtimeClasspath

    useJUnitPlatform {
      isScanForTestClasses = false
      includeTags("New")
    }

    jvmArgumentProviders += CommandLineArgumentProvider {
      listOf(
        "-Xmx2g",
        "-Xms512m",
        "-Dide.test.version=${descriptor.sdkVersion}",
        "-Dplugin.path=${tasks.buildPlugin.flatMap { it.archiveFile }.get().asFile.absolutePath}",
        "-Dui.test.mock.project.path=src/uiTest/resources/mock_project",
        "-Didea.trust.all.projects=true",
        "-Dide.show.tips.on.startup.default.value=false",
        "-Didea.log.config.properties.file=src/uiTest/resources/log.properties",
        "-Didea.log.path=src/uiTest/resources/mock_project",
        "-Dkotest.framework.classpath.scanning.autoscan.disable=true",
      )
    }

    testLogging {
      events("passed", "skipped", "failed", "standardOut", "standardError")
    }
  }

  plugins {
    robotServerPlugin()
  }

  dependencies {
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.okhttp3.logging.interceptor)
    testImplementation(libs.okhttp3.mockwebserver)
    testImplementation(libs.okhttp3.okhttp.tls)
    // TODO: revise and delete old unnecessary deps
    testImplementation("com.intellij.remoterobot:ide-launcher:0.11.23")
    testImplementation("com.intellij.remoterobot:remote-robot:0.11.23")
    testImplementation("com.intellij.remoterobot:remote-fixtures:0.11.23")
  }
}
