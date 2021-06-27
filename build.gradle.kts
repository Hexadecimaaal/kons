plugins {
  kotlin("jvm") version "1.5.20"
  application
}

group = "io.github.hexadecimaaal"
version = "0.1-SNAPSHOT"

repositories {
  jcenter()
  mavenCentral()
}

val arrowVersion = "0.13.2"

kotlin {
  sourceSets {
    val main by getting {
      dependencies {
        implementation("io.arrow-kt:arrow-core:$arrowVersion")
        implementation("io.ktor:ktor-server-netty:1.4.0")
        implementation("io.ktor:ktor-html-builder:1.4.0")
        implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
        implementation("com.beust:klaxon:5.5")
      }
    }
    val test by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        implementation(kotlin("test-junit"))
      }
    }
    all {
      languageSettings.enableLanguageFeature("InlineClasses")
    }
  }
}
