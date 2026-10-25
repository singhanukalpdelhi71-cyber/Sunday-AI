
package com.sunday.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class WakeWordService : Service() {

    companion object {
        const val CHANNEL_ID = "sunday_wake_word"
        const val NOTIFICATION_ID = 1001
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var isStopping = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        isStopping = false

        val notification = buildNotification(
            "Preparing speech recognition..."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (speechRecognizer == null) {
            startRecognition()
        }

        return START_NOT_STICKY
    }

    private fun startRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification(
                "Speech recognition unavailable on this phone"
            )
            stopSelf()
            return
        }

        try {
            speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(this)

            speechRecognizer?.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        updateNotification("Listening for Hey Sunday...")
                    }

                    override fun onBeginningOfSpeech() {
                        updateNotification("Speech detected; listening...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        updateNotification("Processing speech...")
                    }

                    override fun onError(error: Int) {
                        isListening = false

                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO ->
                                "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT ->
                                "Recognizer client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "Microphone permission missing"
                            SpeechRecognizer.ERROR_NETWORK ->
                                "Speech recognition network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "Speech recognition network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH ->
                                "No speech recognized; retrying..."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                "Recognizer busy; retrying..."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "No speech detected; retrying..."
                            else ->
                                "Speech recognition error: $error"
                        }

                        updateNotification(message)
                        scheduleRestart()
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false

                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                        val phrase = matches?.firstOrNull().orEmpty()
                        val normalized = phrase.lowercase().trim()

                        if (
                            normalized.contains("hey sunday") ||
                            normalized == "sunday" ||
                            normalized.startsWith("sunday ")
                        ) {
                            updateNotification(
                                "Wake word detected: $phrase"
                            )
                        } else {
                            updateNotification(
                                "Heard: ${
                                    phrase.ifBlank { "no speech result" }
                                }"
                            )
                        }

                        scheduleRestart()
                    }

                    override fun onPartialResults(
                        partialResults: Bundle?
                    ) {
                        val matches = partialResults?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                        val phrase = matches?.firstOrNull().orEmpty()

                        if (phrase.isNotBlank()) {
                            updateNotification("Hearing: $phrase")
                        }
                    }

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?
                    ) {}
                }
            )

            listenAgain()

        } catch (_: Exception) {
            updateNotification("Couldn't initialize speech recognition")
            stopSelf()
        }
    }

    private fun listenAgain() {
        if (isStopping || isListening) return

        try {
            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    true
                )
                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
                )
            }

            speechRecognizer?.startListening(intent)

        } catch (_: Exception) {
            isListening = false
            updateNotification("Couldn't start listening; retrying...")
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        handler.removeCallbacksAndMessages(null)

        if (isStopping) return

        handler.postDelayed({
            if (!isStopping && speechRecognizer != null) {
                isListening = false
                listenAgain()
            }
        }, 1500)
    }

    private fun buildNotification(message: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Sunday AI")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val manager =
            getSystemService(NotificationManager::class.java)

        manager.notify(
            NOTIFICATION_ID,
            buildNotification(message)
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sunday voice service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isStopping = true
        handler.removeCallbacksAndMessages(null)

        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
