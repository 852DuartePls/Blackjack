plugins {
    `java-library`
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "me.duart"
version = "0.0.1"
description = "Simple and easy to use Blackjack plugin"
var authors = listOf("DaveDuart")
extra["authors"] = authors


repositories {
    mavenCentral()
    maven ("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val properties = mapOf(
            "version" to project.version,
            "name" to project.name,
            "apiVersion" to "1.21",
            "description" to project.description,
            "authors" to project.extra["authors"]

        )
        inputs.properties(properties)
        filesMatching("paper-plugin.yml") {
            expand(properties)
        }
    }
    runServer {
        minecraftVersion("1.21.8")
        serverJar(file("/run/purpur.jar"))
        downloadPlugins {
            github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
            url("https://download.luckperms.net/1595/bukkit/loader/LuckPerms-Bukkit-5.5.10.jar")
            url("https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.21.2-dev+33-ff7c952.jar")
            modrinth("lpc-chat", "3.6.9")
        }
    }
}