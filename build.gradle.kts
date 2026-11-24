buildscript {
    configurations.named("classpath") {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
            force("com.google.guava:guava:33.7.2-jre")
            force("org.apache.commons:commons-lang3:3.18.0")
            force("org.apache.httpcomponents:httpclient:4.5.14")
            force("org.apache.httpcomponents:httpmime:4.5.14")
            force("org.bitbucket.b_c:jose4j:0.9.6")
            force("org.bouncycastle:bcpkix-jdk18on:1.85")
            force("org.bouncycastle:bcprov-jdk18on:1.85")
            force("org.jdom:jdom2:2.0.6.1")
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.library") version "9.4.1"
    id("maven-publish")
}

group = "org.opbutton"
version = "1.0.40"

dependencyLocking {
    lockAllConfigurations()
}

configurations.configureEach {
    resolutionStrategy {
        force("com.google.guava:guava:33.7.2-jre")
        force("org.apache.commons:commons-lang3:3.18.0")
        force("org.apache.httpcomponents:httpclient:4.5.14")
        force("org.apache.httpcomponents:httpmime:4.5.14")
        force("org.bouncycastle:bcpkix-jdk18on:1.85")
        force("org.bouncycastle:bcprov-jdk18on:1.85")
    }
}

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    testImplementation("org.json:json:20260719")
    api("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("io.livekit:livekit-android:2.30.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    constraints {
        implementation("com.google.protobuf:protobuf-javalite:3.25.9") {
            because("3.25.5 and older versions are affected by GHSA-735f-pc8j-v9w8")
        }
    }
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
