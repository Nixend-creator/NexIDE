rootProject.name = "NexIDE"

pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

include(
  ":platform:util",
  ":platform:core",
  ":platform:psi",
  ":platform:plugin",
  ":platform:editor",
  ":platform:ui",
  ":ide-app"
)
