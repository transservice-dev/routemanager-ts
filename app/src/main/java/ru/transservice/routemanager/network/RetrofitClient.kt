package ru.transservice.routemanager.network

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.R
import ru.transservice.routemanager.repositories.RootRepository
import java.io.BufferedInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*


object RetrofitClient {

    private val okHttpClient = OkHttpClient.Builder().apply {
        connectTimeout(60, TimeUnit.SECONDS)
        addInterceptor(
            Interceptor { chain ->
                val builder = chain.request().newBuilder()
                builder.header("Content-Type", "application/json")
                builder.header("Authorization", "Bearer ${RootRepository.authPass}")
                builder.header(
                    "User-Agent",
                    "${RootRepository.deviceName} - version ${AppClass.appVersion} ${System.getProperty("http.agent")}"
                )
                return@Interceptor chain.proceed(builder.build())
            }
        )
        addInterceptor(
            Interceptor { chain ->
                val response: Response = chain.proceed(chain.request())
                val rawJson: String = response.body()!!.string()

                Log.d(
                    "${AppClass.TAG}: Retrofit",
                    """ request url: ${response.request().url()}
                              request code: ${response.code()}
                              $rawJson
                              ${if (!response.isSuccessful) "raw response: $rawJson." else ""} """
                )
                // Re-create the response before returning it because body can be read only once
                return@Interceptor response.newBuilder()
                    .body(ResponseBody.create(response.body()!!.contentType(), rawJson)).build()
            }
        )
        if (!RootRepository.baseUrl.contains("eko-ekb.ru")) {
            val sslSettings = customSSL()
            sslSocketFactory(sslSettings.first, sslSettings.second)
        }
    }.build()


    private fun customSSL(): Pair<SSLSocketFactory, X509TrustManager> {
        //val keyStoreType = KeyStore.getDefaultType()
        val cert = AppClass.appliactionContext().resources.openRawResource(R.raw.apache_selfsigned)
        val caInput: InputStream = BufferedInputStream(cert)
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val ca: X509Certificate = caInput.use {
            cf.generateCertificate(it) as X509Certificate
        }
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("ca", ca)
        }
        // Create a TrustManager that trusts the CAs inputStream our KeyStore
        val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
        val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
            init(keyStore)
        }

        // Create an SSLContext that uses our TrustManager
        val sslContext: SSLContext = SSLContext.getInstance("TLSv1.2").apply {
            init(null, tmf.trustManagers, null)
        }

        return sslContext.socketFactory to (tmf.trustManagers[0]  as X509TrustManager)
    }

    private var gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(RootRepository.baseUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build()

    fun getPostgrestApi() : PostgrestApi{
        return retrofit.create(PostgrestApi::class.java)
    }

    private val okHttpClientApache = OkHttpClient.Builder().apply {
        connectTimeout(60, TimeUnit.SECONDS)
        if (!RootRepository.baseUrl.contains("eko-ekb.ru")) {
            val sslSettings = customSSL()
            sslSocketFactory(sslSettings.first, sslSettings.second)
        }
    }.build()

    fun getApacheConnection(): PostgrestApi{
        val retrofitApache: Retrofit = Retrofit.Builder()
            .baseUrl(RootRepository.baseUrl)
            .client(okHttpClientApache)
            .build()
        return retrofitApache.create(PostgrestApi::class.java)
    }

    fun updateConnectionSettings(){
        okHttpClient.sslSocketFactory()
        retrofit = Retrofit.Builder()
            .baseUrl(RootRepository.baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }
}