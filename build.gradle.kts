plugins {
  id("org.jetbrains.intellij") version "0.6.5"
  kotlin("jvm") version "1.4.21"
  java
}

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.intellij")

group = "eu.ibagroup"
version = "0.0"

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = org.gradle.api.JavaVersion.VERSION_1_8
  targetCompatibility = org.gradle.api.JavaVersion.VERSION_1_8
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
  kotlinOptions {
    jvmTarget = "1.8"
    languageVersion = "1.4"
  }
}


dependencies {
  implementation(group = "com.squareup.retrofit2", name = "retrofit", version = "2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.3.0")
  implementation("com.squareup.retrofit2:converter-scalars:2.1.0")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.21")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.21")
  testImplementation("junit", "junit", "4.12")
}

intellij {
  version = "2020.2"
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
  changeNotes(
    """
      Add change notes here.<br>
      <em>most HTML tags may be used</em>"""
  )
}