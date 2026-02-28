package com.lux.field.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lux.field.R
import com.lux.field.ui.theme.LuxBgBottom
import com.lux.field.ui.theme.LuxBgMid
import com.lux.field.ui.theme.LuxBgTop
import com.lux.field.ui.theme.StatusInProgress
import com.lux.field.ui.theme.Zinc100
import com.lux.field.ui.theme.Zinc400

@Composable
fun RegisterScreen(
    onRegistrationSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) onRegistrationSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(LuxBgTop, LuxBgMid, LuxBgBottom),
                )
            )
            .imePadding(),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Zinc100,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Zinc100,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Zinc400,
            )

            Spacer(modifier = Modifier.height(40.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Zinc100,
                unfocusedBorderColor = Zinc400,
                focusedLabelColor = Zinc100,
                unfocusedLabelColor = Zinc400,
                cursorColor = Zinc100,
                focusedTextColor = Zinc100,
                unfocusedTextColor = Zinc100,
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text(stringResource(R.string.register_name_label)) },
                placeholder = { Text(stringResource(R.string.register_name_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Zinc400)
                },
                singleLine = true,
                enabled = !uiState.isCodeSent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChanged,
                label = { Text(stringResource(R.string.login_phone_label)) },
                placeholder = { Text(stringResource(R.string.login_phone_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Zinc400)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                enabled = !uiState.isCodeSent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
            )

            AnimatedVisibility(visible = uiState.isCodeSent) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.mockCode != null) {
                        Text(
                            text = stringResource(R.string.register_mock_code_hint, uiState.mockCode!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusInProgress,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = uiState.code,
                        onValueChange = viewModel::onCodeChanged,
                        label = { Text(stringResource(R.string.register_code_label)) },
                        placeholder = { Text(stringResource(R.string.register_code_placeholder)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Zinc400)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                    )
                }
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!uiState.isCodeSent) {
                Button(
                    onClick = viewModel::requestCode,
                    enabled = !uiState.isLoading
                            && uiState.name.isNotBlank()
                            && uiState.phone.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.register_send_code),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            } else {
                Button(
                    onClick = viewModel::verify,
                    enabled = !uiState.isLoading && uiState.code.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.register_verify),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
