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
import ru.transservice.routemanager.repositories.RootRepository
import java.util.concurrent.TimeUnit

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
    }.build()

    private var gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(RootRepository.baseUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build()

    fun getPostgrestApi() : PostgrestApi {
        return retrofit.create(PostgrestApi::class.java)
    }

    private val okHttpClientApache = OkHttpClient.Builder().apply {
        connectTimeout(60, TimeUnit.SECONDS)
    }.build()

    fun getApacheConnection(): PostgrestApi{
        val retrofitApache: Retrofit = Retrofit.Builder()
            .baseUrl(RootRepository.baseUrl)
            .client(okHttpClientApache)
            .build()
        return retrofitApache.create(PostgrestApi::class.java)
    }

    fun updateConnectionSettings(){
        retrofit = Retrofit.Builder()
            .baseUrl(RootRepository.baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }
}