package com.taxeca.calculator.ui.utils

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun isAnimationDisabled(): Boolean {
    val context = LocalContext.current
    return !isAnimationEnabled(context)
}

private fun isAnimationEnabled(context: Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        scale > 0f
    } catch (e: Exception) {
        true
    }
}

@Composable
fun getDefaultEnterTransition(): EnterTransition {
    return if (isAnimationDisabled()) {
        EnterTransition.None
    } else {
        fadeIn() + expandVertically()
    }
}

@Composable
fun getDefaultExitTransition(): ExitTransition {
    return if (isAnimationDisabled()) {
        ExitTransition.None
    } else {
        fadeOut() + shrinkVertically()
    }
}
