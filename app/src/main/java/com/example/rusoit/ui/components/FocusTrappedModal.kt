@file:OptIn(ExperimentalComposeUiApi::class)
package com.example.rusoit.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Modal a pantalla completa que atrapa el foco del D-pad.
 * ESC / Back cierran siempre vía [onDismiss] (PC + TV).
 */
@Composable
fun FocusTrappedModal(
    onDismiss: () -> Unit,
    scrimAlpha: Float = 0.9f,
    contentAlignment: Alignment = Alignment.Center,
    initialFocusRequester: FocusRequester? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val dismiss by rememberUpdatedState(onDismiss)

    // TV / Activity back
    BackHandler(enabled = true) { dismiss() }

    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            // Lo manejamos nosotros con BackHandler + teclas para evitar
            // el estado "modal visible pero sin foco" al pulsar ESC en PC.
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val trapFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            runCatching {
                (initialFocusRequester ?: trapFocus).requestFocus()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .focusRequester(trapFocus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val isDismissKey =
                        event.key == Key.Escape || event.key == Key.Back
                    if (event.type == KeyEventType.KeyDown && isDismissKey) {
                        dismiss()
                        true
                    } else {
                        false
                    }
                }
                .focusProperties {
                    // Impide que el D-pad salga al dashboard (que está canFocus=false)
                    exit = { FocusRequester.Cancel }
                },
            contentAlignment = contentAlignment,
            content = content
        )
    }
}
