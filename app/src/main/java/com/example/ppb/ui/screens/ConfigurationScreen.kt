package com.example.ppb.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.ppb.R
import com.example.ppb.data.model.UiEvent
import com.example.ppb.ui.util.DollarAmountTransformation
import com.example.ppb.ui.viewmodels.ConfigurationViewModel

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun ConfigurationScreenPreview() {
    ConfigurationScreen()
}

@Composable
fun ConfigurationScreen(
    viewModel: ConfigurationViewModel = hiltViewModel(),
    onNavBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadConfiguration()
    }
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(8.dp)
            .verticalScroll(scrollState) // Makes content scrollable
    ) {
        Text(stringResource(R.string.config_title), style = MaterialTheme.typography.headlineLarge)
        Row {
            Button(onClick = onNavBack) {
                Text(stringResource(R.string.config_back))
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Button(onClick = {
                viewModel.saveConfiguration()
            }) {
                Text(stringResource(R.string.config_save))
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Button(onClick = {
                viewModel.saveConfiguration()
                onNavBack()
            }) {
                Text(stringResource(R.string.config_save_back))
            }
        }
        Spacer(modifier = Modifier.padding(vertical = 2.dp))
        Row {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                TextField(
                    state = viewModel.adultPrice,
                    label = { Text(stringResource(R.string.config_label_adult)) },
                    prefix = { Text("$ ") },
                    inputTransformation = DollarAmountTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                TextField(
                    state = viewModel.childPrice,
                    label = { Text(stringResource(R.string.config_label_child)) },
                    prefix = { Text("$ ") },
                    inputTransformation = DollarAmountTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                TextField(
                    state = viewModel.planePrice,
                    label = { Text(stringResource(R.string.config_label_plane)) },
                    prefix = { Text("$ ") },
                    inputTransformation = DollarAmountTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                TextField(
                    state = viewModel.cardPercentFee,
                    label = { Text(stringResource(R.string.config_label_percent_fee)) },
                    prefix = { Text("% ") },
                    inputTransformation = DollarAmountTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                TextField(
                    state = viewModel.cardFixedFee,
                    label = { Text(stringResource(R.string.config_label_fixed_fee)) },
                    prefix = { Text("$ ") },
                    inputTransformation = DollarAmountTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
            }
        }
    }
}
