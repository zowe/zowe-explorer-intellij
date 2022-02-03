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
version = "0.5.0"

repositories {
  mavenCentral()
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
  implementation("eu.ibagroup:r2z:1.0.4")
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
      <b>New features:</b><br/>
      <ul>
        <li>JES Explorer - provides the option to submit JCL jobs, view their statuses and operate an input and output of it using the plugin.</li>
        <li>Copy operations - to copy dataset and files both from USS to z/OS and from z/OS to USS using Drag & Drop operation.</li>
      </ul>
      <b>Expanded changes list:</b><br/>
      <ul>
        <li>Rework navigate method in ExplorerTreeNode class for Jes support</li>
        <li>Sequential and Member to PDS + Uss Folder</li>
        <li>Add Get...PropertiesAction for JesExplorer support</li>
        <li>Changed functionality of "+" button in JESExplorer</li>
        <li>Create JES tab in Settings</li>
        <li>SpoolFileContentSynchronizer</li>
        <li>Explorer refactoring</li>
        <li>JobFetchProvider SpoolFileFetchProvider</li>
        <li>Copy PDS to Uss Folder</li>
        <li>Copy Uss File and PDS</li>
        <li>Spike: Drag and drop operations</li>
        <li>Functionality of renaming files and folders as on the Mainframe</li>
        <li>Create Icon for toolbar</li>
        <li>Consider lrecl constraints in datasets when editing</li>
      </ul>
      <b>Bugs fixed:</b>
      <ul>
        <li>Renaming USS directory or file to existing one duplicates name in tree and shows inconsistent information</li>
        <li>Place the cursor in the Member name field</li>
        <li>The same dataset does not open in the second mask</li>
        <li>Duplicated JesFilters removed</li>
        <li>Empty data set hangs in loading state</li>
        <li>No message details when renaming dataset to existing name</li>
      </ul>"""
  )
}
