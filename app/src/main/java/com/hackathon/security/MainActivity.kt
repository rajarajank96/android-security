package com.hackathon.security

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaAction
import com.google.android.recaptcha.RecaptchaClient
import com.hackathon.security.network.Event
import com.hackathon.security.network.Root
import com.hackathon.security.network.RootEvent
import com.hackathon.security.network.service
import com.hackathon.security.ui.theme.AndroidSecurityTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback

class MainActivity : ComponentActivity()
{
    private lateinit var recaptchaClient: RecaptchaClient

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidSecurityTheme {
                var output by remember {
                    mutableStateOf("")
                }
                var isBlocked by remember {
                    mutableStateOf(false)
                }
                val coroutineScope = rememberCoroutineScope()
                // A surface container using the 'background' color from the theme

                Column {

                    if (isBlocked)
                    {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(painter = painterResource(id = R.drawable.err), contentDescription = "error")
                            Text(
                                text = "Your app has been blocked ! Contact Support !",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                    else
                    {
                        Text(
                            text = "  $output",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(450.dp)
                                .padding(horizontal = 8.dp, vertical = 24.dp)
                                .border(2.dp, color = Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.height(5.dp))

                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            onClick = {
                                initializeRecaptchaClient(
                                    onSuccess = {
                                        output = "Successfully initialized RecaptchaClient !"
                                    },
                                    onFailure = {
                                        output = "Initializing RecaptchaClient failed due to - $it."
                                    }
                                ) }
                        ) {
                            Text(text = "Initialize Recaptcha client")
                        }
                        Spacer(modifier = Modifier.height(5.dp))

                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            onClick = { executeLoginAction(
                                onSuccess = {
                                    output = "Successfully executed login action: Token - $it."
//                                    println("<<< $it")
                                    coroutineScope.launch {
                                        delay(2000)
                                    }
                                    createAssessment(
                                        token = it,
                                        onSuccess = { score ->
                                            if ( score <= 0.4F )
                                            {
                                                isBlocked = true
                                            }
                                            else
                                            {
                                                output = "Risk Analysis passed with score: $score."
                                            }
                                        },
                                        onFailure = {
                                            output = "Risk Analysis failed due to - $it."
                                        }
                                    )
                                },
                                onFailure = {
                                    output = "Executing Login action failed due to - $it."
                                }
                            ) }
                        ) {
                            Text(text = "Login")
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
            }
        }
    }

    private fun initializeRecaptchaClient(onSuccess: () -> Unit, onFailure: (exception: Exception) -> Unit)
    {
        lifecycleScope.launch {
            Recaptcha.getClient(application, lowRiskKey)
                .onSuccess { client ->
                    recaptchaClient = client
                    onSuccess()
                }
                .onFailure { exception ->
                    // Handle communication errors ...
                    // See "Handle communication errors" section
                    onFailure(Exception(exception.localizedMessage))
                }
        }
    }

    private fun executeLoginAction(onSuccess: (token: String) -> Unit, onFailure: (exception: Exception) -> Unit)
    {
        lifecycleScope.launch {
            recaptchaClient
                .execute(RecaptchaAction.LOGIN)
                .onSuccess { token ->
                    onSuccess( token )
                }
                .onFailure { exception ->
                    onFailure(Exception(exception.localizedMessage))
                }
        }
    }

    private fun createAssessment( token: String, onSuccess: (score: Float) -> Unit, onFailure: (exception: Exception) -> Unit )
    {
        try
        {
            val event = Event(
                token = token,
                expectedAction = "login"
            )
            val rootEvent = RootEvent(event = event)
            val resp = service.createAssessment(event = rootEvent)
            resp.enqueue(object :Callback<Root>
            {
                override fun onResponse(
                    call: Call<Root>,
                    response: retrofit2.Response<Root>
                ) {
                    println("<<< RESP: ${response.body()}")
                    val score = response.body()?.riskAnalysis?.score ?: 0F
                    onSuccess( score )
                }

                override fun onFailure(
                    call: Call<Root>,
                    t: Throwable
                ) {
                    println("<<< FAIL: $t")
                    onFailure(Exception(t.localizedMessage))
                }
            })

        }
        catch (e: Exception)
        {
            println("<<< EXC: $e")
        }
    }
}