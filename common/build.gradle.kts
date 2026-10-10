plugins {
    `java-library`
}

dependencies {
    api("org.jspecify:jspecify:1.0.0")
    api("org.joml:joml:1.10.8")
    api("com.fasterxml.jackson.core:jackson-databind:2.18.3")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// --- BuildInfo generation ---
// Writes BuildInfo.java with VERSION and FULL_VERSION = "v<VERSION>-<API_VERSION>.<SIM_VERSION>".
// VERSION comes from the newest release tag (v<major>.<minor>.<patch>) reachable from HEAD: "0.1.0" on the tagged
// commit, "0.1.0-3-gabc1234" three commits later. Without a reachable tag it is the root project's version + "-dev".
// The release workflow (.github/workflows/build.yml) tags releases, so a release build shows its tag in game.

val generatedBuildInfoDir = layout.buildDirectory.dir("generated/sources/buildinfo/java/main")

val generateBuildInfo by tasks.registering {
    val outputDir = generatedBuildInfoDir
    val baseVersion = project.version.toString()
    val gitDir = project.rootDir
    outputs.dir(outputDir)
    outputs.upToDateWhen { false }  // git history changes between commits; always recompute

    doLast {
        // Resurrected-style tags (v2.0.3-103.1) are excluded.
        val described = runCatching {
            val process = ProcessBuilder("git", "describe", "--tags", "--match", "v[0-9]*", "--exclude", "v*-*")
                .directory(gitDir)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0) output else ""
        }.getOrDefault("")

        val effectiveVersion = if (described.startsWith("v")) described.substring(1) else "$baseVersion-dev"
        val packageDir = outputDir.get().asFile.resolve("com/oddlabs/util")
        packageDir.mkdirs()
        packageDir.resolve("BuildInfo.java").writeText(
            """
            package com.oddlabs.util;

            public final class BuildInfo {
                public static final String VERSION = "$effectiveVersion";
                public static final String FULL_VERSION = "v" + VERSION + "-" + Compatibility.API_VERSION + "." + Compatibility.SIM_VERSION;
                private BuildInfo() {}
            }

            """.trimIndent()
        )
    }
}

sourceSets.main {
    java.srcDir(generateBuildInfo)
}