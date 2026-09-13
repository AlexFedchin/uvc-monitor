package com.example.uvcmonitor

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Installs a crash handler that shows the stack trace on screen (see
 * [CrashActivity]) instead of the app silently disappearing. Handy on a phone
 * without adb access.
 */
class UvcMonitorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // CrashActivity runs in its own ":crash" process; don't install the
        // handler there or a crash inside the crash screen would loop forever.
        if (currentProcessName().endsWith(CrashActivity.PROCESS_SUFFIX)) return

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
                startActivity(
                    Intent(this, CrashActivity::class.java)
                        .putExtra(CrashActivity.EXTRA_THREAD, thread.name)
                        .putExtra(CrashActivity.EXTRA_TRACE, trace)
                        .addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        )
                )
            } catch (e: Throwable) {
                previous?.uncaughtException(thread, throwable)
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    private fun currentProcessName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return getProcessName()
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.runningAppProcesses
            ?.firstOrNull { it.pid == Process.myPid() }
            ?.processName
            ?: ""
    }
}
