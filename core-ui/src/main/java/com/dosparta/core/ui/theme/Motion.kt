package com.dosparta.core.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * True when the user has switched animations off system-wide.
 *
 * Every animation in the app reads this and falls back to an instant result, so motion never
 * becomes a barrier to using the app.
 */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * Motion tokens for the app.
 *
 * The tokens mirror the Material 3 expressive motion scheme, which is not usable directly because
 * `MotionScheme` and `MaterialExpressiveTheme` are still internal in Material 3 1.4.0. They keep
 * the scheme's central distinction: *spatial* motion (anything that moves or resizes) uses
 * under-damped springs that overshoot slightly, while *effects* (color and alpha) use critically
 * damped springs so a color never overshoots into a wrong value.
 *
 * When [LocalReducedMotion] is set, every spec collapses to [snap] and animated values jump
 * straight to their target.
 */
object TriviaMotion {

    /** Movement, size and position changes at the default pace. */
    @Composable
    @ReadOnlyComposable
    fun <T> spatial(): FiniteAnimationSpec<T> = ifMotion { DefaultSpatial }

    /** Movement that should feel immediate, such as press feedback. */
    @Composable
    @ReadOnlyComposable
    fun <T> spatialFast(): FiniteAnimationSpec<T> = ifMotion { FastSpatial }

    /** Deliberate movement used for reveals and celebratory moments. */
    @Composable
    @ReadOnlyComposable
    fun <T> spatialSlow(): FiniteAnimationSpec<T> = ifMotion { SlowSpatial }

    /** Color and alpha changes at the default pace. */
    @Composable
    @ReadOnlyComposable
    fun <T> effects(): FiniteAnimationSpec<T> = ifMotion { DefaultEffects }

    /** Color and alpha changes that should resolve quickly. */
    @Composable
    @ReadOnlyComposable
    fun <T> effectsFast(): FiniteAnimationSpec<T> = ifMotion { FastEffects }

    /** A bouncy spring for emphasis moments, such as a score landing on its final value. */
    @Composable
    @ReadOnlyComposable
    fun <T> celebratory(): FiniteAnimationSpec<T> = ifMotion { Celebratory }

    /** A linear spec over [durationMillis], collapsing to an instant jump when motion is reduced. */
    @Composable
    @ReadOnlyComposable
    fun <T> linear(durationMillis: Int): FiniteAnimationSpec<T> =
        if (LocalReducedMotion.current) snap() else tween(durationMillis)

    /**
     * Returns [durationMillis], or zero when motion is reduced, for animations driven by an
     * explicit time budget rather than by a spring.
     */
    @Composable
    @ReadOnlyComposable
    fun durationOrInstant(durationMillis: Int): Int =
        if (LocalReducedMotion.current) 0 else durationMillis

    /** Delay before the [index]th element of a staggered entrance starts animating. */
    @Composable
    @ReadOnlyComposable
    fun staggerDelay(index: Int): Int = durationOrInstant(index * STAGGER_STEP_MILLIS)

    @Composable
    @ReadOnlyComposable
    private inline fun <T> ifMotion(spec: () -> FiniteAnimationSpec<*>): FiniteAnimationSpec<T> {
        @Suppress("UNCHECKED_CAST")
        return if (LocalReducedMotion.current) snap() else spec() as FiniteAnimationSpec<T>
    }

    /** Step between staggered sibling entrances. */
    const val STAGGER_STEP_MILLIS = 60

    /** Duration of one shimmer sweep across a loading placeholder. */
    const val SHIMMER_PERIOD_MILLIS = 1_400

    /** Duration of a screen-level enter or exit transition. */
    const val SCREEN_TRANSITION_MILLIS = 350

    private val FastSpatial: FiniteAnimationSpec<Any> =
        spring(dampingRatio = 0.7f, stiffness = 1_400f, visibilityThreshold = null)
    private val DefaultSpatial: FiniteAnimationSpec<Any> =
        spring(dampingRatio = 0.8f, stiffness = 700f, visibilityThreshold = null)
    private val SlowSpatial: FiniteAnimationSpec<Any> =
        spring(dampingRatio = 0.8f, stiffness = 300f, visibilityThreshold = null)
    private val FastEffects: FiniteAnimationSpec<Any> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 3_800f,
        visibilityThreshold = null
    )
    private val DefaultEffects: FiniteAnimationSpec<Any> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1_600f,
        visibilityThreshold = null
    )
    private val Celebratory: FiniteAnimationSpec<Any> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = null
    )
}

/**
 * Resolves whether the system currently has animations disabled.
 *
 * Reads `Settings.Global.ANIMATOR_DURATION_SCALE`; zero means the user turned animations off,
 * commonly via "Remove animations" in accessibility settings or via developer options. Previews
 * always report motion as enabled so animated states stay inspectable.
 */
@Composable
internal fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    return remember(context, isPreview) {
        !isPreview && Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
