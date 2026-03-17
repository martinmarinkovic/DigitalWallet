package com.threemdroid.digitalwallet.feature.fullscreencode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenBrightnessManagerTest {
    @Test
    fun onVisible_whenEnabled_setsMaxBrightnessAndRestoresPreviousValueOnHidden() {
        val controller = FakeBrightnessController(initialBrightness = 0.42f)
        val manager = FullscreenBrightnessManager(controller)

        manager.onVisible(shouldMaximizeBrightness = true)
        manager.onHidden()

        assertEquals(listOf(1f, 0.42f), controller.requestedBrightnessValues)
    }

    @Test
    fun onVisible_whenPreviousBrightnessIsSystemDefault_restoresThatStateOnHidden() {
        val controller = FakeBrightnessController(initialBrightness = -1f)
        val manager = FullscreenBrightnessManager(controller)

        manager.onVisible(shouldMaximizeBrightness = true)
        manager.onHidden()

        assertEquals(listOf(1f, -1f), controller.requestedBrightnessValues)
    }

    @Test
    fun onVisible_whenDisabled_doesNotRequestBrightnessChanges() {
        val controller = FakeBrightnessController(initialBrightness = 0.6f)
        val manager = FullscreenBrightnessManager(controller)

        manager.onVisible(shouldMaximizeBrightness = false)
        manager.onHidden()

        assertTrue(controller.requestedBrightnessValues.isEmpty())
    }

    @Test
    fun onVisible_onlyMaximizesOnceUntilHidden() {
        val controller = FakeBrightnessController(initialBrightness = 0.25f)
        val manager = FullscreenBrightnessManager(controller)

        manager.onVisible(shouldMaximizeBrightness = true)
        manager.onVisible(shouldMaximizeBrightness = true)
        manager.onHidden()

        assertEquals(listOf(1f, 0.25f), controller.requestedBrightnessValues)
    }

    private class FakeBrightnessController(
        initialBrightness: Float
    ) : BrightnessController {
        private var brightness: Float = initialBrightness

        val requestedBrightnessValues = mutableListOf<Float>()

        override fun currentBrightness(): Float = brightness

        override fun setBrightness(brightness: Float) {
            requestedBrightnessValues += brightness
            this.brightness = brightness
        }
    }
}
