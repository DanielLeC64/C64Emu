
plugins {
    kotlin("jvm") version "1.9.21"
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
    applicationDefaultJvmArgs = listOf("-DromsPath=ROMS_PARENT_DIR/roms")
}

tasks.withType<CreateStartScripts>().configureEach {
    doLast {
        windowsScript.writeText(windowsScript.readText().replace("ROMS_PARENT_DIR", "%APP_HOME%"))
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.github.microutils:kotlin-logging:3.0.5")

    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("org.slf4j:slf4j-log4j12:2.0.3")
}
