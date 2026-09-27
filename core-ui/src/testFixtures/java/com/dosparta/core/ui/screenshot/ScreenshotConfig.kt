package com.dosparta.core.ui.screenshot

import android.os.Build

/**
 * SDK level used for screenshot rendering.
 *
 * Robolectric's native graphics mode, which Roborazzi requires, is only supported from SDK 26.
 * Android 13 is chosen because its `android-all-instrumented` artifact is already part of the
 * local Robolectric cache, so screenshot runs do not pull a new runtime.
 */
const val SCREENSHOT_SDK = Build.VERSION_CODES.TIRAMISU

/**
 * Fixed device configuration for screenshot rendering: a 411x891dp, 420dpi phone.
 *
 * Pinning this keeps golden image dimensions stable regardless of the default device Robolectric
 * happens to use.
 */
const val PIXEL_5_QUALIFIERS =
    "w411dp-h891dp-normal-long-notround-any-420dpi-keyshidden-nonav"
