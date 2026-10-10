import com.smushytaco.lwjgl_gradle.Module

plugins {
    id("com.smushytaco.lwjgl3")
}

lwjgl {
    // Strongly recommended: set LWJGL version explicitly
    version = "3.4.1"

    // Add LWJGL modules + the correct native artifacts
    implementation(
        Module.CORE,
        Module.OPENGL,
        Module.STB
    )
}

dependencies {
    implementation(project(":common"))
}

// The Blender add-on in blender/ as the zip Blender installs (Preferences > Add-ons > Install from Disk);
// docs/content-pipeline.md.
tasks.register<Zip>("blenderAddon") {
    group = "build"
    description = "Packs the Blender add-on (tools/blender) into an installable zip in tools/build/blender."
    from("blender/io_scene_tribaltrouble.py")
    archiveFileName.set("io_scene_tribaltrouble.zip")
    destinationDirectory.set(layout.buildDirectory.dir("blender"))
}
