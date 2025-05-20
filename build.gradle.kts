import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	java
	`maven-publish`

	alias(libs.plugins.kotlin)
	alias(libs.plugins.quilt.loom)
}

val modVersion: String by project
val mavenGroup: String by project
val modId: String by project

val version = "${modVersion}+${libs.minecraft.get().version}"
base.archivesName = mavenGroup
val javaVersion = 21

val dataGenerationDir = file("src/main/generated")

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.
}

// Adds generated resources to main.
sourceSets.main {
	resources {
		srcDirs(dataGenerationDir)
	}
}

loom {
	// Loom and Loader both use this block in order to gather more information about your mod.
	mods {
		create(modId) {
			// Tell Loom about each source set used by your mod here. This ensures that your mod's classes are properly transformed by Loader.
			sourceSets.main.get()
			// If you shade (directly include classes, not JiJ) a dependency into your mod, include it here using one of these methods:
			// dependency("com.example.shadowed mod:1.2.3")
			// configuration("exampleShadedConfigurationName")
		}
	}

	runs {
		// Creates a custom run, that runs Fabric's Data Generation
		// On IntelliJ, it should also be added to run configurations.
		// Derived from https://github.com/Up-Mods/Sparkweave/blob/402ce5dfcac03a18aa8296e3fefcdb94df65e957/Fabric/build.gradle#L126-L138
		create("dataGeneration") {
			val buildDir = file("build/datagen")

			configName = "Fabric Data Generation"
			dataGenerationDir.mkdirs()
			buildDir.mkdirs()

			client()
			source(sourceSets.main.get())
			property("fabric-api.datagen")
			property("fabric-api.datagen.strict-validation", "true")
			property("fabric-api.datagen.output-dir", dataGenerationDir.absolutePath)

			runDir("build/datagen")
		}
	}
}

// All the dependencies are declared at gradle/libs.version.toml and referenced with "libs.<id>"
// See https://docs.gradle.org/current/userguide/platforms.html for information on how version catalogs work.
dependencies {
	minecraft(libs.minecraft)
	mappings(
		variantOf(libs.quilt.mappings) {
			classifier("intermediary-v2")
		})

	// Replace the above lines with the block below if you want to use Mojang mappings as your primary mappings, falling back on QM for parameters and Javadocs
	/*
	mappings(
		loom.layered {
			mappings(variantOf(libs.quilt.mappings) { classifier("intermediary-v2") })
			officialMojangMappings()
		}
	)
	*/

	modImplementation(libs.quilt.loader)

	// QSL is not a complete API; You will need Quilted Fabric API to fill in the gaps.
	// Quilted Fabric API will automatically pull in the correct QSL version.
	modImplementation(libs.bundles.quilted.fabric.api)
	// modImplementation(libs.bundles.quilted.fabric.api.deprecated) // If you wish to use the deprecated Fabric API modules (doesn't work in 1.20.6)

	modImplementation(libs.quilt.kotlin.libraries)
}

tasks {
	withType<KotlinCompile> {
		compilerOptions {
			jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
			// languageVersion: A.B of the kotlin plugin version A.B.C
			languageVersion = KotlinVersion.fromVersion(
				libs.plugins.kotlin.get().version.requiredVersion.substringBeforeLast('.')
			)
		}
	}

	withType<JavaCompile>().configureEach {
		options.encoding = "UTF-8"
		options.isDeprecation = true
		options.release.set(javaVersion)
	}

	// Some stuff in here are inspired from
	// https://github.com/VulpixelMC/quilt-kotlin-template-mod/blob/5c188b0f43f91e9265148b0904d150d46d63dd29/build.gradle.kts#L125C1-L135C2
	processResources {
		filteringCharset = "UTF-8"
		inputs.property("version", version)

		// Replaces the strings `${version}`, `${id}`, and `${group}` in
		// mixins and in the mod's config file.
		filesMatching(mutableListOf("quilt.mod.json")) {
			expand(
				"version" to version,
				"id" to modId,
				"group" to mavenGroup,
			)
		}

		// Replaces the string `${id}` in lang files.
		filesMatching("**/lang/*.json") {
			expand("id" to modId)
		}
	}

	javadoc {
		options.encoding = "UTF-8"
	}

	// Run `./gradlew wrapper --gradle-version <newVersion>` or `gradle wrapper --gradle-version <newVersion>` to update gradle scripts
	// BIN distribution should be sufficient for the majority of mods
	wrapper {
		distributionType = Wrapper.DistributionType.BIN
	}

	jar {
		from("LICENSE") {
			rename { "${it}_$modId" }
		}
	}

	val generateDataGenerationDir = "generateDataGenerationDir"
	register(generateDataGenerationDir) {
		inputs.dir(dataGenerationDir)

		doFirst {
			for (file in inputs.files) file.mkdirs()
		}
	}

	ideaSyncTask {
		finalizedBy(get(generateDataGenerationDir))
	}

	"runDataGeneration" {
		dependsOn(generateDataGenerationDir)
	}
}

val targetJavaVersion = JavaVersion.toVersion(javaVersion)
if (JavaVersion.current() < targetJavaVersion) {
	kotlin.jvmToolchain(javaVersion)

	java.toolchain {
		languageVersion.set(JavaLanguageVersion.of(javaVersion))
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	// If this mod is going to be a library, then it should also generate Javadocs in order to aid with development.
	// Uncomment this line to generate them.
	// withJavadocJar()

	// Still required by IDEs such as Eclipse and VSC
	sourceCompatibility = targetJavaVersion
	targetCompatibility = targetJavaVersion
}

// Configure the maven publication
publishing {
	publications {
		register<MavenPublication>("Maven") {
			from(components.getByName("java"))
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
