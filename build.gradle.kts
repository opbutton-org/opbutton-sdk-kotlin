plugins {
    id("com.android.library") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "1.9.24"
    id("maven-publish")
}

group = "org.opbutton"
version = "1.0.37"

android {
    namespace = "org.opbutton.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    api("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("io.livekit:livekit-android:2.24.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

android.publishing {
    singleVariant("release") {
        withSourcesJar()
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "org.opbutton"
                artifactId = "opbutton-sdk"
                version = project.version.toString()

                pom {
                    name.set("OpButton SDK")
                    description.set("Android SDK for OpButton remote debug panel")
                }
            }
        }
    }
}
