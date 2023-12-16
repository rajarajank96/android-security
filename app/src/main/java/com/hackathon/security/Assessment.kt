package com.hackathon.security

import com.google.cloud.recaptchaenterprise.v1.RecaptchaEnterpriseServiceClient
import com.google.cloud.recaptchaenterprise.v1.RecaptchaEnterpriseServiceSettings
import com.google.recaptchaenterprise.v1.Assessment
import com.google.recaptchaenterprise.v1.CreateAssessmentRequest
import com.google.recaptchaenterprise.v1.Event
import com.google.recaptchaenterprise.v1.ProjectName
import com.google.recaptchaenterprise.v1.RiskAnalysis.ClassificationReason
import java.io.IOException

val lowRiskKey = "6LeBezIpAAAAALU2D6I1Df1x5uOuj-8yhveFOqch"
val highRiskKey = "6LdeMjMpAAAAAE0TMSCd0Vjt37xxBhxd5IFVVCyw"
object ZIntegrityAssessment
{
    /**
     * Create an assessment to analyze the risk of an UI action. Assessment approach is the same for
     * both 'score' and 'checkbox' type recaptcha site keys.
     *
     * @param projectID : GCloud Project ID
     * @param recaptchaSiteKey : Site key obtained by registering a domain/app to use recaptcha
     * services. (score/ checkbox type)
     * @param token : The token obtained from the client on passing the recaptchaSiteKey.
     * @param recaptchaAction : Action name corresponding to the token.
     */
    @Throws(IOException::class)
    fun create(token: String)
    {
        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the `client.close()` method on the client to safely
        // clean up any remaining background resources.
        println("<<< 1: ${RecaptchaEnterpriseServiceSettings.getDefaultEndpoint()}")
        RecaptchaEnterpriseServiceClient.create().use { client ->
            println("<<< In: $client")

            // Set the properties of the event to be tracked.
            val event: Event = Event.newBuilder().setSiteKey("6LeBezIpAAAAALU2D6I1Df1x5uOuj-8yhveFOqch").setToken(token).build()

            // Build the assessment request.
            val createAssessmentRequest: CreateAssessmentRequest =
                CreateAssessmentRequest.newBuilder()
                    .setParent(ProjectName.of("simpleapi-206310").toString())
                    .setAssessment(Assessment.newBuilder().setEvent(event).build())
                    .build()
            val response: Assessment = client.createAssessment(createAssessmentRequest)

            // Check if the token is valid.
            if (!response.tokenProperties.valid)
            {
                println("<<< The CreateAssessment call failed because the token was: " + response.tokenProperties.invalidReason.name)
                return
            }

            // Check if the expected action was executed.
            // (If the key is checkbox type and 'action' attribute wasn't set, skip this check.)
            if (!response.tokenProperties.action.equals("login"))
            {
                println("<<< The action attribute in reCAPTCHA tag is: " + response.tokenProperties.action)
                println(
                    ("<<< The action attribute in the reCAPTCHA tag "
                            + "does not match the action ("
                            + "login"
                            + ") you are expecting to score")
                )
                return
            }

            // Get the reason(s) and the risk score.
            // For more information on interpreting the assessment,
            // see: https://cloud.google.com/recaptcha-enterprise/docs/interpret-assessment
            for (reason: ClassificationReason? in response.riskAnalysis.reasonsList)
            {
                println("<<< $reason")
            }
            val recaptchaScore: Float = response.riskAnalysis.score
            println("<<< The reCAPTCHA score is: $recaptchaScore")

            // Get the assessment name (id). Use this to annotate the assessment.
            val assessmentName: String = response.name
            println("<<< Assessment name: " + assessmentName.substring(assessmentName.lastIndexOf("/") + 1))
            client.close()
        }
    }
}