
plugins {
    kotlin("jvm") version "2.0.10"
    application
}

group = "c64"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

application {
    mainClass.set("c64.emulation.ui.EmulatorUIKt")
    applicationDefaultJvmArgs = listOf("-DromsPath=./roms")
}

tasks.withType<CreateStartScripts>().configureEach {
    doLast {
        windowsScript.writeText(windowsScript.readText().replace("-DromsPath=./roms", "-DromsPath=%APP_HOME%/roms"))
    }
}
distributions {
    main {
        contents {
            from("/roms") {
                into("roms")
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.github.microutils:kotlin-logging:3.0.5")

    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.slf4j:slf4j-log4j12:2.0.13")
}
