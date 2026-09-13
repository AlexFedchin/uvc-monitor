package com.example.uvcmonitor

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.camera.bean.CameraStatus
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.utils.bus.BusKey
import com.jiangdg.ausbc.utils.bus.EventBus
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio

/**
 * Displays the HDMI/UVC capture signal full-screen with a rule-of-thirds
 * overlay. Nothing is recorded or stored on the phone — this is a monitor only.
 * Extends the AUSBC CameraFragment, which handles USB permission, opening the
 * UVC device and rendering the preview.
 *
 * Written against AUSBC 3.2.7 (see app/build.gradle.kts for why not 3.3.x):
 * the camera is configured by overriding [getCameraClient], and camera state
 * arrives on the library's EventBus rather than an override hook.
 */
class UvcFragment : CameraFragment() {

    private lateinit var rootView: View
    private lateinit var cameraContainer: ViewGroup
    private lateinit var gridView: ThirdsGridView
    private lateinit var gridBtn: ImageButton
    private lateinit var resBtn: TextView

    private var current = Resolution.P720

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        rootView = inflater.inflate(R.layout.fragment_uvc, container, false)
        cameraContainer = rootView.findViewById(R.id.cameraViewContainer)
        gridView = rootView.findViewById(R.id.gridView)
        gridBtn = rootView.findViewById(R.id.gridBtn)
        resBtn = rootView.findViewById(R.id.resBtn)

        gridBtn.setOnClickListener {
            gridView.visibility =
                if (gridView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        resBtn.setOnClickListener { switchResolution(current.other()) }
        return rootView
    }

    private fun switchResolution(target: Resolution) {
        // The size list is only known once the UVC device is open; if we have
        // it, refuse sizes the device doesn't advertise instead of letting the
        // library fall back to something random.
        val sizes = getAllPreviewSizes()
        if (!sizes.isNullOrEmpty() &&
            sizes.none { it.width == target.width && it.height == target.height }
        ) {
            Toast.makeText(
                requireContext(),
                getString(R.string.res_unsupported, getString(target.labelRes)),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        current = target
        resBtn.setText(target.labelRes)
        updateResolution(target.width, target.height)
    }

    override fun initData() {
        super.initData()
        // The UVC strategy posts START / STOP / ERROR / ERROR_PREVIEW_SIZE here.
        EventBus.with<CameraStatus>(BusKey.KEY_CAMERA_STATUS).observe(this) { status ->
            if (status.code < 0) {
                Toast.makeText(
                    requireContext(),
                    "Camera error: ${status.message ?: "unknown"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Return a fresh render view; the library inserts it into the container below.
    override fun getCameraView(): IAspectRatio = AspectRatioTextureView(requireContext())

    override fun getCameraViewContainer(): ViewGroup = cameraContainer

    override fun getGravity(): Int = Gravity.CENTER

    override fun getCameraClient(): CameraClient {
        val request = CameraRequest.Builder()
            .setFrontCamera(false)
            .setPreviewWidth(current.width)
            .setPreviewHeight(current.height)
            .create()
        // CameraUvcStrategy negotiates MJPEG first and falls back to YUYV, so no
        // explicit preview-format setting is needed for HDMI capture dongles.
        return CameraClient.newBuilder(requireContext())
            .setEnableGLES(true)     // OpenGL render path
            .setRawImage(true)
            .setCameraStrategy(CameraUvcStrategy(requireContext()))
            .setCameraRequest(request)
            .setDefaultRotateType(RotateType.ANGLE_0)
            .openDebug(false)
            .build()
    }

    private enum class Resolution(val width: Int, val height: Int, val labelRes: Int) {
        P720(1280, 720, R.string.res_720p),
        P1080(1920, 1080, R.string.res_1080p);

        fun other() = if (this == P720) P1080 else P720
    }
}
