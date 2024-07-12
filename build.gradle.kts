import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.sideloaded.elusion"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
  //  implementation("io.javalin:javalin:6.1.3")
    implementation("org.mongodb:mongodb-driver-sync:5.1.1")
  //  implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("com.google.code.gson:gson:2.11.0")
  //  implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("org.slf4j:slf4j-api:2.0.13")
  //  implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("net.dv8tion:JDA:5.0.0")
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("elusion-bot")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    manifest {
        attributes(mapOf("Main-Class" to "dev.sideloaded.elusion.Main"))
    }
}