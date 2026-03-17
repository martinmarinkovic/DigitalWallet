package com.threemdroid.digitalwallet.feature.fullscreencode

import android.view.Window

interface BrightnessController {
    fun currentBrightness(): Float

    fun setBrightness(brightness: Float)
}

class WindowBrightnessController(
    private val window: Window
) : BrightnessController {
    override fun currentBrightness(): Float = window.attributes.screenBrightness

    override fun setBrightness(brightness: Float) {
        val updatedAttributes = window.attributes
        updatedAttributes.screenBrightness = brightness
        window.attributes = updatedAttributes
    }
}

class NoOpBrightnessController : BrightnessController {
    override fun currentBrightness(): Float = 0f

    override fun setBrightness(brightness: Float) = Unit
}

class FullscreenBrightnessManager(
    private val brightnessController: BrightnessController
) {
    private var previousBrightness: Float? = null

    fun onVisible(shouldMaximizeBrightness: Boolean) {
        if (!shouldMaximizeBrightness || previousBrightness != null) {
            return
        }

        previousBrightness = brightnessController.currentBrightness()
        brightnessController.setBrightness(1f)
    }

    fun onHidden() {
        val brightnessToRestore = previousBrightness ?: return
        brightnessController.setBrightness(brightnessToRestore)
        previousBrightness = null
    }
}
