package com.example.pixelscanner.feature.home.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.pixelscanner.R
import com.example.pixelscanner.ui.theme.PixelScannerColorTokens
import com.example.pixelscanner.ui.theme.PixelScannerMotionTokens
import com.example.pixelscanner.ui.theme.PixelScannerSpacingTokens
import com.example.pixelscanner.ui.theme.rememberPixelScannerTypographyTokens

@Composable
fun StartScreen(
    recoverableSession: RecoverableSessionUiState? = null,
    onStartScanClicked: () -> Unit,
    onRestoreRecoverableSession: (String) -> Unit = {},
    onDiscardRecoverableSession: (String) -> Unit = {},
) {
    val typography = rememberPixelScannerTypographyTokens()
    var startAnimation by rememberSaveable { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = PixelScannerMotionTokens.logoScaleIn,
        label = "LogoScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PixelScannerColorTokens.homeSurface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Section: Logo and App Name
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = PixelScannerSpacingTokens.heroOffset),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(id = R.string.start_logo_content_description),
                    modifier = Modifier
                        .size(PixelScannerSpacingTokens.heroLogoSize)
                        .scale(scale)
                )
                Spacer(modifier = Modifier.height(PixelScannerSpacingTokens.lg))
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = typography.display,
                    color = PixelScannerColorTokens.homeOnSurface
                )
                Text(
                    text = stringResource(id = R.string.start_tagline),
                    style = typography.title,
                    color = PixelScannerColorTokens.homeOnSurface.copy(alpha = 0.7f)
                )
            }

            // Bottom Section: Start Button
            Button(
                onClick = onStartScanClicked,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        horizontal = PixelScannerSpacingTokens.xxl,
                        vertical = PixelScannerSpacingTokens.xxxl,
                    )
                .fillMaxWidth()
                .height(PixelScannerSpacingTokens.primaryButtonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PixelScannerColorTokens.homeButtonContainer,
                    contentColor = PixelScannerColorTokens.homeButtonContent
                )
            ) {
                Text(
                    text = stringResource(id = R.string.start_primary_action),
                    style = typography.title,
                )
            }
        }
    }

    if (recoverableSession != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(id = R.string.start_recover_session_title)) },
            text = {
                val statusLine = if (recoverableSession.hasMissingAssets) {
                    stringResource(id = R.string.start_recover_session_missing_assets_message)
                } else {
                    stringResource(
                        id = R.string.start_recover_session_message,
                        recoverableSession.pageCount
                    )
                }
                Text(statusLine)
            },
            confirmButton = {
                TextButton(
                    onClick = { onRestoreRecoverableSession(recoverableSession.sessionId) }
                ) {
                    Text(stringResource(id = R.string.start_recover_session_restore_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onDiscardRecoverableSession(recoverableSession.sessionId) }
                ) {
                    Text(stringResource(id = R.string.start_recover_session_discard_action))
                }
            },
        )
    }
}
