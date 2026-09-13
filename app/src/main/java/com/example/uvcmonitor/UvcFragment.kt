package com.example.uvcmonitor

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.camera.bean.CameraStatus
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.utils.bus.BusKey
import com.jiangdg.ausbc.utils.bus.EventBus
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio

/**
 * Displays the HDMI/UVC capture signal full-screen with optional
 * rule-of-thirds and luminance-histogram overlays. Nothing is recorded or
 * stored on the phone — this is a monitor only. Extends the AUSBC
 * CameraFragment, which handles USB permission, opening the UVC device and
 * rendering the preview.
 *
 * Written against AUSBC 3.2.7 (see app/build.gradle.kts for why not 3.3.x):
 * the camera is configured by overriding [getCameraClient], and camera state
 * arrives on the library's EventBus rather than an override hook.
 */
class UvcFragment : CameraFragment() {

    private lateinit var rootView: View
    private lateinit var cameraContainer: ViewGroup
    private lateinit var gridView: ThirdsGridView
    private lateinit var histogramView: HistogramView
    private lateinit var gridBtn: ImageButton
    private lateinit var histBtn: ImageButton

    // Read on the UVC frame thread, written on the UI thread.
    @Volatile private var histogramEnabled = false
    private var lastHistogramNanos = 0L
    private val histogramBins = IntArray(HistogramView.BINS)

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        rootView = inflater.inflate(R.layout.fragment_uvc, container, false)
        cameraContainer = rootView.findViewById(R.id.cameraViewContainer)
        gridView = rootView.findViewById(R.id.gridView)
        histogramView = rootView.findViewById(R.id.histogramView)
        gridBtn = rootView.findViewById(R.id.gridBtn)
        histBtn = rootView.findViewById(R.id.histBtn)

        gridBtn.setOnClickListener { toggle(gridView) }
        histBtn.setOnClickListener {
            histogramEnabled = toggle(histogramView)
        }
        return rootView
    }

    /** Flips a view between VISIBLE and GONE; returns true if it is now visible. */
    private fun toggle(view: View): Boolean {
        val show = view.visibility != View.VISIBLE
        view.visibility = if (show) View.VISIBLE else View.GONE
        return show
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
        addPreviewDataCallBack(previewCallback)
    }

    /**
     * Builds the luminance histogram from the Y plane of each NV21 frame.
     * Runs on the library's frame thread, so it is throttled and subsampled.
     */
    private val previewCallback = object : IPreviewDataCallBack {
        override fun onPreviewData(data: ByteArray?, format: IPreviewDataCallBack.DataFormat) {
            if (!histogramEnabled || data == null) return
            val now = System.nanoTime()
            if (now - lastHistogramNanos < HISTOGRAM_INTERVAL_NANOS) return
            lastHistogramNanos = now

            // NV21: full-res Y plane first (2/3 of the buffer), then interleaved VU.
            val yLen = data.size / 3 * 2
            histogramBins.fill(0)
            var i = 0
            while (i < yLen) {
                histogramBins[data[i].toInt() and 0xFF]++
                i += HISTOGRAM_STRIDE
            }
            histogramView.update(histogramBins)
        }
    }

    // Return a fresh render view; the library inserts it into the container below.
    override fun getCameraView(): IAspectRatio = AspectRatioTextureView(requireContext())

    override fun getCameraViewContainer(): ViewGroup = cameraContainer

    override fun getGravity(): Int = Gravity.CENTER

    override fun getCameraClient(): CameraClient {
        val request = CameraRequest.Builder()
            .setFrontCamera(false)
            .setPreviewWidth(1280)
            .setPreviewHeight(720)
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

    companion object {
        private const val HISTOGRAM_INTERVAL_NANOS = 100_000_000L // ~10 updates/s
        private const val HISTOGRAM_STRIDE = 7 // odd, so we don't lock onto columns
    }
}
