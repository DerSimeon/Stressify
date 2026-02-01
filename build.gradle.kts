import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.kotlin)
}

group = "lol.simeon"
version = "1.0.1"

repositories {
    mavenCentral()
    google()
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    implementation(libs.material3.desktop)

    implementation(libs.clikt)
    implementation(libs.clikt.markdown)
    implementation(libs.mcprotocollib)
    implementation(libs.jansi)
    implementation(libs.coroutines.core)
    implementation(libs.serializer.plain)
    implementation(libs.serializer.ansi)
}

compose.desktop {
    application {
        mainClass = "lol.simeon.stressify.UiMainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Dmg,
                TargetFormat.Deb,
                TargetFormat.Rpm,
            )

            packageName = "Stressify"
            packageVersion = project.version.toString()
            vendor = "Simeon"
            copyright = "© 2025 Simeon L. All rights reserved."

            windows {
                shortcut = true
                menu = true
                perUserInstall = false
                upgradeUuid = "530bfa51-5962-4613-90c9-bf8072c38a71"
                iconFile.set(project.file("src/main/resources/icon/icon.ico"))
                exePackageVersion = packageVersion
                msiPackageVersion = packageVersion
            }

            macOS {
                bundleID = "lol.simeon.stressify"
                dockName = "Stressify"
                iconFile.set(project.file("src/main/resources/icon/icon.icns"))
            }

            linux {
                shortcut = true
                iconFile.set(project.file("src/main/resources/icon/icon.png"))
            }

            nativeDistributions {
                modules(
                    "java.naming",
                    "jdk.naming.dns"
                )
            }

            buildTypes.release.proguard {
                isEnabled = false
            }
        }
    }
}

kotlin {
    jvmToolchain(25)
}