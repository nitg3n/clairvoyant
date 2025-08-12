import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

plugins {
    kotlin("jvm") version "2.2.20-Beta2"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.nitg3n"
version = "v1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}



dependencies {

    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.exposed:exposed-core:0.52.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.52.0")
    compileOnly("net.kyori:adventure-api:4.17.0")

}



java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}



tasks {
    runServer {
        minecraftVersion("1.21")
    }



    shadowJar {
        archiveClassifier.set("")
        relocate("org.xerial.sqlite", "com.nitg3n.clairvoyant.libs.sqlite")
        relocate("com.zaxxer.hikari", "com.nitg3n.clairvoyant.libs.hikaricp")
        relocate("org.jetbrains.exposed", "com.nitg3n.clairvoyant.libs.exposed")
        transform(ServiceFileTransformer::class.java)
    }



    build {
        dependsOn(shadowJar)
    }



    processResources {
        filesMatching("plugin.yml") {
            expand(project.properties)
        }
    }
}