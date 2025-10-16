package com.example.anglecaring.ui.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.anglecaring.R
import java.io.ByteArrayOutputStream
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditPage(
    onNavigateBack: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var passwordVisibility by remember { 
        mutableStateOf(PasswordVisibilityState()) 
    }
    var activeTab by remember { mutableStateOf(ProfileTab.PROFILE) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        android.util.Log.d("ProfileEditPage", "Image picker result: $uri")
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                    val imageBytes = byteArrayOutputStream.toByteArray()
                    viewModel.selectImage(uri, imageBytes)
                    android.util.Log.d("ProfileEditPage", "Image selected successfully")
                } else {
                    Toast.makeText(context, "無法讀取圖片，請選擇其他圖片", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "圖片處理失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Show success message as Toast
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
        }
    }

    // Show error message as Toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            kotlinx.coroutines.delay(100) // 短暫延遲避免快速重複顯示
            viewModel.clearAllMessages()
        }
    }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("個人資料設定", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_back_24dp),
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = activeTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileTab.values().forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab.title) }
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (activeTab) {
                    ProfileTab.PROFILE -> {
                        ProfileEditContent(
                            uiState = uiState,
                            userName = viewModel.userName,
                            email = viewModel.email,
                            selectedImageUri = viewModel.selectedImageUri,
                            onUserNameChange = viewModel::updateUserName,
                            onEmailChange = viewModel::updateEmail,
                            onImageClick = { imagePickerLauncher.launch("image/*") },
                            onRemoveImage = viewModel::removeSelectedImage,
                            onUpdateProfile = viewModel::updateProfile,
                            focusManager = focusManager
                        )
                    }
                    ProfileTab.PASSWORD -> {
                        PasswordChangeContent(
                            uiState = uiState,
                            currentPassword = viewModel.currentPassword,
                            newPassword = viewModel.newPassword,
                            confirmPassword = viewModel.confirmPassword,
                            passwordVisibility = passwordVisibility,
                            onCurrentPasswordChange = viewModel::updateCurrentPassword,
                            onNewPasswordChange = viewModel::updateNewPassword,
                            onConfirmPasswordChange = viewModel::updateConfirmPassword,
                            onPasswordVisibilityChange = { passwordVisibility = it },
                            onUpdatePassword = viewModel::updatePassword,
                            focusManager = focusManager
                        )
                    }
                }

                // 移除了原本的錯誤/成功訊息顯示區域，因為現在使用 Toast
            }
        }
    }
}

