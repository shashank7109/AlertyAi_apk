package com.alertyai.app.widget

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import com.alertyai.app.network.RetrofitClient
import com.alertyai.app.network.TokenManager
import org.json.JSONObject

/**
 * BroadcastReceiver that handles the widget "🎤 Voice" button tap.
 *
 * Behavior:
 *  1. Checks RECORD_AUDIO permission — opens MainActivity for permission request if missing.
 *  2. Starts Android's SpeechRecognizer (on-device). The recognizer auto-stops on silence
 *     (VAD built into Android), so no manual stop is needed.
 *  3. On result, sends transcript to /api/v2/tasks/from-text to create the task.
 *  4. Shows a Toast confirming the created task title.
 */
class VoiceWidgetReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "VoiceWidgetReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TaskWidgetReceiver.ACTION_WIDGET_VOICE) return

        // Check mic permission — if not granted, launch MainActivity to request it
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            val permIntent = Intent(context, com.alertyai.app.MainActivity::class.java).apply {
                action = TaskWidgetReceiver.ACTION_TOGGLE_NOTIF // reuse toggle action to request mic
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(permIntent)
            return
        }

        val token = TokenManager.getToken(context)
        if (token.isNullOrBlank()) {
            Toast.makeText(context, "Please log in to Smaran AI first", Toast.LENGTH_SHORT).show()
            return
        }

        startVoiceRecognition(context, token)
    }

    private fun startVoiceRecognition(context: Context, token: String) {
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // No EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH override → uses default VAD (auto-stop)
        }

        Toast.makeText(context, "🎤 Listening… speak your task", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "SpeechRecognizer started")

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech began")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech — processing…")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val transcript = matches?.firstOrNull()?.trim()

                speechRecognizer.destroy()

                if (transcript.isNullOrBlank()) {
                    Toast.makeText(context, "Could not understand. Try again.", Toast.LENGTH_SHORT).show()
                    return
                }

                Log.d(TAG, "Transcript: $transcript")
                Toast.makeText(context, "Creating task: \"$transcript\"", Toast.LENGTH_SHORT).show()

                // Send to backend on IO thread
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        createTaskFromText(context, token, transcript)
                    } catch (e: Exception) {
                        Log.e(TAG, "Task creation error: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to create task: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            override fun onError(error: Int) {
                speechRecognizer.destroy()
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH       -> "No speech detected. Try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Try again."
                    SpeechRecognizer.ERROR_AUDIO          -> "Audio recording error."
                    SpeechRecognizer.ERROR_NETWORK        -> "Network error during recognition."
                    else -> "Recognition error ($error). Try again."
                }
                Log.e(TAG, "Speech error: $msg")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // Must start on main thread
        CoroutineScope(Dispatchers.Main).launch {
            speechRecognizer.startListening(recognizerIntent)
        }
    }

    private suspend fun createTaskFromText(context: Context, token: String, transcript: String) {
        val encodedContent = java.net.URLEncoder.encode(transcript, "UTF-8")
        val url = "${RetrofitClient.BASE_URL}api/v2/tasks/from-text?content=$encodedContent&language=en"

        val client = RetrofitClient.okHttpClient
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $token")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JSONObject(body)
            val taskObj = json.optJSONObject("task")
            val title = taskObj?.optString("title") ?: transcript
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "✅ Task created: \"$title\"", Toast.LENGTH_LONG).show()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Server error ${response.code}: $body", Toast.LENGTH_LONG).show()
            }
        }
    }
}
