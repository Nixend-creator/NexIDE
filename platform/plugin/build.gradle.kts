plugins {
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  api(project(":platform:core"))
  api(project(":platform:util"))
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.junit.jupiter)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
  useJUnitPlatform()
}
