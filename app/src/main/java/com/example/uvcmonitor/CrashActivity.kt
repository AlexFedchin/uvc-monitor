package com.example.uvcmonitor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess

/**
 * Full-screen view of an uncaught exception, launched by [UvcMonitorApp].
 * Runs in a separate process so it survives the main process being killed.
 */
class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        val trace = intent.getStringExtra(EXTRA_TRACE) ?: "(no stack trace)"
        val thread = intent.getStringExtra(EXTRA_THREAD) ?: "?"
        val report = buildString {
            appendLine("Thread: $thread")
            appendLine("App: $packageName (v${appVersion()})")
            appendLine(
                "Device: ${Build.MANUFACTURER} ${Build.MODEL}, " +
                    "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            )
            appendLine()
            append(trace)
        }
        findViewById<TextView>(R.id.crashText).text = report

        findViewById<Button>(R.id.copyBtn).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("crash report", report))
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.restartBtn).setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
            exitProcess(0)
        }
        findViewById<Button>(R.id.closeBtn).setOnClickListener {
            finishAffinity()
            exitProcess(0)
        }
    }

    private fun appVersion(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    companion object {
        const val EXTRA_TRACE = "trace"
        const val EXTRA_THREAD = "thread"
        const val PROCESS_SUFFIX = ":crash"
    }
}
