plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "com.source.sourcesce"
version = "1.0.11"

val paperApiVersion = providers.gradleProperty("paperApiVersion")
    .orElse("26.3.build.38-alpha")
    .get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    if (paperApiVersion.startsWith("26.")) {
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "paperApiMetadata"
            metadataSources {
                mavenPom()
                artifact()
                ignoreGradleMetadataRedirection()
            }
            content {
                includeModule("io.papermc.paper", "paper-api")
            }
        }
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        if (paperApiVersion.startsWith("26.")) {
            content {
                excludeModule("io.papermc.paper", "paper-api")
            }
        }
    }
    maven("https://repo.codemc.io/repository/maven-public/")  // PacketEvents
}

dependencies {
    // Server API — provides Player, Adventure, title API, events. Server is 26.1.2 (Paper's 1.21.11 line).
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    // PacketEvents API — wrapper classes to send the spectator game-state packet that hides the vanilla
    // HUD. The spigot runtime plugin (2.13.0) provides the implementation at runtime.
    compileOnly("com.github.retrooper:packetevents-api:2.13.0")
    // CraftEngine (overlay image) and BetterHud (hide its HUDs) are called by reflection — no compile deps.
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
    }
    jar {
        enabled = false
    }
    shadowJar {
        archiveFileName.set("SourceScE.jar")
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}

// ── 部署产物统一输出 ─────────────────────────────────────────────────────────
// 所有插件的 shade 包集中输出到 <IdeaProjects>/Source-dist，方便一次性上传到服务器。
// 需要换目录：./gradlew shadowJar -PsourceDist=D:/upload
val sourceDistDir = file(providers.gradleProperty("sourceDist").orNull ?: "${rootDir}/../Source-dist")
tasks.matching { it.name == "shadowJar" }.configureEach {
    if (this is org.gradle.api.tasks.bundling.AbstractArchiveTask) {
        destinationDirectory.set(sourceDistDir)
    }
}
