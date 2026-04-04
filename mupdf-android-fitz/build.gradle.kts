plugins {
    id("com.android.library")
}

android {
    namespace = "com.artifex.mupdf.fitz"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/main/java")
            manifest.srcFile("src/main/AndroidManifest.xml")
            jniLibs.srcDir("src/main/jniLibs")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
