import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.61"
    application
}

group = "com.aopro"
version = "0.1.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

repositories {
    jcenter()
}

dependencies {
    val kotlinVersion: String by extra
    val ktorVersion = "1.0.1"
    val MongoVersion = "3.11.2"

    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")

    implementation("org.mongodb:mongo-java-driver:$MongoVersion")
    implementation("org.litote.kmongo:kmongo:$MongoVersion")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("commons-codec:commons-codec:1.5")
    implementation("com.auth0:java-jwt:3.8.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}