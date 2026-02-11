package com.lux.field.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lux.field.R
import com.lux.field.ui.navigation.components.ManeuverBanner
import com.lux.field.ui.navigation.components.NavigationMapContent
import com.lux.field.ui.navigation.components.TripProgressBar

@Composable
fun NavigationScreen(
    workOrderId: String,
    destLat: Double,
    destLng: Double,
    onBack: () -> Unit,
    onArrived: () -> Unit,
    viewModel: NavigationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val routes by viewModel.navigationRoutes.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.stopNavigation()
        onBack()
    }

    LaunchedEffect(uiState.hasArrived) {
        if (uiState.hasArrived) {
            onArrived()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-screen map with route line, puck, and navigation camera
        NavigationMapContent(
            routes = routes,
            destLat = destLat,
            destLng = destLng,
            isNavigating = uiState.isNavigating,
        )

        // Back button
        IconButton(
            onClick = {
                viewModel.stopNavigation()
                onBack()
            },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 48.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }

        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Maneuver banner (during active navigation)
        if (uiState.isNavigating && uiState.currentManeuver != null) {
            ManeuverBanner(
                maneuver = uiState.currentManeuver!!,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
            )
        }

        // Start button (route preview state)
        if (!uiState.isNavigating && uiState.routePreview.isNotEmpty()) {
            Button(
                onClick = { viewModel.startNavigation() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 32.dp),
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(R.string.nav_start))
            }
        }

        // Trip progress bar (during active navigation)
        if (uiState.isNavigating && uiState.tripProgress != null) {
            TripProgressBar(
                progress = uiState.tripProgress!!,
                onStopClick = {
                    viewModel.stopNavigation()
                    onBack()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            )
        }

        // Error snackbar
        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Text(
                    text = when (uiState.error) {
                        "nav_no_location" -> stringResource(R.string.nav_no_location)
                        else -> uiState.error ?: stringResource(R.string.nav_route_error)
                    }
                )
            }
        }

        // Arrived snackbar
        if (uiState.hasArrived) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(stringResource(R.string.nav_arrived))
            }
        }
    }
}
