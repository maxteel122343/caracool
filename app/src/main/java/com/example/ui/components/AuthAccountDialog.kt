package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.service.SupabaseUser
import com.example.ui.theme.NaturalBorderLight
import com.example.ui.theme.NaturalBorderSubtle
import com.example.ui.theme.NaturalTextMuted
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AuthAccountDialog(
    viewModel: MainViewModel,
    currentUser: SupabaseUser?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsState()
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isCuMode = settings.isCaraDeKoolMode
    val profilePhotoUrl = currentUser?.photoUrl?.toString() 
        ?: settings.userProfilePhotoUri 
        ?: settings.userPhotoUri

    val displayName = currentUser?.displayName 
        ?: (if (isCuMode && (settings.userName == "Você (Cara de Paçoca)" || settings.userName.isBlank())) "Você (Cara de Cu)" else settings.userName.ifBlank { null })
        ?: currentUser?.email?.substringBefore("@") 
        ?: "Usuário"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .testTag("auth_account_dialog"),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isCuMode) Color(0xFFFCE4EC) else Color(0xFFFFE0B2),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (isCuMode) Color(0xFFE91E63) else Color(0xFFE65100),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (currentUser != null) "Conta Conectada" else "Modo de Armazenamento",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCuMode) Color(0xFF880E4F) else NaturalTextPrimary
                            )
                            Text(
                                text = if (currentUser != null) "Sincronização Ativa" else "Modo Convidado ativo",
                                fontSize = 11.sp,
                                color = if (currentUser != null) Color(0xFF2E7D32) else (if (isCuMode) Color(0xFFAD1457) else NaturalTextSecondary),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = if (isCuMode) Color(0xFFAD1457) else NaturalTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (currentUser != null) {
                    // --- LOGGED IN VIEW (Clean, shows photo, no internal DB terms, no sync button) ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // User Profile Avatar
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.5.dp,
                                        if (isCuMode) Color(0xFFF06292) else Color(0xFFFFB74D),
                                        CircleShape
                                    )
                                    .background(if (isCuMode) Color(0xFFFCE4EC) else Color(0xFFFFE0B2)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!profilePhotoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = profilePhotoUrl,
                                        contentDescription = "Foto de perfil",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = settings.userAvatarEmoji.ifBlank { if (isCuMode) "👾" else "🥜" },
                                        fontSize = 34.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = displayName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )

                            if (!currentUser.email.isNullOrBlank()) {
                                Text(
                                    text = currentUser.email ?: "",
                                    fontSize = 13.sp,
                                    color = Color(0xFF2E7D32),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xFFC8E6C9), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF1B5E20),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sincronização Ativa",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign Out Button
                    OutlinedButton(
                        onClick = {
                            viewModel.signOut()
                            Toast.makeText(context, "Desconectado. Retornando ao Modo Convidado.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("sign_out_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sair da Conta (Modo Convidado)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // --- GUEST / LOGIN VIEW ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCuMode) Color(0xFFFFF0F5) else Color(0xFFFFF8E1)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isCuMode) Color(0xFFF8BBD0) else Color(0xFFFFECB3)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "📱 Modo Convidado Ativo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCuMode) Color(0xFFE91E63) else Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Seus dados estão salvos com segurança no seu celular. Conecte sua conta para salvar suas fotos e desbloqueios na nuvem!",
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                color = if (isCuMode) Color(0xFF880E4F) else Color(0xFF5D4037)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Google Sign In Button
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            viewModel.signInWithGoogle(
                                context = context,
                                onComplete = { success, msg ->
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(context, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        enabled = !isLoading
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("G", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Continuar com Google", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider "ou por e-mail"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = if (isCuMode) Color(0xFFF8BBD0) else NaturalBorderSubtle)
                        Text(
                            text = " OU POR E-MAIL ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCuMode) Color(0xFFAD1457) else NaturalTextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = if (isCuMode) Color(0xFFF8BBD0) else NaturalBorderSubtle)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Email Field
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; errorMessage = null },
                        label = { Text("E-mail", fontSize = 13.sp) },
                        placeholder = { Text("seu@email.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = if (isCuMode) Color(0xFFAD1457) else NaturalTextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isCuMode) Color(0xFFE91E63) else Color(0xFFE65100),
                            unfocusedBorderColor = if (isCuMode) Color(0xFFF8BBD0) else NaturalBorderLight
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; errorMessage = null },
                        label = { Text("Senha", fontSize = 13.sp) },
                        placeholder = { Text("Mínimo 6 caracteres") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (isCuMode) Color(0xFFAD1457) else NaturalTextSecondary) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = if (isCuMode) Color(0xFFAD1457) else NaturalTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isCuMode) Color(0xFFE91E63) else Color(0xFFE65100),
                            unfocusedBorderColor = if (isCuMode) Color(0xFFF8BBD0) else NaturalBorderLight
                        )
                    )

                    // Error message if any
                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Button (Login / Cadastrar)
                    Button(
                        onClick = {
                            if (emailInput.isBlank() || passwordInput.isBlank()) {
                                errorMessage = "Preencha e-mail e senha."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            if (isSignUpMode) {
                                viewModel.signUpWithEmail(emailInput.trim(), passwordInput) { success, msg ->
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            } else {
                                viewModel.signInWithEmail(emailInput.trim(), passwordInput) { success, msg ->
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(context, "Login realizado!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCuMode) Color(0xFFE91E63) else Color(0xFFE65100)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isSignUpMode) "Criar Minha Conta" else "Entrar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Switch Mode Toggle (Login <-> Cadastro)
                    TextButton(
                        onClick = {
                            isSignUpMode = !isSignUpMode
                            errorMessage = null
                        }
                    ) {
                        Text(
                            text = if (isSignUpMode) "Já tem uma conta? Entrar" else "Não tem conta? Criar nova conta",
                            fontSize = 12.5.sp,
                            color = if (isCuMode) Color(0xFFE91E63) else Color(0xFFE65100),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Continue as guest button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("continue_as_guest_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isCuMode) Color(0xFFF8BBD0) else NaturalBorderLight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isCuMode) Color(0xFFAD1457) else NaturalTextSecondary
                        )
                    ) {
                        Text("Continuar no Modo Convidado", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
