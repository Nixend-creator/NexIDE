plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.compose.multiplatform) apply false
  alias(libs.plugins.compose.compiler) apply false
}

allprojects {
  group = "com.nexide"
  version = "0.1.0-SNAPSHOT"
}

subprojects {
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
      freeCompilerArgs.add("-Xcontext-receivers")
    }
  }
}
