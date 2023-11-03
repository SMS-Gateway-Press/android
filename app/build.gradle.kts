plugins {
    id("com.android.application")
}

android {
    namespace = "com.smsgatewaypress.smsgatewaypressclient"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.smsgatewaypress.smsgatewaypressclient"
        minSdk = 26
        targetSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-preferences-rxjava3:1.0.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("org.mockito:mockito-android:5.5.0")
//    androidTestImplementation("androidx.test.ext:junit:1.1.5")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
//    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
//    androidTestImplementation("androidx.test:runner:1.5.2")
//    androidTestImplementation("androidx.test:rules:1.5.0")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}