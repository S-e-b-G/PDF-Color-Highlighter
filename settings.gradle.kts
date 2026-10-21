import org.gradle.api.initialization.resolve.RepositoriesMode
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "Artifex"
            url = uri("https://maven.ghostscript.com")
            content { includeGroup("com.artifex.mupdf") }
        }
    }
}
rootProject.name = "PdfColorHighlighter"
include(":app")
