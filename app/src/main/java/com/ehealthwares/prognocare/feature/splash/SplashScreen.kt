package com.ehealthwares.prognocare.feature.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ehealthwares.prognocare.R
import com.ehealthwares.prognocare.designsystem.theme.AppThemeColors
import com.ehealthwares.prognocare.designsystem.theme.Primary
import com.ehealthwares.prognocare.designsystem.theme.PrimaryDark
import kotlinx.coroutines.delay

@Composable
fun SplashScreen() {
    var showLogo by remember { mutableStateOf(false) }
    var showBrand by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }
    var showFeatures by remember { mutableStateOf(false) }

    // Logo animation: spring bounce
    val logoScale by animateFloatAsState(
        targetValue = if (showLogo) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "logoScale",
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (showLogo) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "logoAlpha",
    )

    // Trigger animations sequentially
    LaunchedEffect(Unit) {
        showLogo = true
        delay(500)
        showBrand = true
        delay(400)
        showTagline = true
        delay(400)
        showFeatures = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        PrimaryDark,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo with spring animation
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_honeycomb),
                    contentDescription = "PrognoCare Logo",
                    modifier = Modifier.size(100.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand name - slide up + fade
            AnimatedVisibility(
                visible = showBrand,
                enter = fadeIn(tween(500)) + slideInVertically(
                    animationSpec = tween(500),
                    initialOffsetY = { it / 3 },
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PrognoCare",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline - fade in
            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(tween(500)),
            ) {
                Text(
                    text = "Electronic Medical Records",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Normal,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Feature icons - fade in
            AnimatedVisibility(
                visible = showFeatures,
                enter = fadeIn(tween(500)),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FeatureIcon(
                        icon = Icons.Default.LocalHospital,
                        label = "Clinical",
                        color = AppThemeColors.current.kpiBlueLight,
                    )
                    FeatureIcon(
                        icon = Icons.Default.MedicalServices,
                        label = "Records",
                        color = AppThemeColors.current.kpiGreenLight,
                    )
                    FeatureIcon(
                        icon = Icons.Default.Favorite,
                        label = "Health",
                        color = AppThemeColors.current.kpiOrangeLight,
                    )
                    FeatureIcon(
                        icon = Icons.Default.Check,
                        label = "Quality",
                        color = AppThemeColors.current.kpiPurpleLight,
                    )
                }
            }
        }

        // Loading indicator at bottom
        AnimatedVisibility(
            visible = showTagline,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(300)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 80.dp),
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White.copy(alpha = 0.7f),
                    strokeWidth = 2.5.dp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading your health data...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun FeatureIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
    }
}
