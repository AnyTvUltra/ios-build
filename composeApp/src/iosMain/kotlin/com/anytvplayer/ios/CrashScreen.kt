package com.anytvplayer.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CrashScreen(
    trail: String?,
    details: String?,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Previous launch did not finish",
            color = Color(0xFFFF5252),
            style = MaterialTheme.typography.titleMedium
        )
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Text("Continue to app")
        }

        Text(
            text = "LAST STAGE REACHED",
            color = Color(0xFFFFD54F),
            fontSize = 12.sp
        )
        Text(
            text = trail ?: "(no stages recorded)",
            color = Color(0xFF80D8FF),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "CRASH DETAILS",
            color = Color(0xFFFFD54F),
            fontSize = 12.sp
        )
        Text(
            text = details ?: "(none captured)",
            color = Color(0xFFE0E0E0),
            fontSize = 10.sp,
            lineHeight = 13.sp
        )
    }
}
