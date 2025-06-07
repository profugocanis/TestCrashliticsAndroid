import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Paths
import java.util.UUID

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.ijk.testcrashlytics"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ijk.testcrashlytics"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // MARK: Networking
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // MARK: Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// MARK: AssembleAndSendFile
tasks.register("AssembleAndSendFile") {
//    val buildVariant = "Staging"
    val buildVariant = "Debug"
//    dependsOn("assemble$buildVariant")
    val buildDir = getLayout().buildDirectory.asFile.get().path
    val fileName = "${android.defaultConfig.versionCode}-talkback-${buildVariant.lowercase()}.apk"

    var gitMessages = ""
    doFirst {
        val command = listOf("git", "log", "-10", "--pretty=format:%h - %s")
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        gitMessages = process.inputStream.bufferedReader().readText()
        process.waitFor()
    }


    doLast {
        val filePath = Paths.get(
            buildDir,
            "outputs/apk/${buildVariant.lowercase()}/app-${buildVariant.lowercase()}.apk"
        )
        println("File path: $filePath")

        val file = File(filePath.toString())
        if (!file.exists()) {
            println("File not found: $filePath")
            return@doLast
        }

        val urlString = "http://mf.bot.nu:5678/webhook-test/upload-apk"
//        val urlString = "http://mf.bot.nu:5678/webhook/upload-apk"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        val boundary = UUID.randomUUID().toString()
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        connection.apply {
            doInput = true
            doOutput = true
            useCaches = false
            requestMethod = "POST"
            setRequestProperty(
                "Authorization",
                "tGEIvSRBppzTYLO3Suwrpb6D7JLudigdboeSJkJMrkIc7g2Aj05I6HkWqt2fQlF5"
            )
            setRequestProperty("Connection", "Keep-Alive")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        val outputStream = DataOutputStream(connection.outputStream)

        outputStream.apply {
            writeBytes(twoHyphens + boundary + lineEnd)
            writeBytes("Content-Disposition: form-data; name=\"body\"$lineEnd")
            writeBytes(lineEnd)
            writeBytes(gitMessages + lineEnd)
        }

        outputStream.apply {
            writeBytes(twoHyphens + boundary + lineEnd)
            writeBytes("Content-Disposition: form-data; name=\"filename\"$lineEnd")
            writeBytes(lineEnd)
            writeBytes(fileName + lineEnd)
        }

        // write the file
        outputStream.apply {
            writeBytes(twoHyphens + boundary + lineEnd)
            writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"$lineEnd")
            writeBytes("Content-Type: application/vnd.android.package-archive$lineEnd")
            writeBytes(lineEnd)

            file.inputStream().use { it.copyTo(this) }

            writeBytes(lineEnd)
            writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
            flush()
            close()
        }

        val responseCode = connection.responseCode
        val responseMessage = connection.inputStream.bufferedReader().readText()

        println("Response Code: $responseCode")
        println("Response Message: $responseMessage")
    }
}