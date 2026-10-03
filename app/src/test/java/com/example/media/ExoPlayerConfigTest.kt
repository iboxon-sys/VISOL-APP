package com.example.media

import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExoPlayerConfigTest {

    @Test
    fun testSupportedCodecsIncludeH264AndAAC() {
        assertTrue(
            "Supported video MIME types must include H.264 / AVC",
            ExoPlayerConfig.SUPPORTED_VIDEO_MIMES.contains(MimeTypes.VIDEO_H264)
        )
        assertTrue(
            "Supported audio MIME types must include AAC",
            ExoPlayerConfig.SUPPORTED_AUDIO_MIMES.contains(MimeTypes.AUDIO_AAC)
        )
    }

    @Test
    fun testSoftwareDecoderDetection() {
        assertTrue(ExoPlayerConfig.isSoftwareDecoder("c2.android.avc.decoder"))
        assertTrue(ExoPlayerConfig.isSoftwareDecoder("OMX.google.h264.decoder"))
        assertTrue(ExoPlayerConfig.isSoftwareDecoder("c2.android.aac.decoder"))
        assertTrue(ExoPlayerConfig.isSoftwareDecoder("OMX.google.aac.decoder"))
        assertTrue(ExoPlayerConfig.isSoftwareDecoder("libffmpeg_decoder"))

        // Hardware decoders should return false
        assertFalse(ExoPlayerConfig.isSoftwareDecoder("c2.qti.avc.decoder"))
        assertFalse(ExoPlayerConfig.isSoftwareDecoder("OMX.qcom.video.decoder.avc"))
        assertFalse(ExoPlayerConfig.isSoftwareDecoder("OMX.Exynos.avc.dec"))
    }

    @Test
    fun testDecoderErrorCodeDetection() {
        assertTrue(ExoPlayerConfig.isDecoderErrorCode(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED))
        assertTrue(ExoPlayerConfig.isDecoderErrorCode(PlaybackException.ERROR_CODE_DECODING_FAILED))
        assertTrue(ExoPlayerConfig.isDecoderErrorCode(PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED))
        assertTrue(ExoPlayerConfig.isDecoderErrorCode(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED))
        assertFalse(ExoPlayerConfig.isDecoderErrorCode(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT))
        assertFalse(ExoPlayerConfig.isDecoderErrorCode(PlaybackException.ERROR_CODE_REMOTE_ERROR))
    }
}
