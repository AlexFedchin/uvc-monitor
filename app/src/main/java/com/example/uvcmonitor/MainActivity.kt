package com.example.uvcmonitor

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Hosts [UvcFragment], but only after the two permissions the AUSBC library
 * silently depends on are sorted out:
 *
 *  1. CAMERA — CameraClient.openCamera() refuses to run without it on
 *     targetSdk >= 28, even though we only ever talk to a USB device.
 *  2. USB device permission — the library requests it with an immutable
 *     PendingIntent, which on Android 12+ means the system can't fill in the
 *     "granted" result and the library concludes it was denied. So we ask
 *     first, with a mutable explicit intent, and hand over once granted.
 */
class MainActivity : AppCompatActivity() {

    private val usbManager by lazy { getSystemService(Context.USB_SERVICE) as UsbManager }
    private var monitorShown = false

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Camera permission is required by the capture library",
                    Toast.LENGTH_LONG
                ).show()
            }
            requestUsbPermissionThenShow()
        }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                Toast.makeText(context, "USB device permission denied", Toast.LENGTH_LONG).show()
            }
            showMonitor()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ContextCompat.registerReceiver(
            this,
            usbPermissionReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        if (savedInstanceState != null) {
            // FragmentManager restores UvcFragment itself.
            monitorShown = true
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            requestUsbPermissionThenShow()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(usbPermissionReceiver)
        super.onDestroy()
    }

    private fun requestUsbPermissionThenShow() {
        val device = usbManager.deviceList.values
            .firstOrNull { isVideoDevice(it) && !usbManager.hasPermission(it) }
        if (device == null) {
            // Nothing attached, or already granted (e.g. launched from the
            // USB_DEVICE_ATTACHED prompt) — go straight to the monitor.
            showMonitor()
            return
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(packageName),
            flags
        )
        usbManager.requestPermission(device, pendingIntent)
    }

    private fun showMonitor() {
        if (monitorShown || isFinishing) return
        monitorShown = true
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, UvcFragment())
            .commit()
    }

    /** UVC device: class 14 at device level, or (typical for capture dongles) a class-14 interface. */
    private fun isVideoDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_VIDEO) return true
        for (i in 0 until device.interfaceCount) {
            if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_VIDEO) return true
        }
        return false
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.uvcmonitor.USB_PERMISSION"
    }
}
