package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Visol.uz", appName)
  }

  @Test
  fun `verify uzbekistan regions list loaded`() {
    val regions = com.example.model.UzbekistanData.regions
    assertEquals(14, regions.size)
  }

  @Test
  fun `verify fcm manager initializes safely without throwing IllegalStateException`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.FcmManager.retrieveAndLogFcmToken(context)
    val token = com.example.data.UserRepository(context).fcmToken.value
    org.junit.Assert.assertNotNull(token)
  }
}
