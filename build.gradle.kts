import java.net.URI

plugins {
    kotlin("jvm") version "2.1.20"
    application
}

group = "org.bot"
version = "1.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("Mainkt")
}