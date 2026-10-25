
package com.sunday.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.content.pm.ServiceInfo
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
    private var wakeWordDetected = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val notification = buildNotification(
            "Starting speech recognition..."
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

        startRecognition()
        return START_NOT_STICKY
    }

    private fun startRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification(
                "Speech recognition isn't available on this phone"
            )
            stopSelf()
            return
        }

        try {
            speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(this)

            speechRecognizer?.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: android.os.Bundle?) {
                        isListening = true
                        if (!wakeWordDetected) {
                            updateNotification("Listening for Hey Sunday...")
                        }
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        scheduleRestart()
                    }

                    override fun onResults(results: android.os.Bundle?) {
                        isListening = false

                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                        val phrase = matches?.firstOrNull().orEmpty()
                            .lowercase()

                        if (
                            phrase.contains("hey sunday") ||
                            phrase == "sunday" ||
                            phrase.startsWith("sunday ")
                        ) {
                            wakeWordDetected = true
                            updateNotification(
                                "Hey Sunday detected: $phrase"
                            )
                        } else if (!wakeWordDetected) {
                            updateNotification(
                                "Heard: ${phrase.ifBlank { "no speech detected" }}"
                            )
                        }

                        scheduleRestart()
                    }

                    override fun onPartialResults(
                        partialResults: android.os.Bundle?
                    ) {}

                    override fun onEvent(
                        eventType: Int,
                        params: android.os.Bundle?
                    ) {}
                }
            )

            listenAgain()
        } catch (_: Exception) {
            updateNotification("Couldn't start speech recognition")
            stopSelf()
        }
    }

    private fun listenAgain() {
        if (isListening) return

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
            isListening = true
        } catch (_: Exception) {
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (speechRecognizer != null) {
                isListening = false
                listenAgain()
            }
        }, 1000)
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
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
