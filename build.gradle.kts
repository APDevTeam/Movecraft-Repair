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
    api("com.google.code.gson:gson:+")
    api("org.jetbrains:annotations-java5:24.1.0")
    api("com.sk89q.worldedit:worldedit-core:7.3.3")
    api("com.sk89q.worldedit:worldedit-bukkit:7.3.3")
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("net.countercraft:movecraft:+")
    compileOnly("com.github.MilkBowl:VaultAPI:+")
    compileOnly("it.unimi.dsi:fastutil:+")
    compileOnly("org.roaringbitmap:RoaringBitmap:+")
}

group = "net.countercraft.movecraft.repair"
version = "1.0.0_beta-7_dev-2"
description = "Movecraft-Repair"
java.toolchain.languageVersion = JavaLanguageVersion.of(21)

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
                platformVersions.set(listOf("1.20.6-1.21.1"))
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
