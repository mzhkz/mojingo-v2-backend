import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
    application
}

group = "com.aopro"
version = "0.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlinx")
    maven("https://kotlin.bintray.com/ktor")
}

dependencies {
    val kotlinVersion: String by extra
    val ktorVersion = "1.2.6"

    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
//    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")

    implementation("org.mongodb:mongo-java-driver:3.9.1")
    implementation("org.litote.kmongo:kmongo:3.9.1")

    implementation("ch.qos.logback:logback-classic:1.2.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}