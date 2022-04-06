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
version = "0.5.2"

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
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType(KotlinCompile::class).all {
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_11.toString()
    languageVersion = org.jetbrains.kotlin.config.LanguageVersion.LATEST_STABLE.versionString
  }
}

tasks.buildSearchableOptions {
  enabled = false
}

dependencies {
  implementation(group = "com.squareup.retrofit2", name = "retrofit", version = "2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.5.0")
  implementation("com.squareup.retrofit2:converter-scalars:2.1.0")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.30")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.30")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
  implementation("org.jgrapht:jgrapht-core:1.5.0")
  implementation("eu.ibagroup:r2z:1.0.20")
  implementation("com.segment.analytics.java:analytics:+")
  testImplementation("junit", "junit", "4.12")
}

intellij {
  version = "2021.3"
}

tasks.getByName<PatchPluginXmlTask>("patchPluginXml") {
  sinceBuild("203.5981")
  untilBuild("213.*")
  changeNotes(
    """
      <b>Expanded changes list:</b><br/>
      <ul>
        <li>Manual sync option implemented</li>
        <li>Separate icons for Datasets and USS folders</li>
        <li>Autosave reworks</li>
        <li>Logs in jobs console log view</li>
        <li>Parse config xml file in old format</li>
      </ul>
      <b>Bugs fixed:</b>
      <ul>
        <li>IDE Fatal errors if delete the connection that has working set</li>
        <li>Delete option does not work for Working Sets in File Explorer</li>
        <li>Right click->New->WorkingSet on JES Explorer page opens dialog to create WorkingSet instead of JobsWorkingSet</li>
        <li>Creating z/Os mask with first digit in HLQ is not blocked</li>
        <li>The creation of masks with three or more asterisks is not blocked</li>
        <li>Renaming the mask in the file explorer to existing one does not return any message</li>
      </ul>"""
  )
}
