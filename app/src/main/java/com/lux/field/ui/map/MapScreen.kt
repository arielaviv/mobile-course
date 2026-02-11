package com.lux.field.ui.map

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.lux.field.BuildConfig
import com.lux.field.R
import com.lux.field.service.LocationTrackingService
import com.lux.field.ui.components.EmptyState
import com.lux.field.ui.map.components.WorkOrderBottomSheet
import com.lux.field.ui.map.components.WorkOrderMapContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onWorkOrderClick: (String) -> Unit,
    onNavigateToWorkOrder: (String, Double, Double) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mapStyle by viewModel.mapStyle.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Step 1: Foreground location + notification permissions
    val foregroundPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val foregroundPermissionState = rememberMultiplePermissionsState(foregroundPermissions)

    val hasLocationPermission = foregroundPermissionState.permissions
        .any { it.permission == Manifest.permission.ACCESS_FINE_LOCATION && it.status.isGranted }

    // Step 2: Background location (must be requested separately after foreground is granted)
    val backgroundPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        emptyList()
    }
    val backgroundPermissionState = if (backgroundPermissions.isNotEmpty()) {
        rememberMultiplePermissionsState(backgroundPermissions)
    } else {
        null
    }

    // Request foreground permissions on first composition
    LaunchedEffect(Unit) {
        foregroundPermissionState.launchMultiplePermissionRequest()
    }

    // After foreground granted, request background location
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            LocationTrackingService.start(context)
            // Request background location separately (Android requires this)
            backgroundPermissionState?.launchMultiplePermissionRequest()
        }
    }

    // Callback to center map on user location
    var centerOnUser by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (uiState.userName.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // User initial avatar
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = uiState.userName.first().uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.userName,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.map_refresh),
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { centerOnUser = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.map_my_location))
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            WorkOrderMapContent(
                workOrders = uiState.workOrders,
                selectedWorkOrder = uiState.selectedWorkOrder,
                mapboxToken = BuildConfig.MAPBOX_PUBLIC_TOKEN,
                styleUri = mapStyle.styleUri,
                userLocation = userLocation,
                centerOnUser = centerOnUser,
                onCenterOnUserConsumed = { centerOnUser = false },
                onMarkerClick = { wo ->
                    viewModel.selectWorkOrder(wo)
                    showBottomSheet = true
                    scope.launch { sheetState.show() }
                },
            )

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            // Empty state overlay
            if (uiState.workOrders.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    icon = Icons.Default.WorkOutline,
                    title = stringResource(R.string.map_empty_title),
                    subtitle = stringResource(R.string.map_empty_subtitle),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                WorkOrderBottomSheet(
                    workOrders = uiState.workOrders,
                    selectedWorkOrder = uiState.selectedWorkOrder,
                    onWorkOrderSelect = { viewModel.selectWorkOrder(it) },
                    onViewDetails = { onWorkOrderClick(it.id) },
                    onNavigateClick = { wo ->
                        onNavigateToWorkOrder(wo.id, wo.location.latitude, wo.location.longitude)
                    },
                )
            }
        }
    }
}
