import dev.architectury.pack200.java.Pack200Adapter

plugins {
    id("java")
    id("gg.essential.loom") version "1.6.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

val projectName: String by project
val projectId: String by project
val projectVersion: String by project
val projectGroup: String by project
val mcVersion = property("minecraft.version")?.toString()
    ?: error("minecraft.version is not set")

val projectDescription = findProperty("projectDescription")?.toString().orEmpty()
val projectUrl = findProperty("projectUrl")?.toString().orEmpty()
val projectUpdateUrl = findProperty("projectUpdateUrl")?.toString().orEmpty()

group = projectGroup
version = projectVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

loom {
    silentMojangMappingsLicense()

    forge {
        pack200Provider.set(Pack200Adapter())
    }

    runs {
        named("client") {
            ideConfigGenerated(true)
        }
    }
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.minecraftforge.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    modImplementation("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
}

val manifestAttributes = mapOf(
    "FMLCorePlugin" to "modhider.CoreMod",
    "FMLCorePluginContainsFMLMod" to true,
    "ForceLoadAsMod" to true,
    "MixinConfigs" to "mixins.modhider.json",
    "ModSide" to "CLIENT",
    "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
    "TweakOrder" to "0",
)

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    processResources {
        val metadata = mapOf(
            "id" to projectId,
            "name" to projectName,
            "version" to projectVersion,
            "mcversion" to mcVersion,
            "description" to projectDescription,
            "url" to projectUrl,
            "updateUrl" to projectUpdateUrl,
        )

        inputs.properties(metadata)

        filesMatching("mcmod.info") {
            expand(metadata)
        }
    }

    jar {
        archiveBaseName.set(projectName)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes(manifestAttributes)
        }
        from(project.configurations.getByName("modImplementation").files.map { file ->
            if (file.isDirectory) file else zipTree(file)
        }) {
            exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        }
    }
}

val localInstallScript = file("install.local.gradle.kts")
if (localInstallScript.exists()) {
    apply(from = localInstallScript)
}
