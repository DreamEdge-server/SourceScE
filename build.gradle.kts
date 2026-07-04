plugins {
    java
}

group = "com.source.sourcesce"
version = "1.0.10"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven("https://repo.codemc.io/repository/maven-public/")  // PacketEvents
}

dependencies {
    // Server API — provides Player, Adventure, title API, events. Server is 26.1.2 (Paper's 1.21.11 line).
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // PacketEvents API — wrapper classes to send the spectator game-state packet that hides the vanilla
    // HUD. The spigot runtime plugin (2.13.0) provides the implementation at runtime.
    compileOnly("com.github.retrooper:packetevents-api:2.13.0")
    // CraftEngine (overlay image) and BetterHud (hide its HUDs) are called by reflection — no compile deps.
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks.jar {
    archiveFileName.set("SourceScE.jar")
}
