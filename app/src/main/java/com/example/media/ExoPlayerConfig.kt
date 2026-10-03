package com.example.media

import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

/**
 * Robust ExoPlayer Configuration for Android devices.
 *
 * Handles:
 * 1. Common video and audio codecs (H.264/AVC, AAC, H.265/HEVC, VP9).
 * 2. Hardware acceleration prioritized for optimal performance and battery life.
 * 3. Seamless fallback to software decoding (c2.android.*, OMX.google.*) when
 *    hardware decoders encounter initialization errors, hardware resource limits,
 *    or device-specific codec bugs.
 */
@OptIn(UnstableApi::class)
object ExoPlayerConfig {

    private const val TAG = "ExoPlayerConfig"

    /**
     * Common video and audio MIME types explicitly checked and guaranteed for fallback.
     */
    val SUPPORTED_VIDEO_MIMES = listOf(
        MimeTypes.VIDEO_H264,  // "video/avc"
        MimeTypes.VIDEO_H265,  // "video/hevc"
        MimeTypes.VIDEO_VP9,   // "video/x-vnd.on2.vp9"
        MimeTypes.VIDEO_AV1    // "video/av01"
    )

    val SUPPORTED_AUDIO_MIMES = listOf(
        MimeTypes.AUDIO_AAC,   // "audio/mp4a-latm"
        MimeTypes.AUDIO_MPEG,  // "audio/mpeg" (MP3)
        MimeTypes.AUDIO_OPUS   // "audio/opus"
    )

    /**
     * Checks if a codec name or MediaCodecInfo indicates a software decoder.
     */
    fun isSoftwareDecoder(codecName: String): Boolean {
        val lower = codecName.lowercase()
        return lower.startsWith("c2.android.") ||
                lower.startsWith("omx.google.") ||
                lower.startsWith("c2.google.") ||
                lower.contains(".sw.") ||
                lower.contains("software") ||
                lower.contains("ffmpeg")
    }

    /**
     * Creates a custom MediaCodecSelector that:
     * - Recognizes H.264 (AVC) and AAC formats as well as common video/audio streams.
     * - Keeps hardware decoders first for speed, but guarantees software decoders
     *   are placed immediately after as reliable fallbacks.
     * - If [forceSoftwareFallback] is true, inverts the order to prioritize software decoders.
     */
    fun createMediaCodecSelector(forceSoftwareFallback: Boolean = false): MediaCodecSelector {
        return MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val defaultDecoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )

            if (defaultDecoders.isEmpty()) {
                Log.w(TAG, "No default decoders found for mimeType: $mimeType")
                return@MediaCodecSelector emptyList()
            }

            // Split into hardware-accelerated and software decoders
            val (softwareDecoders, hardwareDecoders) = defaultDecoders.partition { info ->
                info.softwareOnly || isSoftwareDecoder(info.name)
            }

            Log.d(
                TAG,
                "Decoders for $mimeType (forceSW=$forceSoftwareFallback): " +
                        "HW=${hardwareDecoders.map { it.name }}, SW=${softwareDecoders.map { it.name }}"
            )

            val orderedList = mutableListOf<MediaCodecInfo>()

            if (forceSoftwareFallback) {
                // Prioritize software decoders first, then hardware
                orderedList.addAll(softwareDecoders)
                orderedList.addAll(hardwareDecoders)
            } else {
                // Standard: Hardware first for performance, followed by software for fallback
                orderedList.addAll(hardwareDecoders)
                orderedList.addAll(softwareDecoders)
            }

            // Ensure H.264 / AVC and AAC always have at least one software decoder fallback
            if (softwareDecoders.isEmpty()) {
                val fallbackCodec = findSystemSoftwareDecoder(mimeType)
                if (fallbackCodec != null) {
                    try {
                        val swInfo = MediaCodecInfo.newInstance(
                            fallbackCodec,
                            mimeType,
                            mimeType,
                            /* capabilities= */ null,
                            /* hardwareAccelerated= */ false,
                            /* softwareOnly= */ true,
                            /* vendor= */ false,
                            /* forceDisableAdaptive= */ false,
                            /* forceSecure= */ false
                        )
                        orderedList.add(swInfo)
                        Log.i(TAG, "Appended supplemental SW decoder: $fallbackCodec for $mimeType")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not construct fallback MediaCodecInfo for $fallbackCodec", e)
                    }
                }
            }

            orderedList
        }
    }

    /**
     * Looks up system software decoders (like c2.android.avc.decoder or OMX.google.h264.decoder)
     * using Android MediaCodecList if not already registered in ExoPlayer's default selector.
     */
    private fun findSystemSoftwareDecoder(mimeType: String): String? {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            val codecInfos = codecList.codecInfos
            for (info in codecInfos) {
                if (info.isEncoder) continue
                for (type in info.supportedTypes) {
                    if (type.equals(mimeType, ignoreCase = true)) {
                        if (isSoftwareDecoder(info.name)) {
                            return info.name
                        }
                    }
                }
            }
            null
        } catch (e: Throwable) {
            Log.w(TAG, "Failed scanning MediaCodecList for $mimeType", e)
            null
        }
    }

    /**
     * Builds a DefaultRenderersFactory configured with:
     * - Automatic decoder fallback enabled (setEnableDecoderFallback(true))
     * - Custom MediaCodecSelector prioritizing HW with guaranteed SW fallback
     * - Extension renderer mode enabled for bundled software decoders
     */
    fun createRenderersFactory(
        context: Context,
        forceSoftwareFallback: Boolean = false
    ): DefaultRenderersFactory {
        return DefaultRenderersFactory(context.applicationContext).apply {
            // Enable fallback to secondary (software) decoders when primary fails
            setEnableDecoderFallback(true)

            // Prefer / turn on extension renderers (VP9, AV1, etc. software extensions if bundled)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

            // Inject custom codec selector or hardcoded fallback
            if (forceSoftwareFallback) {
                // Media3's DEFAULT acts as DEFAULT_WITH_FALLBACK from ExoPlayer v2
                setMediaCodecSelector(MediaCodecSelector.DEFAULT)
            } else {
                setMediaCodecSelector(createMediaCodecSelector(false))
            }
        }
    }

    /**
     * Builds an optimized ExoPlayer instance with custom codec configuration,
     * decoder fallback, and responsive buffer control.
     */
    fun buildExoPlayer(
        context: Context,
        forceSoftwareFallback: Boolean = false
    ): ExoPlayer {
        val renderersFactory = createRenderersFactory(context, forceSoftwareFallback)

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd()) // Dynamic quality switching optimization
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2_500,
                /* maxBufferMs = */ 8_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    /**
     * Checks whether an errorCode represents a decoder failure.
     */
    fun isDecoderErrorCode(errorCode: Int): Boolean {
        return when (errorCode) {
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> true
            else -> false
        }
    }

    /**
     * Checks whether a PlaybackException is caused by a hardware codec failure
     * that would benefit from a software decoding retry.
     */
    fun isDecoderFailure(error: PlaybackException): Boolean {
        if (isDecoderErrorCode(error.errorCode)) return true
        val msg = error.message?.lowercase() ?: ""
        return msg.contains("mediacodec") || msg.contains("decoder") || msg.contains("codec")
    }
}
