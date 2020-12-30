plugins {
  kotlin("multiplatform") version "1.4.0"
}

group = "com.redgear"
version = "1.0-SNAPSHOT"

val pwBinNomVersion = "0.1.18"

repositories {
  mavenCentral()
  maven(url = "https://repo.binom.pw/releases")
}
configurations.all {
  resolutionStrategy {
    dependencySubstitution {
      substitute(module("pw.binom.io:core-mingwx64")).with(module("pw.binom.io:core-mingwX64:$pwBinNomVersion"))
      substitute(module("pw.binom.io:env-mingwx64")).with(module("pw.binom.io:env-mingwX64:$pwBinNomVersion"))
    }
  }
}

kotlin {

  jvm()

  mingwX64("win64") {

    binaries {
      executable {

      }
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }

    val win64Main by getting {
      dependencies {
        implementation("pw.binom.io:file-mingwX64:$pwBinNomVersion")
      }
    }

    val jvmMain by getting {
      dependencies {

      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
      }
    }
  }
}
