plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.moogerscouncil"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.moogerscouncil"
        minSdk = 31
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.activity)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

tasks.register<Javadoc>("generateJavadoc") {
    dependsOn("compileDebugJavaWithJavac")

    val mainSrc = fileTree("src/main/java") {
        include("**/*.java")
    }
    source = mainSrc

    exclude("**/test/**")
    exclude("**/androidTest/**")

    val studioJavadoc = file("/opt/android-studio/jbr/bin/javadoc")
    if (studioJavadoc.exists()) {
        executable = studioJavadoc.absolutePath
    }

    val outputDir = file("$projectDir/javadoc_output")
    destinationDir = outputDir

    isFailOnError = false

    (options as StandardJavadocDocletOptions).apply {
        isSplitIndex = true
        memberLevel = JavadocMemberLevel.PROTECTED
    }

    doFirst {
        // Pull classpath from the compile task — it already has boot classpath +
        // all AAR classes.jar entries extracted by the Android Gradle plugin.
        val compileTask = tasks.getByName("compileDebugJavaWithJavac") as JavaCompile
        classpath += compileTask.classpath

        if (outputDir.exists()) outputDir.deleteRecursively()
        outputDir.mkdirs()
    }
}