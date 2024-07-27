plugins {
    `java-library`
    `maven-publish`
    id("io.github.0ffz.github-packages") version "1.2.1"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
}

repositories {
    gradlePluginPortal()
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { githubPackage("apdevteam/movecraft")(this) }
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    api("com.google.code.gson:gson:2.10")
    api("org.jetbrains:annotations-java5:24.1.0")
    api("com.sk89q.worldedit:worldedit-core:7.2.9")
    api("com.sk89q.worldedit:worldedit-bukkit:7.2.9")
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("net.countercraft:movecraft:+")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("it.unimi.dsi:fastutil:8.5.11")
    compileOnly("org.roaringbitmap:RoaringBitmap:1.0.6")
}

group = "net.countercraft.movecraft.repair"
version = "1.0.0_beta-5"
description = "Movecraft-Repair"
java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.jar {
    archiveBaseName.set("Movecraft-Repair")
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.processResources {
    from(rootProject.file("LICENSE.md"))
    filesMatching("*.yml") {
        expand(mapOf("projectVersion" to project.version))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.countercraft.movecraft.repair"
            artifactId = "movecraft-repair"
            version = "${project.version}"

            artifact(tasks.jar)
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/apdevteam/movecraft-repair")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set("Release")
        id.set("Airship-Pirates/Movecraft-Repair")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms {
            register(io.papermc.hangarpublishplugin.model.Platforms.PAPER) {
                jar.set(tasks.jar.flatMap { it.archiveFile })
                platformVersions.set(listOf("1.18.2-1.21"))
                dependencies {
                    hangar("Movecraft") {
                        required.set(true)
                    }
                    hangar("WorldEdit") {
                        required.set(true)
                    }
                    url("Vault", "https://github.com/milkbowl/Vault") {
                        required.set(false)
                    }
                }
            }
        }
    }
}
