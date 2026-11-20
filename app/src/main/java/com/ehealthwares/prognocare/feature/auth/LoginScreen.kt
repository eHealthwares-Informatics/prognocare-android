package com.ehealthwares.prognocare.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.annotation.DrawableRes
import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.ehealthwares.prognocare.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.facebook.login.LoginManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.ehealthwares.prognocare.R
import com.ehealthwares.prognocare.data.config.AppConfig
import com.ehealthwares.prognocare.data.config.ServerEnvironment
import com.ehealthwares.prognocare.designsystem.theme.AppThemeColors
import com.ehealthwares.prognocare.designsystem.theme.Primary
import com.ehealthwares.prognocare.designsystem.theme.PrimaryDark
import com.ehealthwares.prognocare.designsystem.theme.Spacing
import com.ehealthwares.prognocare.feature.settings.DiscoveryState
import com.ehealthwares.prognocare.feature.settings.SaveResult
import com.ehealthwares.prognocare.feature.settings.ServerConfigContent
import com.ehealthwares.prognocare.feature.settings.SettingsViewModel
import com.ehealthwares.prognocare.feature.settings.TapCounter
import com.ehealthwares.prognocare.navigation.UserRole

@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole) -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading by loginViewModel.state.collectAsState()
    var selectedRole by remember { mutableStateOf(UserRole.Doctor) }
    var configRevealed by remember { mutableStateOf(false) }
    var showConfigSheet by remember { mutableStateOf(false) }
    var tapProgress by remember { mutableStateOf(0) }
    val tapCounter = remember { TapCounter() }
    val config by settingsViewModel.config.collectAsState()
    val isVerifying by settingsViewModel.isVerifying.collectAsState()
    val saveResult by settingsViewModel.saveResult.collectAsState()
    val discovery by settingsViewModel.discovery.collectAsState()
    var loginError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var googleBusy by remember { mutableStateOf(false) }
    var facebookBusy by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        when (val s = isLoading) {
            is LoginState.Success -> {
                onLoginSuccess(s.role)
                loginViewModel.reset()
            }
            is LoginState.Error -> loginError = s.message
            else -> Unit
        }
    }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // ── Header / Branding ───────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            PrimaryDark,
                        )
                    )
                )
                .clickable {
                    tapCounter.onTap()
                    tapProgress = tapCounter.count
                    configRevealed = tapCounter.revealed
                    if (tapCounter.revealed) showConfigSheet = true
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Logo with white background
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_honeycomb),
                        contentDescription = "eHealthWares Logo",
                        modifier = Modifier.size(60.dp),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.base))
                Text(
                    text = "PrognoCare",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Electronic Medical Records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                // Feature pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    FeaturePill(text = "Secure", color = AppThemeColors.current.kpiGreenLight)
                    FeaturePill(text = "Fast", color = AppThemeColors.current.kpiBlueLight)
                    FeaturePill(text = "Reliable", color = AppThemeColors.current.kpiOrangeLight)
                }
                // Easter-egg progress hint (after 3+ taps of 10)
                if (!configRevealed && tapProgress >= 3) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = if (tapProgress < 10) {
                            "${10 - tapProgress} more tap${if (10 - tapProgress == 1) "" else "s"} to unlock server config…"
                        } else {
                            "Unlocking…"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }

        // ── Login Form ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl, vertical = Spacing.base),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Outlined.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                singleLine = true,
                shape = RoundedCornerShape(Spacing.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        loginError = null
                        loginViewModel.login(email, password)
                    },
                ),
                singleLine = true,
                shape = RoundedCornerShape(Spacing.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

                TextButton(
                onClick = { /* TODO: forgot password */ },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = "Forgot password?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (loginError != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = loginError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Role Selector (Demo) ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Select role (demo)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${UserRole.entries.size} roles available",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))

            RoleDropdown(
                selectedRole = selectedRole,
                onRoleSelected = { selectedRole = it },
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Login Button ─────────────────────────────────
            Button(
                onClick = {
                    loginError = null
                    loginViewModel.login(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isLoading !is LoginState.Loading,
                shape = RoundedCornerShape(Spacing.md),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
            ) {
                AnimatedVisibility(
                    visible = isLoading is LoginState.Loading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
                AnimatedVisibility(
                    visible = isLoading !is LoginState.Loading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Sign up link ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                TextButton(onClick = { /* TODO: sign up */ }) {
                    Text(
                        text = "Sign Up",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // ── Divider with "or" ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "  or continue with  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Social login buttons ─────────────────────────
            val socialContext = LocalContext.current
            val socialScope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(socialContext) }
            var socialBusy by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SocialLoginButton(
                    text = "Google",
                    iconRes = R.drawable.ic_google,
                    enabled = !socialBusy,
                    onClick = {
                        if (socialBusy) return@SocialLoginButton
                        socialBusy = true
                        socialScope.launch {
                            try {
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    // Audience of the returned ID token — the
                                    // backend verifies `aud` against this value.
                                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                    .setFilterByAuthorizedAccounts(false)
                                    .build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()
                                val result = runCatching {
                                    credentialManager.getCredential(socialContext, request)
                                }.getOrElse { e ->
                                    // Derive flow can report no candidate even
                                    // when accounts exist; fall back to the
                                    // explicit button flow (own account chooser).
                                    if (e is NoCredentialException) {
                                        val explicit = GetCredentialRequest.Builder()
                                            .addCredentialOption(
                                                GetSignInWithGoogleOption.Builder(
                                                    BuildConfig.GOOGLE_WEB_CLIENT_ID,
                                                ).build(),
                                            )
                                            .build()
                                        credentialManager.getCredential(socialContext, explicit)
                                    } else {
                                        throw e
                                    }
                                }
                                val credential = result.credential
                                if (credential is GoogleIdTokenCredential) {
                                    loginViewModel.loginWithGoogle(credential.idToken)
                                } else {
                                    loginError = "Unexpected Google credential type"
                                }
                            } catch (_: GetCredentialCancellationException) {
                                // User closed the account sheet — not an error.
                            } catch (_: NoCredentialException) {
                                loginError =
                                    "No Google account available. Sign in to a Google " +
                                        "account on this phone and try again."
                            } catch (e: GetCredentialException) {
                                loginError =
                                    "Google sign-in unavailable — check Play Services (${e.type})"
                            } finally {
                                socialBusy = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                SocialLoginButton(
                    text = "Facebook",
                    iconRes = R.drawable.ic_facebook,
                    enabled = !socialBusy,
                    onClick = {
                        if (socialBusy) return@SocialLoginButton
                        if (BuildConfig.FACEBOOK_APP_ID.isBlank()) {
                            loginError = "Facebook sign-in not configured in this build"
                            return@SocialLoginButton
                        }
                        val activity = socialContext as? Activity
                        if (activity == null) {
                            loginError = "Facebook sign-in unavailable"
                            return@SocialLoginButton
                        }
                        socialBusy = true
                        FacebookAuthBridge.onFacebookResult = { token, cancelled, error ->
                            when {
                                cancelled -> Unit
                                error != null -> loginError = error
                                token != null -> loginViewModel.loginWithFacebook(token)
                            }
                            socialBusy = false
                        }
                        LoginManager.getInstance()
                            .logInWithReadPermissions(
                                activity,
                                listOf("public_profile", "email"),
                            )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showConfigSheet) {
        ServerConfigSheet(
            config = config,
            isVerifying = isVerifying,
            saveResult = saveResult,
            discovery = discovery,
            onSave = { emr, conv, concepts, rxsoft, channel, env ->
                settingsViewModel.saveConfig(emr, conv, concepts, rxsoft, channel, env)
            },
            onReset = { settingsViewModel.resetToDefaults() },
            onDiscover = settingsViewModel::discoverServers,
            onDismissDiscovery = settingsViewModel::dismissDiscovery,
            onDismiss = { showConfigSheet = false },
        )
    }
}

@Composable
private fun FeaturePill(
    text: String,
    color: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.3f))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(Spacing.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(if (enabled) 1f else 0.5f),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
                    .copy(alpha = if (enabled) 1f else 0.5f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedRole.displayName,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Role") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(Spacing.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            UserRole.entries.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role.displayName) },
                    onClick = {
                        onRoleSelected(role)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Easter-egg server-config panel. Shown as a modal bottom sheet so the hidden
 * 10-tap unlock always appears over the login screen regardless of how much
 * vertical space the form uses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerConfigSheet(
    config: AppConfig,
    isVerifying: Boolean,
    saveResult: SaveResult?,
    discovery: DiscoveryState,
    onSave: (String, String, String, String, String, ServerEnvironment) -> Unit,
    onReset: () -> Unit,
    onDiscover: () -> Unit,
    onDismissDiscovery: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.xl),
        ) {
            Text(
                text = "Server configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            // Current server URLs (read-only) above the editable fields
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Spacing.md),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = "Current server URLs",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "EMR: ${config.emrBaseUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Chat: ${config.conversationBaseUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Concepts: ${config.conceptsBaseUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "rxsoft: ${config.rxsoftBaseUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Channel: ${config.webChannelCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            ServerConfigContent(
                config = config,
                onSave = onSave,
                onReset = onReset,
                isVerifying = isVerifying,
                saveResult = saveResult,
                discovery = discovery,
                onDiscover = onDiscover,
                onDismissDiscovery = onDismissDiscovery,
            )
        }
    }
}
