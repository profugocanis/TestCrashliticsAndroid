//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
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
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
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

tasks.register("AssembleDebug") {
    group = "build"
    description = "\uD83D\uDCDD Runs assembleDebug build variant"
//    dependsOn("assembleDebug")
    dependsOn("assembleDebug")

    doLast {

        val apkDir = File(layout.buildDirectory.get().asFile, "outputs/apk/debug")
        val apkFiles = apkDir.listFiles { _, name -> name.endsWith(".apk") } ?: emptyArray()

        if (apkFiles.isNotEmpty()) {
            println("✅ Signed APK(s) generated:")
            apkFiles.forEach {
                println("📦 ${it.absolutePath}")
            }

            exec {
                commandLine("open", "$apkDir")
            }
        } else {
            println("⚠️ No APK files found in ${apkDir.absolutePath}")
        }
    }
}

tasks.register<Exec>("runTerminalCommand") {
//    val output = ByteArrayOutputStream()
//    exec {
//        commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
//        standardOutput = output
//    }
    commandLine("bash", "-c", "echo Hello from terminal")
//    println("Current branch: ${output.toString().trim()}")
}

// MARK: sendFile
tasks.register<Exec>("sendFile") {
//    dependsOn("assembleDebug")

    val output = ByteArrayOutputStream()
    commandLine("git", "log", "-10", "--pretty=format:%h - %s")
    standardOutput = output

    doLast {

        val fileName = "app-debug.apk"
        val filePath = Paths.get(buildDir.toString(), "outputs/apk/debug/$fileName")
        println("File path: $filePath")

        val file = File(filePath.toString())
        if (!file.exists()) {
            println("File not found: $filePath")
            return@doLast
        }

        val urlString = "http://mf.bot.nu:5678/webhook-test/d72f5c25-0ae5-4d5b-b349-42a1f52f05b7"
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
            setRequestProperty("Connection", "Keep-Alive")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        val outputStream = DataOutputStream(connection.outputStream)

        val text = output.toString().trim()
        outputStream.apply {
            writeBytes(twoHyphens + boundary + lineEnd)
            writeBytes("Content-Disposition: form-data; name=\"body\"$lineEnd")
            writeBytes(lineEnd)
            writeBytes(text + lineEnd)
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