@Composable
private fun ProfileEditContent(
    uiState: ProfileEditUiState,
    userName: String,
    email: String,
    selectedImageUri: Uri?,
    onUserNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onImageClick: () -> Unit,
    onRemoveImage: () -> Unit,
    onUpdateProfile: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    // 追蹤原始值以實現增量更新
    val originalUserName = uiState.currentUser?.userName ?: ""
    val originalEmail = uiState.currentUser?.email ?: ""
    val originalImageBytes = uiState.currentUser?.userImage
    
    // 檢查是否有變更
    val hasUserNameChanged = userName != originalUserName
    val hasEmailChanged = email != originalEmail
    val hasImageChanged = selectedImageUri != null
    val hasAnyChanges = hasUserNameChanged || hasEmailChanged || hasImageChanged
    
    // 表單驗證狀態
    var validationState by remember { 
        mutableStateOf(FormValidationState()) 
    }
    
    // 實時驗證
    LaunchedEffect(userName, email) {
        validationState = FormValidationState(
            userNameError = validateUserName(userName),
            emailError = validateEmail(email),
            isFormValid = validateUserName(userName) == null && validateEmail(email) == null
        )
    }
    // Profile Image Section
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        ProfileImage(
            selectedImageUri = selectedImageUri,
            userImageBytes = uiState.currentUser?.userImage,
            onImageClick = onImageClick
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "點擊更換頭像",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    // User Name Field
    ValidatedTextField(
        value = userName,
        onValueChange = onUserNameChange,
        label = "用戶名",
        fieldType = TextFieldType.USERNAME,
        errorMessage = validationState.userNameError,
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.person_24dp),
                contentDescription = "用戶名"
            )
        },
        imeAction = ImeAction.Next,
        onKeyboardAction = { focusManager.moveFocus(FocusDirection.Down) }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Email Field
    ValidatedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = "電子郵件",
        fieldType = TextFieldType.EMAIL,
        errorMessage = validationState.emailError,
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.mail_24dp),
                contentDescription = "電子郵件"
            )
        },
        imeAction = ImeAction.Done,
        onKeyboardAction = { focusManager.clearFocus() }
    )

    Spacer(modifier = Modifier.height(24.dp))


    // Update Button
    Button(
        onClick = onUpdateProfile,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = !uiState.isLoading && hasAnyChanges && validationState.isFormValid,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (uiState.isLoading && !uiState.isPasswordUpdateSuccess) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "更新中...",
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Text(
                text = "更改個人資料",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PasswordChangeContent(
    uiState: ProfileEditUiState,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    passwordVisibility: PasswordVisibilityState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityChange: (PasswordVisibilityState) -> Unit,
    onUpdatePassword: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Text(
        text = "更改密碼",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 24.dp)
    )

    // Current Password
    PasswordField(
        value = currentPassword,
        onValueChange = onCurrentPasswordChange,
        label = "目前密碼",
        showPassword = passwordVisibility.showCurrentPassword,
        onShowPasswordToggle = { onPasswordVisibilityChange(passwordVisibility.toggleCurrentPassword()) },
        imeAction = ImeAction.Next,
        onKeyboardAction = { focusManager.moveFocus(FocusDirection.Down) }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // New Password
    PasswordField(
        value = newPassword,
        onValueChange = onNewPasswordChange,
        label = "新密碼",
        showPassword = passwordVisibility.showNewPassword,
        onShowPasswordToggle = { onPasswordVisibilityChange(passwordVisibility.toggleNewPassword()) },
        imeAction = ImeAction.Next,
        onKeyboardAction = { focusManager.moveFocus(FocusDirection.Down) }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Confirm Password
    PasswordField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = "確認新密碼",
        showPassword = passwordVisibility.showConfirmPassword,
        onShowPasswordToggle = { onPasswordVisibilityChange(passwordVisibility.toggleConfirmPassword()) },
        imeAction = ImeAction.Done,
        onKeyboardAction = { focusManager.clearFocus() }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 密碼驗證狀態（檢查所有要求）
    val passwordChecks = listOf(
        PasswordCheck(
            text = "至少6個字符",
            isMet = newPassword.length >= 6
        ),
        PasswordCheck(
            text = "不能與目前密碼相同",
            isMet = newPassword.isNotEmpty() && newPassword != currentPassword
        ),
        PasswordCheck(
            text = "包含大小寫字母",
            isMet = newPassword.isNotEmpty() && 
                    newPassword.any { it.isUpperCase() } && 
                    newPassword.any { it.isLowerCase() }
        ),
        PasswordCheck(
            text = "包含數字",
            isMet = newPassword.isNotEmpty() && newPassword.any { it.isDigit() }
        ),
        PasswordCheck(
            text = "包含特殊字符",
            isMet = newPassword.isNotEmpty() && 
                    newPassword.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }
        )
    )
    
    val isPasswordValid = currentPassword.isNotEmpty() && 
                         newPassword.isNotEmpty() && 
                         confirmPassword.isNotEmpty() &&
                         newPassword == confirmPassword &&
                         passwordChecks.all { it.isMet }

    // 密碼要求卡片（包含強度分析）
    PasswordRequirementsCard(
        newPassword = newPassword,
        currentPassword = currentPassword,
        passwordChecks = passwordChecks
    )

    Spacer(modifier = Modifier.height(24.dp))

    // 密碼匹配檢查和 Toast 處理
    val context = LocalContext.current
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            if (error.contains("密碼不匹配") || error.contains("密碼匹配")) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Update Password Button
    Button(
        onClick = {
            if (newPassword != confirmPassword) {
                // 密碼不匹配時顯示 Toast
                Toast.makeText(context, "密碼不匹配，請重新確認", Toast.LENGTH_SHORT).show()
            } else {
                onUpdatePassword()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = !uiState.isLoading && isPasswordValid,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (uiState.isLoading && !uiState.isProfileUpdateSuccess) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "更新中...",
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Text(
                text = "更新密碼",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// 提取的默認頭像組件
@Composable
private fun DefaultProfileIcon() {
    Icon(
        painter = painterResource(id = R.drawable.person_24dp),
        contentDescription = "個人資料圖像",
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}

// 提取的密碼字段組件
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onShowPasswordToggle: () -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    onKeyboardAction: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onShowPasswordToggle) {
                Icon(
                    painter = painterResource(
                        id = if (showPassword) R.drawable.visibility_off_24 else R.drawable.visibility_24dp
                    ),
                    contentDescription = if (showPassword) "隱藏密碼" else "顯示密碼",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onKeyboardAction() },
            onDone = { onKeyboardAction() }
        ),
        singleLine = true
    )
}

// 提取的圖片處理函數
@Composable
private fun ProfileImage(
    selectedImageUri: Uri?,
    userImageBytes: ByteArray?,
    onImageClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .border(
                width = if (isPressed) 4.dp else 3.dp,
                color = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
            .background(
                if (selectedImageUri == null && userImageBytes == null) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else 
                    MaterialTheme.colorScheme.surface,
                CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { 
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) {
        when {
            selectedImageUri != null -> {
                // Show selected image
                val context = LocalContext.current
                val bitmap = remember(selectedImageUri) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(selectedImageUri)
                        BitmapFactory.decodeStream(inputStream)
                    } catch (e: Exception) {
                        null
                    }
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "選擇的圖片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            userImageBytes != null -> {
                // Show current user image
                val bitmap = remember(userImageBytes) {
                    try {
                        BitmapFactory.decodeByteArray(
                            userImageBytes,
                            0,
                            userImageBytes.size
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "個人資料圖像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    DefaultProfileIcon()
                }
            }
            else -> {
                DefaultProfileIcon()
            }
        }

        // Edit overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { 
                    android.util.Log.d("ProfileImage", "Edit overlay clicked!")
                    onImageClick() 
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.edit_24dp),
                contentDescription = "編輯頭像",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 密碼可見性狀態數據類
data class PasswordVisibilityState(
    val showCurrentPassword: Boolean = false,
    val showNewPassword: Boolean = false,
    val showConfirmPassword: Boolean = false
) {
    fun toggleCurrentPassword() = copy(showCurrentPassword = !showCurrentPassword)
    fun toggleNewPassword() = copy(showNewPassword = !showNewPassword)
    fun toggleConfirmPassword() = copy(showConfirmPassword = !showConfirmPassword)
}

// 表單驗證狀態
data class FormValidationState(
    val userNameError: String? = null,
    val emailError: String? = null,
    val isFormValid: Boolean = true
) {
    fun hasErrors() = userNameError != null || emailError != null
}

// 文本字段類型枚舉
enum class TextFieldType {
    USERNAME, EMAIL
}

// 驗證函數
private fun validateUserName(userName: String): String? {
    return when {
        userName.isBlank() -> "用戶名不能為空"
        userName.length < 2 -> "用戶名至少需要2個字符"
        userName.length > 20 -> "用戶名不能超過20個字符"
        !userName.matches(Regex("^[a-zA-Z0-9\\u4e00-\\u9fa5_]+$")) -> "用戶名只能包含字母、數字、中文和下劃線"
        else -> null
    }
}

private fun validateEmail(email: String): String? {
    return when {
        email.isBlank() -> "電子郵件不能為空"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "請輸入有效的電子郵件地址"
        else -> null
    }
}

// 通用的文本字段組件
@Composable
private fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    fieldType: TextFieldType,
    errorMessage: String?,
    leadingIcon: @Composable (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    onKeyboardAction: () -> Unit = {},
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = modifier,
            leadingIcon = leadingIcon,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = when (fieldType) {
                    TextFieldType.EMAIL -> KeyboardType.Email
                    else -> KeyboardType.Text
                },
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onKeyboardAction() },
                onDone = { onKeyboardAction() }
            ),
            singleLine = true
        )
        
        // 錯誤提示
        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}


// 密碼要求卡片（包含強度分析）
@Composable
private fun PasswordRequirementsCard(
    newPassword: String,
    currentPassword: String,
    passwordChecks: List<PasswordCheck>
) {
    val strength = if (newPassword.isNotEmpty()) calculatePasswordStrength(newPassword, currentPassword) else null
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (strength != null) {
                when (strength.level) {
                    PasswordStrengthLevel.WEAK -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    PasswordStrengthLevel.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    PasswordStrengthLevel.STRONG -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                }
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.privacy_tip_24dp),
                    contentDescription = "安全要求",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "密碼安全要求",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // 密碼強度顯示
            if (strength != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = when (strength.level) {
                                PasswordStrengthLevel.WEAK -> R.drawable.warning_24dp
                                PasswordStrengthLevel.MEDIUM -> R.drawable.info_24dp
                                PasswordStrengthLevel.STRONG -> R.drawable.check_circle_24dp
                            }
                        ),
                        contentDescription = "密碼強度",
                        tint = when (strength.level) {
                            PasswordStrengthLevel.WEAK -> MaterialTheme.colorScheme.error
                            PasswordStrengthLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                            PasswordStrengthLevel.STRONG -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "強度：${strength.label}",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (strength.level) {
                            PasswordStrengthLevel.WEAK -> MaterialTheme.colorScheme.error
                            PasswordStrengthLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                            PasswordStrengthLevel.STRONG -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
                
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 使用相同的密碼檢查邏輯
            
            passwordChecks.forEach { check ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (check.isMet) {
                        Icon(
                            painter = painterResource(id = R.drawable.check_circle_24dp),
                            contentDescription = "已符合",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                    }
                    Text(
                        text = check.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (check.isMet) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// 密碼強度計算
private fun calculatePasswordStrength(password: String, currentPassword: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength(PasswordStrengthLevel.WEAK, "未輸入", emptyList())
    
    var score = 0
    
    // 長度檢查
    if (password.length >= 8) score += 2
    else if (password.length >= 6) score += 1
    
    // 字符多樣性檢查
    if (password.any { it.isUpperCase() }) score += 1
    if (password.any { it.isLowerCase() }) score += 1
    if (password.any { it.isDigit() }) score += 1
    if (password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) score += 1
    
    // 不能與當前密碼相同
    if (password == currentPassword) {
        score = 0
    }
    
    val level = when {
        score >= 5 -> PasswordStrengthLevel.STRONG
        score >= 3 -> PasswordStrengthLevel.MEDIUM
        else -> PasswordStrengthLevel.WEAK
    }
    
    val label = when (level) {
        PasswordStrengthLevel.WEAK -> "弱"
        PasswordStrengthLevel.MEDIUM -> "中等"
        PasswordStrengthLevel.STRONG -> "強"
    }
    
    return PasswordStrength(level, label, emptyList())
}

// 密碼強度數據類
data class PasswordStrength(
    val level: PasswordStrengthLevel,
    val label: String,
    val suggestions: List<String>
)

// 密碼檢查數據類
data class PasswordCheck(
    val text: String,
    val isMet: Boolean
)

enum class PasswordStrengthLevel {
    WEAK, MEDIUM, STRONG
}

enum class ProfileTab(val title: String) {
    PROFILE("個人資料"),
    PASSWORD("更改密碼")
}
