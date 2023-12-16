package com.hackathon.security.network

import com.google.gson.annotations.SerializedName
import com.hackathon.security.highRiskKey
import com.hackathon.security.lowRiskKey
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


internal lateinit var retrofit: Retrofit
var service: GCService = getClient().create(GCService::class.java)

data class RootEvent(
    @SerializedName("event")
    val event: Event
)
class Event constructor(
    private val token: String,
    private val siteKey: String = lowRiskKey,
    private val expectedAction: String
)

data class Root(
    val name: String,
    val event: ResponseEvent,
    val riskAnalysis: RiskAnalysis,
    val tokenProperties: TokenProperties
)

data class ResponseEvent(
    val token: String,
    val siteKey: String,
    val userAgent: String,
    val userIpAddress: String,
    val expectedAction: String,
    val hashedAccountId: String,
    val express: Boolean,
    val requestedUri: String,
    val wafTokenAssessment: Boolean,
    val ja3: String,
    val headers: List<Any>,
    val firewallPolicyEvaluation: Boolean
)

data class RiskAnalysis(
    val score: Float,
    val reasons: List<Any>,
    val extendedVerdictReasons: List<Any>
)

data class TokenProperties(
    val valid: Boolean,
    val invalidReason: String,
    val hostname: String,
    val androidPackageName: String,
    val iosBundleId: String,
    val action: String,
    val createTime: String
)



interface GCService
{
    @POST("assessments?key=AIzaSyAX9tfGOj1Kf10wkUGlbRSMYgR1AQApZ6g")
    fun createAssessment(@Body event: RootEvent): Call<Root>
}

fun getClient(): Retrofit {
    val interceptor = HttpLoggingInterceptor()
    interceptor.level = HttpLoggingInterceptor.Level.BODY
    val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    val clientBuilder = OkHttpClient.Builder()
    clientBuilder.addInterceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
       println("<<< API: ${response.body()}")
        response
    }

    retrofit = Retrofit.Builder()
        .baseUrl("https://recaptchaenterprise.googleapis.com/v1/projects/simpleapi-206310/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
    return retrofit
}