package com.lux.field.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lux.field.BuildConfig
import com.lux.field.R
import com.lux.field.ui.map.components.WorkOrderBottomSheet
import com.lux.field.ui.map.components.WorkOrderMapContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onWorkOrderClick: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.userName.isNotEmpty()) {
                            uiState.userName
                        } else {
                            stringResource(R.string.app_name)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.map_refresh),
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
                onClick = { /* my location — future GPS integration */ },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = if (showBottomSheet) 200.dp else 0.dp),
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
                )
            }
        }
    }
}
