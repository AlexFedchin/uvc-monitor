package com.example.uvcmonitor

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio

/**
 * Displays the HDMI/UVC capture signal full-screen with a rule-of-thirds
 * overlay. Nothing is recorded or stored on the phone — this is a monitor only.
 * Extends the AUSBC CameraFragment, which handles USB permission, opening the
 * UVC device and rendering the preview.
 */
class UvcFragment : CameraFragment() {

    private lateinit var rootView: View
    private lateinit var cameraContainer: ViewGroup
    private lateinit var gridView: ThirdsGridView
    private lateinit var gridBtn: Button

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        rootView = inflater.inflate(R.layout.fragment_uvc, container, false)
        cameraContainer = rootView.findViewById(R.id.cameraViewContainer)
        gridView = rootView.findViewById(R.id.gridView)
        gridBtn = rootView.findViewById(R.id.gridBtn)

        gridBtn.setOnClickListener {
            gridView.visibility =
                if (gridView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        return rootView
    }

    // Return a fresh render view; the library inserts it into the container below.
    override fun getCameraView(): IAspectRatio = AspectRatioTextureView(requireContext())

    override fun getCameraViewContainer(): ViewGroup = cameraContainer

    override fun getGravity(): Int = Gravity.CENTER

    override fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(1280)   // change to 1920 for 1080p if your capture card supports it
            .setPreviewHeight(720)   // change to 1080 accordingly
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setDefaultRotateType(RotateType.ANGLE_0)
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG) // MJPEG = best for HDMI dongles
            .setAspectRatioShow(true)
            .create()
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        if (code == ICameraStateCallBack.State.ERROR) {
            Toast.makeText(requireContext(), "Camera error: ${msg ?: "unknown"}", Toast.LENGTH_SHORT)
                .show()
        }
    }
}
