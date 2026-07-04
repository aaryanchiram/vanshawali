package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SecurityLockScreen(
    savedPin: String, // Empty if not set
    onPinCreated: (String) -> Unit,
    onUnlockSuccess: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isSetupMode = savedPin.isEmpty()

    var enteredPin by remember { mutableStateOf("") }
    var setupPhrase by remember { mutableStateOf("नया सुरक्षा पिन बनाएँ\n(Create Your Security PIN)") }
    var errorMessage by remember { mutableStateOf("") }
    var tempFirstPin by remember { mutableStateOf("") } // For setup verification

    val totalDigits = 4

    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF040605), Color(0xFF14241B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE9F0E8), Color(0xFFF9FAF7)))
    }

    // Biometrics simulated state
    var showBiometricSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(showBiometricSuccess) {
        if (showBiometricSuccess) {
            delay(1000)
            onUnlockSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSetupMode) Icons.Filled.Security else Icons.Filled.Lock,
            contentDescription = "Lock",
            tint = if (isDark) Color(0xFFBAE16C) else Color(0xFF558B2F),
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "VanshVriksh (वंशवृक्ष)",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color(0xFF1B3B0C)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isSetupMode) setupPhrase else "सुरक्षित पारिवारिक डेटा के लिए अपनी पिन दर्ज करें\n(Enter Security PIN to unlock)",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = if (isDark) Color.LightGray else Color.DarkGray
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Bullet dots showing PIN entries
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            for (i in 1..totalDigits) {
                val isActive = enteredPin.length >= i
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) {
                                if (isDark) Color(0xFFBAE16C) else Color(0xFF558B2F)
                            } else {
                                if (isDark) Color(0xFF333333) else Color(0xFFCCCCCC)
                            }
                        )
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Numeric Keypad Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Biometric", "0", "Backspace")
            )

            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    row.forEach { item ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f),
                            contentAlignment = Alignment.Center
                        ) {
                            when (item) {
                                "Biometric" -> {
                                    if (!isSetupMode) {
                                        // Draw Fingerprint trigger
                                        IconButton(
                                            onClick = {
                                                showBiometricSuccess = true
                                                enteredPin = "••••"
                                            },
                                            modifier = Modifier
                                                .size(64.dp)
                                                .background(
                                                    if (isDark) Color(0xFF1E3326) else Color(0xFFE2EDE4),
                                                    CircleShape
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isDark) Color(0xFFBAE16C) else Color(0xFF558B2F),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Fingerprint,
                                                contentDescription = "Simulate Fingerprint Scan",
                                                tint = if (isDark) Color(0xFFBAE16C) else Color(0xFF558B2F),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                                "Backspace" -> {
                                    IconButton(
                                        onClick = {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        },
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = if (isDark) Color.LightGray else Color.DarkGray
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDark) Color(0xFF1B1B1B) else Color(0xFFF1F1F1)
                                            )
                                            .clickable {
                                                if (enteredPin.length < totalDigits) {
                                                    enteredPin += item
                                                    errorMessage = ""

                                                    // Standard authentication verify logic
                                                    if (enteredPin.length == totalDigits) {
                                                        if (isSetupMode) {
                                                            if (tempFirstPin.isEmpty()) {
                                                                tempFirstPin = enteredPin
                                                                enteredPin = ""
                                                                setupPhrase = "पिन की पुष्टि करें (Confirm Security PIN)"
                                                            } else {
                                                                if (enteredPin == tempFirstPin) {
                                                                    onPinCreated(enteredPin)
                                                                } else {
                                                                    errorMessage = "पिन मेल नहीं खाती! फिर से दर्ज करें।"
                                                                    enteredPin = ""
                                                                    tempFirstPin = ""
                                                                    setupPhrase = "नया सुरक्षा पिन बनाएँ"
                                                                }
                                                            }
                                                        } else {
                                                            if (enteredPin == savedPin) {
                                                                onUnlockSuccess()
                                                            } else {
                                                                errorMessage = "गलत पिन! कृपया पुन: प्रयास करें।"
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showBiometricSuccess,
            enter = fadeIn() + expandVertically()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .border(2.dp, Color(0xFF2E7D32), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = "Success",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "बायोमेट्रिक प्रमाणीकरण सफल!\n(Biometric login successful!)",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
