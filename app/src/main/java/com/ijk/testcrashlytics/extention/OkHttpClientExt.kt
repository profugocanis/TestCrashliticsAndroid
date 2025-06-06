package com.ijk.testcrashlytics.extention

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

fun OkHttpClient.Builder.disableSslVerification(): OkHttpClient.Builder {
    val x509TrustManager: X509TrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }
    val trustAllCerts = arrayOf<TrustManager>(x509TrustManager)
    val sslContext = SSLContext.getInstance("TLSv1.3")
    sslContext.init(null, trustAllCerts, SecureRandom())
    this.sslSocketFactory(sslContext.socketFactory, x509TrustManager)
    this.hostnameVerifier { _, _ -> true }
    return this
}