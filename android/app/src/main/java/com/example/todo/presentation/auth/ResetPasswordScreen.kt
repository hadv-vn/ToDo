package com.example.todo.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Màn hình Đặt lại mật khẩu — theo LLD mục 4.2/6.2. `email` lấy từ nav arg qua SavedStateHandle
 * trong [ResetPasswordViewModel] (route `reset_password/{email}`), KHÔNG cần truyền qua tham số
 * Composable nữa.
 */
@Composable
fun ResetPasswordScreen(
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.resetSuccess) {
        if (uiState.resetSuccess) {
            onNavigateToLogin()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Đặt lại mật khẩu", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Mã xác nhận đã được gửi tới ${uiState.email}")
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = uiState.otp,
            onValueChange = viewModel::onOtpChange,
            label = { Text("Mã xác nhận (6 số)") },
            isError = uiState.otpError != null,
            supportingText = { uiState.otpError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.newPassword,
            onValueChange = viewModel::onNewPasswordChange,
            label = { Text("Mật khẩu mới") },
            isError = uiState.newPasswordError != null,
            supportingText = {
                Text(uiState.newPasswordError ?: "Tối thiểu 8 ký tự, có ít nhất 1 chữ và 1 số")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.confirmNewPassword,
            onValueChange = viewModel::onConfirmNewPasswordChange,
            label = { Text("Xác nhận mật khẩu mới") },
            isError = uiState.confirmNewPasswordError != null,
            supportingText = { uiState.confirmNewPasswordError?.let { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        uiState.generalError?.let { error ->
            Text(text = error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = viewModel::onSubmit,
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Xác nhận")
            }
        }
    }
}
