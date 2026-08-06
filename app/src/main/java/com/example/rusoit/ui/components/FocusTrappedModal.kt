@file:OptIn(ExperimentalComposeUiApi::class)
package com.example.rusoit.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Modal a pantalla completa que atrapa el foco del D-pad.
 * El menú / contenido detrás no reciben foco hasta pulsar Cerrar.
 * Back no cierra el modal.
 */
@Composable
fun FocusTrappedModal(
    scrimAlpha: Float = 0.9f,
    contentAlignment: Alignment = Alignment.Center,
    initialFocusRequester: FocusRequester? = null,
    content: @Composable BoxScope.() -> Unit
) {
    BackHandler(enabled = true) { }

    Dialog(
        onDismissRequest = { /* solo Cerrar */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val trapFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            (initialFocusRequester ?: trapFocus).requestFocus()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .focusRequester(trapFocus)
                .focusProperties {
                    // Impide que el D-pad salga del Dialog hacia el dashboard
                    exit = { FocusRequester.Cancel }
                },
            contentAlignment = contentAlignment,
            content = content
        )
    }
}
