import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    
    kotlin("jvm") version "1.3.61"
    id("com.github.johnrengelman.shadow") version "4.0.4"
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
    implementation("com.google.zxing:javase:3.4.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev614-1.18.0-rc")
    implementation("com.google.api-client:google-api-client:1.30.6")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.30.6")
    implementation("com.google.cloud:google-cloud-texttospeech:0.98.0-beta")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("wordlink-application")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "io.ktor.server.netty.EngineMain"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}