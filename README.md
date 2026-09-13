# HDMI Monitor (UVC) — Android

A minimal app that turns your phone into an external monitor for any HDMI
source connected through a USB-C **UVC capture card** (e.g. your Blackmagic
Pocket Cinema Camera 4K or Sony a6400 via an HDMI-to-USB-C capture dongle).

Features:
- Full-screen live preview of the capture signal
- Rule-of-thirds overlay (toggle with the GRID button)
- **Monitor only — nothing is recorded or stored on the phone.**

Note: triggering the *camera's* recording from the phone is not possible over
the HDMI-to-USB capture cable — that path only carries video into the phone and
has no command channel back to the camera. (Remote record is only doable over a
separate channel: Bluetooth on the Blackmagic, Wi-Fi on the Sony — unrelated to
this app.)

Built on the open-source AUSBC engine
(`com.github.jiangdongguo.AndroidUSBCamera:libausbc`).

## Build the APK with GitHub (no Android Studio needed)

1. Create a new GitHub repository (private is fine).
2. Upload every file in this folder, keeping the structure intact. The
   `.github/workflows/build.yml` file must keep that exact path.
3. Pushing automatically starts a build. Open the **Actions** tab, click the
   running "Build APK" job, and wait ~3–5 minutes.
4. On the finished run, scroll to **Artifacts** and download
   **hdmi-monitor-apk**. Unzip it to get `app-debug.apk`.
5. Copy the APK to your phone and install it (allow "install unknown apps"
   for your file manager/browser).

### Using git instead of the upload page
```bash
git init
git add .
git commit -m "HDMI monitor app"
git branch -M main
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```

## Using the app
1. Plug the capture dongle into the phone, camera HDMI into the dongle.
2. Open **HDMI Monitor**. Approve the USB permission dialog when it appears.
3. The live image fills the screen. Tap GRID to show/hide the thirds overlay.

## Notes / tweaks
- Default preview is 1280x720 MJPEG (best compatibility + low latency).
  To try 1080p, edit `UvcFragment.kt` → `getCameraRequest()` and set
  `setPreviewWidth(1920).setPreviewHeight(1080)`. If you get a black screen,
  your dongle doesn't support that mode — revert to 720p.
- This produces a *debug* APK (signed with the standard debug key), which is
  installable directly.
