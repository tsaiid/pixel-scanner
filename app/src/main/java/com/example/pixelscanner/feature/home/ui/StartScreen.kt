package com.example.pixelscanner.feature.home.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pixelscanner.R

@Composable
fun StartScreen(
    recoverableSession: RecoverableSessionUiState? = null,
    onStartScanClicked: () -> Unit,
    onRestoreRecoverableSession: (String) -> Unit = {},
    onDiscardRecoverableSession: (String) -> Unit = {},
) {
    val brandPurple = Color(0xFF541765) // Updated to match icon background #541765
    var startAnimation by rememberSaveable { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = brandPurple
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
                    .padding(bottom = 80.dp), // Slightly adjusted offset
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(id = R.string.start_logo_content_description),
                    modifier = Modifier
                        .size(280.dp) // Increased from 192dp
                        .scale(scale)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.displayMedium, // Larger typography
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = stringResource(id = R.string.start_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Bottom Section: Start Button
            Button(
                onClick = onStartScanClicked,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 48.dp)
                .fillMaxWidth()
                .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = brandPurple
                )
            ) {
                Text(
                    text = stringResource(id = R.string.start_primary_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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
