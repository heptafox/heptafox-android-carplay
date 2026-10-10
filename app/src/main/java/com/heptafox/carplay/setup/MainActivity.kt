package com.heptafox.carplay.setup

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.heptafox.carplay.assistant.VoiceAssistant
import com.heptafox.carplay.playback.MediaLaunchers
import kotlinx.coroutines.launch

/**
 * Phone-side setup: grants the two permissions the car app needs, and lets you test a typed
 * command (same parsing and playback as the car, minus the microphone).
 */
class MainActivity : ComponentActivity() {

    private val assistant by lazy { VoiceAssistant(MediaLaunchers.create(this)) }

    private val requestMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatus() }

    private lateinit var status: TextView
    private lateinit var testResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
    }

    override fun onResume() {
        super.onResume()
        refreshStatus() // also picks up the overlay grant when returning from system settings
    }

    private fun refreshStatus() {
        status.text = buildString {
            appendLine("Microphone: ${describe(Permissions.hasMic(this@MainActivity))}")
            append("Background launch: ${describe(Permissions.canLaunchInBackground(this@MainActivity))}")
        }
    }

    private fun describe(granted: Boolean) = if (granted) "allowed ✅" else "not allowed ❌"

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun runTest(command: String) {
        testResult.text = "…"
        lifecycleScope.launch { testResult.text = assistant.handle(command).message }
    }

    private fun buildLayout(): View {
        status = TextView(this)
        testResult = TextView(this)
        val commandInput = EditText(this).apply {
            hint = "play believer on spotify"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply { text = "Hepta setup"; textSize = 24f })
            addView(status)
            addView(button("Allow microphone") { requestMic.launch(Manifest.permission.RECORD_AUDIO) })
            addView(button("Allow background launch") { openOverlaySettings() })
            addView(TextView(context).apply { text = "\nTry a command without the car:" })
            addView(commandInput)
            addView(button("Test") { runTest(commandInput.text.toString()) })
            addView(testResult)
        }

        // Android 16 draws apps edge to edge; keep content clear of the status/navigation bars and keyboard.
        val padding = (24 * resources.displayMetrics.density).toInt()
        return ScrollView(this).apply {
            addView(column)
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom)
                insets
            }
        }
    }

    private fun button(label: String, onClick: () -> Unit) =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }
}
