rootProject.name = "phantazm"

val localSettings = file("local.settings.gradle.kts")
if (localSettings.exists()) {
    //apply from local settings too, if it exists
    //can be used to sideload Minestom for faster testing
    apply(localSettings)
}

pluginManagement {
    repositories {
        //necessary for phantazm-zombies-mapeditor module which contains a Fabric mod
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }


        maven("https://dl.cloudsmith.io/public/steanky/element/maven/")
        mavenCentral()
        gradlePluginPortal()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val toSkip = gradle.startParameter.projectProperties.getOrDefault("skipBuild", "").split(",")

sequenceOf(
    "core",
    "commons",
    "messaging",
    "mob",
    "proxima-minestom",
    "server",
    "velocity",
    "zombies",
    "zombies-mapdata",
    "zombies-mapeditor"
).forEach {
    if (!toSkip.contains(it)) {
        include(":phantazm-$it")
        project(":phantazm-$it").projectDir = file(it)
    } else {
        println("Skipping project module $it")
    }
}
