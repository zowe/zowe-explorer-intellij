import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.intellij") version "0.6.5"
  kotlin("jvm") version "1.4.32"
  java
}

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")

group = "eu.ibagroup"
version = "0.4.5"

repositories {
  mavenCentral()
  maven {
    url = uri("http://10.221.23.186:8082/repository/internal/")
    isAllowInsecureProtocol = true
    credentials {
      username = "admin"
      password = "password123"
    }
    metadataSources {
      mavenPom()
      artifact()
      ignoreGradleMetadataRedirection()
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(KotlinCompile::class).all {
  kotlinOptions {
    jvmTarget = "1.8"
    languageVersion = "1.4"
  }
}

dependencies {
  implementation(group = "com.squareup.retrofit2", name = "retrofit", version = "2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.5.0")
  implementation("com.squareup.retrofit2:converter-scalars:2.1.0")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.30")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.30")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
  implementation("org.jgrapht:jgrapht-core:1.5.0")
  implementation("eu.ibagroup:r2z:1.0.19")
  implementation("com.segment.analytics.java:analytics:+")
  testImplementation("junit", "junit", "4.12")
}

intellij {
  version = "2021.2"
}

tasks.getByName<PatchPluginXmlTask>("patchPluginXml") {
  sinceBuild("203.5981")
  untilBuild("212.*")
  changeNotes(
    """
      <h1>Version 0.4.5</h1><br/>
      <h2>Stories resolved</h2><br/>
      <ul>
        <li>IJMP-87 Spike: Drag and drop operations</li>
      </ul>
      <h2>Bugs fixed</h2>
      <ul>
        <li>IJMP-301 Renaming USS directory or file to existing one duplicates name in tree and shows inconsistent information</li>
        <li>IJMP-312 Incorrect warning during dataset creation</li>
      </ul>"""
  )
}
