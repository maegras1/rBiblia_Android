buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.1.1")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.10")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.2.10-2.0.2")
        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
    }
}

plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}
