package com.lux.field.ui.workorder.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lux.field.R
import com.lux.field.domain.model.CameraFacing
import com.lux.field.domain.model.PhotoAnalysisStatus
import com.lux.field.domain.model.TaskPhoto
import java.io.File

@Composable
fun PhotoGalleryStrip(
    photos: List<TaskPhoto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.photo_gallery_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(photos, key = { it.id }) { photo ->
                PhotoThumbnail(photo = photo)
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(
    photo: TaskPhoto,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(end = 8.dp)
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        AsyncImage(
            model = File(photo.thumbnailPath ?: photo.filePath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(72.dp),
        )

        // Camera type icon overlay
        Icon(
            imageVector = if (photo.cameraFacing == CameraFacing.FRONT) {
                Icons.Default.Person
            } else {
                Icons.Default.CameraAlt
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(1.dp),
        )

        // Analysis spinner
        if (photo.analysisStatus == PhotoAnalysisStatus.PENDING) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(4.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}
