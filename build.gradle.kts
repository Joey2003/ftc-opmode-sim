// Based on github.com/processing/processing-library-template, trimmed.
// ./gradlew build  -> build/libs/ftc-opmode-sim.jar, then installs into the Processing sketchbook.

import java.io.File
import java.util.Properties
import org.gradle.internal.os.OperatingSystem

plugins {
    id("java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

val libraryProperties = Properties().apply {
    load(rootProject.file("release.properties").inputStream())
}

val libName = "ftc-opmode-sim"
// Processing library folder names must be ASCII letters/digits only (no hyphens).
// Installed folder and jar use this name.
val installName = "ftcopmodesim"
group = "org.nycfirst"
version = libraryProperties.getProperty("prettyVersion")

// Processing core from the installed app (provided by Processing at runtime).
// Auto-detects core-*.jar inside /Applications/Processing.app (any version).
// Override with -PprocessingCore=/path/to/core.jar
val processingCore: String = (findProperty("processingCore") as String?)?.also {
    if (!File(it).isFile) throw GradleException("processingCore not found: $it")
} ?: run {
    val os = OperatingSystem.current()
    val app = when {
        os.isMacOsX -> File("/Applications/Processing.app")
        os.isWindows -> File("C:/Program Files/Processing")
        else -> File("/usr/share/processing")
    }
    app.walkTopDown().maxDepth(10).onEnter { it.name != "jdk" }
        .filter { it.isFile && it.name.matches(Regex("core-.*\\.jar")) && it.parentFile.name == "library" }
        .maxByOrNull { it.name }?.absolutePath
        ?: throw GradleException(
            "Processing core jar not found in ${app.absolutePath}.\n" +
            "Install Processing 4 there, or pass the path: ./gradlew build -PprocessingCore=/path/to/core.jar")
}

// Sketchbook location (same logic as the template). Override with -PsketchbookLocation=...
val userHome = System.getProperty("user.home")
val os = OperatingSystem.current()
val processingPreferencesFile = when {
    os.isWindows -> File(System.getenv("APPDATA") ?: "$userHome/AppData/Roaming", "Processing/preferences.txt")
    else -> null
}
val configuredSketchbook = processingPreferencesFile?.takeIf { it.isFile }?.let { preferencesFile ->
    Properties().apply { load(preferencesFile.inputStream()) }.getProperty("sketchbook.path.four")
}
val sketchbookLocation: String = (findProperty("sketchbookLocation") as String?) ?: configuredSketchbook ?: run {
    when {
        os.isMacOsX -> if (File("$userHome/Documents/Processing/sketchbook").isDirectory)
            "$userHome/Documents/Processing/sketchbook" else "$userHome/Documents/Processing"
        os.isWindows -> "$userHome/Documents/Processing"
        else -> "$userHome/sketchbook"
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files(processingCore))
}

tasks.jar {
    archiveBaseName.set(libName)
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.register<WriteProperties>("writeLibraryProperties") {
    group = "processing"
    destinationFile = layout.buildDirectory.file("library.properties")
    listOf("name", "version", "authors", "url", "categories", "sentence",
           "paragraph", "minRevision", "maxRevision").forEach {
        property(it, libraryProperties.getProperty(it))
    }
    property("prettyVersion", project.version)
}

// Installs to <sketchbook>/libraries/ftcopmodesim/{library.properties, library/ftcopmodesim.jar}
tasks.register<Sync>("installLibrary") {
    group = "processing"
    dependsOn("jar", "writeLibraryProperties")
    into("$sketchbookLocation/libraries/$installName")
    from(layout.buildDirectory.file("library.properties"))
    from(tasks.jar) { into("library"); rename { "$installName.jar" } }
    from("examples") { into("examples") }
    doLast { println("Installed $installName to $sketchbookLocation/libraries/$installName") }
}

tasks.build { finalizedBy("installLibrary") }
