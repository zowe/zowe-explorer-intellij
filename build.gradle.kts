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
 */

import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    getUntil = { provider { "242.*" } },
    sdkVersion = "2023.3",
    sourceFolder = "IC-233"
  ),
  PluginDescriptor(
    jvmTargetVersion = JavaVersion.VERSION_21,
    since = "243.12818",
    getUntil = { provider { null } },
    sdkVersion = "243-EAP-SNAPSHOT",
    sourceFolder = "IC-243"
  )
)
val productName = System.getenv("PRODUCT_NAME") ?: "IC-231"
val descriptor = plugins.first { it.sourceFolder == productName }

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()
val remoteRobotVersion = "0.11.23"
val okHttp3Version = "4.12.0"
val kotestVersion = "5.9.1"
val retrofit2Vertion = "2.11.0"
val junitVersion = "5.11.1"
val mockkVersion = "1.13.12"
val ibmMqVersion = "9.4.0.0"
val jGraphTVersion = "1.5.2"
val zoweKotlinSdkVersion = "0.5.0"
val javaKeytarVersion = "1.0.0"

plugins {
  id("org.sonarqube") version "5.1.0.4882"
  id("org.jetbrains.intellij.platform") version "2.1.0"
  id("org.jetbrains.changelog") version "2.2.1"
  kotlin("jvm") version "1.9.22"
  java
  id("org.jetbrains.kotlinx.kover") version "0.8.3"
  id("org.owasp.dependencycheck") version "10.0.4"
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

dependencies {
  intellijPlatform {
//    intellijIdeaCommunity(descriptor.sdkVersion)
//    TO TEST EAP:
    intellijIdeaCommunity(descriptor.sdkVersion, useInstaller = false)
    jetbrainsRuntime()
    instrumentationTools()
    pluginVerifier()
    testFramework(TestFrameworkType.Plugin.Java)
    zipSigner()
  }
  implementation(group = "com.squareup.retrofit2", name = "retrofit", version = retrofit2Vertion)
  implementation("com.squareup.retrofit2:converter-gson:$retrofit2Vertion")
  implementation("com.squareup.retrofit2:converter-scalars:$retrofit2Vertion")
  implementation("com.squareup.okhttp3:okhttp:$okHttp3Version")
  implementation("org.jgrapht:jgrapht-core:$jGraphTVersion")
  implementation("com.starxg:java-keytar:$javaKeytarVersion")
  implementation("org.zowe.sdk:zowe-kotlin-sdk:$zoweKotlinSdkVersion")
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
  testImplementation("com.squareup.okhttp3:logging-interceptor:$okHttp3Version")
  testImplementation("com.intellij.remoterobot:ide-launcher:$remoteRobotVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$junitVersion")
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
      }
    }
  }
}

dependencyCheck {
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

//  withType<ClasspathIndexCleanupTask> {
//    onlyIf {
//      gradle.startParameter.taskNames.contains("test")
//    }
//    dependsOn(compileTestKotlin)
//  }

//
//  withType<ClasspathIndexCleanupTask> {
//    onlyIf {
//      gradle.startParameter.taskNames.contains("uiTest")
//    }
//    dependsOn("compileUiTestKotlin")
//  }

  test {
    useJUnitPlatform()

    // To run unit tests only and do not trigger "uiTest" task related things (like "compileUiTestKotlin")
    onlyIf { !gradle.startParameter.taskNames.contains("uiTest") }

    jvmArgs("--add-opens", "java.desktop/java.awt=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.desktop/java.awt.event=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.nio.file=ALL-UNNAMED")

    testLogging {
      events("passed", "skipped", "failed")
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

// TODO: fix
//  downloadRobotServerPlugin {
//    version.set(remoteRobotVersion)
//  }

// TODO: fix
//  runIdeForUiTests {
//    systemProperty("idea.trust.all.projects", "true")
//    systemProperty("ide.show.tips.on.startup.default.value", "false")
//  }
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
  systemProperty("ideLaunchFolder", System.getProperty("ideLaunchFolder"))
  systemProperty("forMainframePath", System.getProperty("forMainframePath"))
  systemProperty("remoteRobotUrl", System.getProperty("remoteRobotUrl"))
  systemProperty("ideaVersionForTest", System.getProperty("ideaVersionForTest"))
  systemProperty("ideaBuildVersionForTest", System.getProperty("ideaBuildVersionForTest"))
  systemProperty("robotServerForTest", System.getProperty("robotServerForTest"))
  testClassesDirs = sourceSets["uiTest"].output.classesDirs
  classpath = sourceSets["uiTest"].runtimeClasspath
  useJUnitPlatform {
    excludeTags("FirstTime")
    excludeTags("SmokeTest")
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
  useJUnitPlatform {
    includeTags("FirstTime")
  }
  testLogging {
    events("passed", "skipped", "failed")
  }
}

/**
 * Runs the smoke ui test
 */
val smokeUiTest = task<Test>("smokeUiTest") {
  description = "Gets rid of license agreement, etc."
  group = "verification"
  testClassesDirs = sourceSets["uiTest"].output.classesDirs
  classpath = sourceSets["uiTest"].runtimeClasspath
  useJUnitPlatform {
    includeTags("SmokeTest")
  }
  testLogging {
    events("passed", "skipped", "failed")
  }
}
