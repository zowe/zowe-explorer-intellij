import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.intellij") version "0.6.5"
  kotlin("jvm") version "1.4.32"
  java
  jacoco
}

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")

group = "eu.ibagroup"
version = "0.6.0"

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
  implementation("eu.ibagroup:r2z:1.0.22")
  implementation("com.segment.analytics.java:analytics:+")
  testImplementation("io.mockk:mockk:1.10.2")
  testImplementation("org.mock-server:mockserver-netty:5.11.1")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}

intellij {
  version = "2022.1"
}

tasks.getByName<PatchPluginXmlTask>("patchPluginXml") {
  sinceBuild("203.5981")
  untilBuild("221.*")
  changeNotes(
    """
      <b>Expanded changes list:</b><br/>
      <ul>
        <li>Manual sync option</li>
        <li>Delete dataset/file when it is being edited</li>
        <li>Job Console log view</li>
        <li>Copy from remote to local</li>
        <li>Job operations</li>
        <li>Separate icons for Datasets</li>
        <li>Autosync reworked</li>
        <li>User configs parsing in new format</li>
        <li>Force rename</li>
        <li>Settings tab</li>
      </ul>
      <b>Bugs fixed:</b>
      <ul>
        <li>Renaming the mask in the file explorer to existing one does not return any message</li>
        <li>The creation of masks with three or more asterisks is not blocked</li>
        <li>Error when the prefix length for JobFilter is more than 8 characters or Prefix contains unacceptable symbols</li>
        <li>NPE when notification was closed</li>
        <li>Change lower case to upper case during DS mask creation</li>
        <li>Wrong Directory allocation restrictions</li>
        <li>Error 404 during dataset creation</li>
        <li>Block Size is being overwritten on error</li>
        <li>Missing UI error message for z/Os mask with length more than 44 chars</li>
        <li>Adding job filter through context menu item in JES Explorer doesn't work correctly</li>
        <li>Creating z/Os mask with first digit in HLQ is not blocked</li>
        <li>Right click->New->WorkingSet on JES Explorer page opens dialog to create WorkingSet instead of JobsWorkingSet</li>
        <li>Delete option does not work for Working Sets in File Explorer.</li>
        <li>Hotfix. Fix plugin compatibility with old versions of Inellij platform.</li>
        <li>ScrollPanel in "Allocate Dataset" frame</li>
        <li>Renaming an uss folder/file and then renaming it back to its original name causes an error</li>
        <li>No message details when renaming dataset to existing name</li>
        <li>Error message for failed dataset allocation should be duplicated in Dataset Allocation window</li>
        <li>Impossible to close job's output</li>
        <li>IDE Fatal errors if delete the connection that has working set</li>
        <li>Use binary mode changes file contents</li>
        <li>Bad connection was deleted</li>
        <li>NullPointerException when tree opens by user with incorrect password.</li>
        <li>Username/Password are not validated during connection creation </li>
        <li>Impossible to close the file located locally on PC (the file was opened not from plugin)</li>
      </ul>"""
  )

tasks.test {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed")
  }
  finalizedBy(tasks.jacocoTestReport)
}
}
