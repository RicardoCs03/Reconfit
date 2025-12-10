plugins {
    alias(libs.plugins.android.application)
    //alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    //id("kotlin-kapt")
}

android {
    namespace = "com.example.reconfit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.reconfit"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    /*kotlinOptions {
        jvmTarget = "11"
    }*/
}

dependencies {
    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    // Android UI
    //implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    //óóimplementation(libs.play.services.location)
    // Dependencia principal de Lombok (Solo Compilación)
    compileOnly("org.projectlombok:lombok:1.18.30")
    // Procesador de Anotaciones para Java
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Procesador de Anotaciones para Kotlin
    //kapt("org.projectlombok:lombok:1.18.30")
    // (Opcional) Para pruebas
    //kaptTest("org.projectlombok:lombok:1.18.30")

    // Testing
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}