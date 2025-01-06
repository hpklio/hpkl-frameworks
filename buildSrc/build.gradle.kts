plugins { `kotlin-dsl` }

repositories {
  mavenCentral()
  gradlePluginPortal()
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
  implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
  implementation("org.pkl-lang:pkl-core:0.27.1")
  implementation("org.pkl-lang:pkl-tools:0.27.1")
}

val pklPackageVersion = project.version

tasks.processResources {
  eachFile {
    if (name == "PklProject.template") {
      expand("pklPackageVersion" to pklPackageVersion)
    }
  }
}

kotlin { target { compilations.configureEach { kotlinOptions { jvmTarget = "17" } } } }