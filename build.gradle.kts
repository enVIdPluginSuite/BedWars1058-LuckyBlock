import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "9.3.0"
}

group = "dev.etran"
version = "3.1.4"

repositories {
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    // Compiled against the OLDEST supported API (Spigot 1.8.8) so that nothing newer can be referenced by
    // accident. Anything that only exists on newer versions is reached through dev.etran.luckyblock.compat.
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")

    // BedWars1058 API
    // (isTransitive = false: its POM drags in an old full Spigot server jar and sidebar libs we don't need)
    compileOnly("com.andrei1058.bedwars:bedwars-api:23.12.1-SNAPSHOT") {
        isTransitive = false
    }

    // Kotlin's standard library is put inside the plugin jar (and renamed, see shadowJar), because servers
    // from 1.8 on do not provide it.
    implementation(kotlin("stdlib"))
}

// Minecraft 1.8 servers run on Java 8, so target Java 8 bytecode (it also runs on the Java 17/21 that
// newer servers use).
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// plugin.yml gets the project version
tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("BedWars1058-LuckyBlock-${project.version}-plain.jar")
}

// The plugin jar: our classes plus Kotlin's standard library moved to dev.etran.luckyblock.libs, so it can
// never clash with another plugin that ships its own Kotlin.
tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    archiveFileName.set("BedWars1058-LuckyBlock-${project.version}.jar")
    relocate("kotlin", "dev.etran.luckyblock.libs.kotlin")
    relocate("org.jetbrains", "dev.etran.luckyblock.libs.jetbrains")
    relocate("org.intellij", "dev.etran.luckyblock.libs.intellij")
    exclude("META-INF/*.kotlin_module", "META-INF/maven/**", "META-INF/versions/**", "DebugProbesKt.bin")
    minimize()
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}
