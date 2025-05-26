plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
apply(from = "version-info.gradle.kts")

// 獲取函數引用
val getGitSha: () -> String by extra
val getBuildTime: () -> String by extra

android {
    namespace = "com.earthbook.log_exception_module"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.earthbook.log_exception_module"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 直接在這裡使用函數
        buildConfigField("String", "GIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.core.ktx)
//    implementation("com.jakewharton.timber:timber:5.0.1")
    debugImplementation(libs.leakcanary.android)
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")

    // Room 依賴
    val room_version = "2.7.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-rxjava3:$room_version")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.13.1")
    implementation(project(":rxTimber"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
