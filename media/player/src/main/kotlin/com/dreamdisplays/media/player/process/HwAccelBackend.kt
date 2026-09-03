// HwAccelBackend.kt

package com.dreamdisplays.media.player.process

import com.dreamdisplays.util.OsInfo

/**
 * Hardware-accelerated video decoder backends supported by `FFmpeg`.
 *
 * Offloads H.264 / HEVC / VP9 / AV1 decoding to the GPU's video-decode block:
 *   - Android (Zalith / MobileGlues): MediaCodec, when an Android Surface bridge is available
 *   - macOS:                          VideoToolbox
 *   - Windows:                        D3D11VA
 *   - Linux:                          VAAPI
 *
 * Android / MediaCodec notes
 * --------------------------
 * The prebuilt `libffmpeg.so` from hzw1199/Android-FFmpeg-Prebuilt (FFmpeg 9.0)
 * is compiled with MediaCodec hardware decode support for H.264, HEVC, and MPEG-4.
 * The correct hwaccel invocation is:
 *
 *   -hwaccel mediacodec -hwaccel_output_format mediacodec -c:v h264_mediacodec
 *
 * The `android_hardware_buffer` output format is NOT available in this build —
 * that requires a custom FFmpeg compile with --enable-android-hardware-buffer,
 * which hzw1199's prebuilt does not include.
 *
 * MediaCodec in FFmpeg requires an Android `Surface` to be passed via
 * `av_mediacodec_default_init()` before `avcodec_open2()` is called.
 * Without it FFmpeg emits: "No device available for decoder: device type mediacodec needed".
 * That Surface must be wired up in the native / JNI layer that drives this player.
 *
 * MobileGlues / Turnip note:
 * MobileGlues translates OpenGL → Vulkan (Turnip).  MediaCodec decoded frames
 * are delivered via a `SurfaceTexture` and then sampled as an external OES
 * texture.  MobileGlues exposes `GL_OES_EGL_image_external`, so this path works
 * without needing `AHardwareBuffer` / Vulkan import.
 */
enum class HwAccelBackend(val ffmpegName: String?, val hwOutputFormat: String?, val lavCode: Int) {

    /**
     * Android MediaCodec hardware decode.
     *
     * Supported codecs in this FFmpeg build: H.264, HEVC, MPEG-4.
     * Requires a valid `Surface` passed to the native decoder before open.
     * Output format `"mediacodec"` delivers frames to the attached Surface.
     */
    MEDIACODEC("mediacodec", "mediacodec", 6),

    /** Apple platforms — H.264, HEVC, VP9, ProRes. */
    VIDEOTOOLBOX("videotoolbox", "videotoolbox_vld", 2),

    /** Windows Direct3D 11 Video Acceleration. */
    D3D11VA("d3d11va", "d3d11", 3),

    /**
     * Linux Video Acceleration API.
     * AMD / Intel native; NVIDIA requires the proprietary VAAPI bridge.
     */
    VAAPI("vaapi", "vaapi", 4),

    /** NVIDIA CUDA / NVDEC — fastest on NVIDIA, limited to NVIDIA hardware. */
    CUDA("cuda", "cuda", 5),

    /** Software decoding — last resort, always available. */
    NONE(null, null, 0);

    companion object {

        // ---------------------------------------------------------------------------------
        // Platform detection
        // ---------------------------------------------------------------------------------

        /**
         * True when running inside Zalith / PojavLauncher on Android.
         *
         * Checks launcher-specific env vars rather than `java.vm.name` because
         * the JVM inside Zalith is OpenJDK (not Dalvik), so the VM name alone
         * is not a reliable Android indicator in this context.
         */
        private val isAndroid: Boolean by lazy {
            System.getProperty("os.version")?.contains("android", ignoreCase = true) == true ||
            System.getenv("POJAV_NATIVEDIR") != null ||
            System.getenv("POJAV_FFMPEG_PATH") != null
        }

        /** True when MobileGlues is the active GL → Vulkan translation layer. */
        @Suppress("unused")
        private val isMobileGlues: Boolean by lazy {
            System.getenv("LIBGL_EGL")?.contains("mobileglues", ignoreCase = true) == true ||
            System.getenv("POJAVEXEC_EGL")?.contains("mobileglues", ignoreCase = true) == true
        }

        // ---------------------------------------------------------------------------------
        // Default selection
        // ---------------------------------------------------------------------------------

        /**
         * Picks the best backend for the current platform.
         *
         * Prefers broad compatibility over peak speed — a stream that fails to decode
         * is worse than one that decodes a little slower.
         *
         * Android → [MEDIACODEC] only when a Surface bridge is wired into the native pipeline
         * macOS            → [VIDEOTOOLBOX]
         * Windows          → [D3D11VA]
         * Linux            → [VAAPI]
         * Unknown          → [NONE]
         */
        fun detectDefault(): HwAccelBackend = when {
            // The guest JVM and FFmpeg subprocess cannot create or receive an Android Surface.
            // Keep Android on software decoding until a launcher-provided Surface bridge exists.
            isAndroid -> NONE
            OsInfo.isMac     -> VIDEOTOOLBOX
            OsInfo.isWindows -> D3D11VA
            OsInfo.isLinux   -> VAAPI
            else             -> NONE
        }

        // ---------------------------------------------------------------------------------
        // Fallback chain
        // ---------------------------------------------------------------------------------

        /**
         * Returns the next backend to try after [current] fails.
         *
         * Android: MEDIACODEC → NONE  (no intermediate surface-only variant needed;
         *          if MediaCodec init fails the Surface wiring is broken and software
         *          decode is the only safe fallback.)
         * Others:  <platform backend> → NONE
         */
        fun fallbackFor(current: HwAccelBackend): HwAccelBackend = when (current) {
            MEDIACODEC   -> NONE
            VIDEOTOOLBOX -> NONE
            D3D11VA      -> NONE
            VAAPI        -> NONE
            CUDA         -> NONE
            NONE         -> NONE
        }

        // ---------------------------------------------------------------------------------
        // Failure detection
        // ---------------------------------------------------------------------------------

        /**
         * Returns `true` if [stderr] looks like a hardware-decoder startup failure
         * (as opposed to an unrelated error), so the caller can invoke [fallbackFor].
         */
        fun looksLikeHwAccelFailure(stderr: String): Boolean {
            if (stderr.isEmpty()) return false
            val s = stderr.lowercase()
            return HWACCEL_FAIL_MARKERS.any { s.contains(it) }
        }

        private val HWACCEL_FAIL_MARKERS = listOf(
            // Generic hwaccel
            "hwaccel",
            "hardware acceleration",
            "failed setup for format",
            "no device available",
            "device creation failed",
            "no usable hwaccel",
            "decoder does not support",
            // Apple
            "videotoolbox",
            "scale_vt",
            // Windows
            "d3d11va",
            // Linux
            "vaapi",
            // NVIDIA
            "cuda",
            "cuvid",
            "nvdec",
            // Android / MediaCodec
            "mediacodec",
            "no android surface",
            "dequeue input buffer",   // MediaCodec queue timeout
            "codec error",
            "omx",                    // OMX backend errors on older Android stacks
            // FFmpeg filter errors
            "no such filter",
        )
    }
}
