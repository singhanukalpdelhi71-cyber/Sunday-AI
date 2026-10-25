
package com.sunday.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

private val Night = Color(0xFF050914)
private val Panel = Color(0xFF0B1425)
private val Cyan = Color(0xFF45E6FF)
private val Blue = Color(0xFF537BFF)
private val Muted = Color(0xFF8B9BB5)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            SundayScreen(
                onSpeak = { speak(it) },
                onListen = { startListening() },
                onOpenApp = { openApp(it) }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    private fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "sunday_reply")
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Sunday")
        }
        try {
            startActivityForResult(intent, 42)
        } catch (_: Exception) {
            speak("Speech recognition is not available on this device.")
        }
    }

    @Deprecated("Legacy callback used for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            val words = data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!words.isNullOrBlank()) handleCommand(words)
        }
    }

    private fun handleCommand(raw: String) {
        val command = raw.lowercase(Locale.getDefault())
            .removePrefix("hey sunday")
            .removePrefix("sunday")
            .trim()

        val response = when {
            command.contains("hello") || command.contains("hi") ->
                "Hello. Sunday is online."

            command.contains("your name") ->
                "I am Sunday, your personal assistant."

            command.contains("time") ->
                "The current time is " +
                    java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
                        .format(java.util.Date())

            command.startsWith("open ") -> {
                val app = command.removePrefix("open ").trim()
                if (openApp(app)) "Opening $app."
                else "I couldn't find $app on this device."
            }

            command.contains("help") ->
                "You can ask me to tell the time or open supported apps."

            command.isBlank() ->
                "I'm listening. What would you like me to do?"

            else ->
                "I heard you say $command. My online AI brain is not connected yet."
        }

        speak(response)
    }

    private fun openApp(name: String): Boolean {
        val packageName = when {
            name.contains("youtube") -> "com.google.android.youtube"
            name.contains("whatsapp") -> "com.whatsapp"
            name.contains("chrome") -> "com.android.chrome"
            name.contains("spotify") -> "com.spotify.music"
            name.contains("gmail") -> "com.google.android.gm"
            name.contains("maps") -> "com.google.android.apps.maps"
            name.contains("instagram") -> "com.instagram.android"
            name.contains("calendar") -> "com.google.android.calendar"
            name.contains("settings") -> "com.android.settings"
            else -> return false
        }

        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?: return false

        return try {
            startActivity(launch)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun SundayScreen(
    onSpeak: (String) -> Unit,
    onListen: () -> Unit,
    onOpenApp: (String) -> Boolean
) {
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("SYSTEM READY") }
    var history by remember {
        mutableStateOf(listOf("Sunday" to "Systems online. How may I assist you?"))
    }

    val transition = rememberInfiniteTransition(label = "orb")
    val outerScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outerScale"
    )
    val innerScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "innerScale"
    )

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Night,
            surface = Panel,
            primary = Cyan
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0C1930), Night, Color(0xFF02040A))
                    )
                )
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, Cyan.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", color = Cyan, fontSize = 24.sp, fontWeight = FontWeight.Light)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "S U N D A Y",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "PERSONAL INTELLIGENCE",
                        color = Muted,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    )
                }

                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Muted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .border(1.dp, Cyan.copy(alpha = 0.13f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.WifiTethering, null, tint = Cyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(status, color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    Text("Local voice interface active", color = Muted, fontSize = 11.sp)
                }
                Box(
                    Modifier
                        .size(7.dp)
                        .background(Color(0xFF55F6A8), CircleShape)
                )
            }

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(246.dp)
                        .scale(outerScale)
                        .border(1.dp, Cyan.copy(alpha = 0.12f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .border(1.dp, Blue.copy(alpha = 0.3f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(innerScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Cyan.copy(alpha = 0.25f),
                                    Blue.copy(alpha = 0.13f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                        .border(1.dp, Cyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF244C83),
                                        Color(0xFF0D1D38),
                                        Night
                                    )
                                ),
                                CircleShape
                            )
                            .border(2.dp, Cyan.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "S",
                            color = Color.White,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.ExtraLight
                        )
                    }
                }
            }

            Text(
                "YOUR ASSISTANT",
                modifier = Modifier.fillMaxWidth(),
                color = Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 10.sp,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "At your command.",
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .background(Panel.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { item ->
                    Column {
                        Text(
                            item.first.uppercase(),
                            color = Cyan,
                            fontSize = 9.sp,
                            letterSpacing = 1.5.sp
                        )
                        Text(item.second, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(18.dp))
                    .border(1.dp, Cyan.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (message.isEmpty()) {
                                Text("Talk to Sunday...", color = Muted, fontSize = 14.sp)
                            }
                            inner()
                        }
                    }
                )

                IconButton(
                    onClick = {
                        status = "PROCESSING"
                        val sent = message.trim()
                        if (sent.isNotEmpty()) {
                            history = history + ("You" to sent)
                            val reply = when {
                                sent.startsWith("open ", true) -> {
                                    val app = sent.substringAfter(" ").trim()
                                    if (onOpenApp(app)) "Opening $app."
                                    else "I couldn't find $app."
                                }
                                sent.contains("hello", true) ->
                                    "Hello. Sunday is online."
                                else ->
                                    "I received your message. Connect an AI service to enable natural conversation."
                            }
                            history = history + ("Sunday" to reply)
                            onSpeak(reply)
                            message = ""
                        }
                        status = "SYSTEM READY"
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Cyan)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        status = "LISTENING"
                        onListen()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan.copy(alpha = 0.13f),
                        contentColor = Cyan
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("TALK TO SUNDAY", letterSpacing = 1.sp, fontSize = 11.sp)
                }
            }
        }
    }
}
