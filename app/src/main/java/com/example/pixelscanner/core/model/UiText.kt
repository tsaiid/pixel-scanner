package com.example.pixelscanner.core.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed interface UiText {
    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class DynamicString(
        val value: String,
    ) : UiText
}

fun uiTextResource(@StringRes resId: Int, vararg args: Any): UiText {
    return UiText.StringResource(resId = resId, args = args.toList())
}

fun UiText.asString(context: Context): String {
    return when (this) {
        is UiText.StringResource -> context.getString(resId, *args.toTypedArray())
        is UiText.DynamicString -> value
    }
}

@Composable
fun UiText.asString(): String {
    return asString(LocalContext.current)
}
