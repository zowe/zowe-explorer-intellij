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

plugins {
  id("org.jetbrains.intellij") version "1.5.3"
  kotlin("jvm") version "1.6.21"
  java
}

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")

group = "org.zowe"
version = "0.1.3"

repositories {
  mavenCentral()
  flatDir {
    dirs("libs")
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
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.20")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.20")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
  implementation("org.jgrapht:jgrapht-core:1.5.1")
  implementation("com.starxg:java-keytar:1.0.0")
  implementation("org.zowe:kotlinsdk:0.1.0")
  implementation("com.segment.analytics.java:analytics:+")
  testImplementation("junit", "junit", "4.12")
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
    sinceBuild.set("203.5981")
    untilBuild.set("221.*")
    changeNotes.set(
      """
      <b>Bugs fixed:</b>
      <ul>
        <li>https://github.com/zowe/zowe-explorer-intellij/issues/38</li>
      </ul>"""
    )
  }
}
