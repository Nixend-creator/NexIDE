plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

dependencies {
  implementation(project(":platform:core"))
  implementation(project(":platform:psi"))
  implementation(project(":platform:plugin"))
  implementation(project(":platform:editor"))
  implementation(project(":platform:ui"))
  implementation(project(":platform:util"))

  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.swing)

  implementation(compose.desktop.currentOs)
  implementation(compose.material3)
  implementation(compose.components.resources)

  testImplementation(libs.junit.jupiter)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
  useJUnitPlatform()
}

compose.desktop {
  application {
    mainClass = "com.nexide.MainKt"
    jvmArgs += listOf("--add-modules", "jdk.jdi")
    nativeDistributions {
      targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
      packageName = "NexIDE"
      packageVersion = "0.2.1"
      description = "NexIDE — modern IDE inspired by IntelliJ IDEA"
      copyright = "© 2026 NexIDE Contributors"
      vendor = "NexIDE"
    }
  }
}
