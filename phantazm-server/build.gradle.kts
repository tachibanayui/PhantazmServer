import java.security.MessageDigest

plugins {
    id("phantazm.java-conventions")
}

repositories {
    maven("https://jitpack.io")
    maven("https://dl.cloudsmith.io/public/steank-f1g/ethylene/maven/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation(project(":phantazm-api"))
    implementation(project(":phantazm-zombies"))

    implementation(libs.miniMessage)
    implementation(libs.ethylene.toml)
}

tasks.jar {
    dependsOn("copyLibs")

    outputs.upToDateWhen {
        !tasks.getByName("copyLibs").didWork
    }

    archiveBaseName.set("server")
    archiveVersion.set("")
    archiveClassifier.set("")

    doFirst {
        manifest {
            val copyLibsTask = tasks.getByName("copyLibs")
            val libsFolder = copyLibsTask.extensions["libsFolder"] as File

            @Suppress("UNCHECKED_CAST")
            val outputFiles = copyLibsTask.extensions["outputFiles"] as List<File>

            attributes(
                "Class-Path" to outputFiles.joinToString(" ") {
                    "libs/${it.relativeTo(libsFolder).path.replace('\\', '/')}"
                },
                "Main-Class" to "com.github.phantazmnetwork.server.Main",
                "Multi-Release" to true
            )
        }
    }
}

tasks.register<Copy>("setupServer") {
    dependsOn(tasks.jar)

    outputs.upToDateWhen {
        !tasks.jar.get().didWork
    }

    from(tasks.jar)
    into("$rootDir/run/server-1/")
}