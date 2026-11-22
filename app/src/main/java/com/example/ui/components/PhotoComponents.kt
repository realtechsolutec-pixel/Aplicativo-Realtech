package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.util.ImageCompressionPreset
import com.example.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private fun createTempCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "foto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private data class CompressionSummary(
    val originalBytes: Long,
    val compressedBytes: Long,
    val savedPercent: Int
)

@Composable
private fun CompressionPresetSelector(
    preset: ImageCompressionPreset,
    onPresetChange: (ImageCompressionPreset) -> Unit,
    summary: CompressionSummary?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Compress,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Compactação automática (WebP • Supabase)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ImageCompressionPreset.entries.forEach { item ->
                    FilterChip(
                        selected = preset == item,
                        onClick = { onPresetChange(item) },
                        label = { Text(item.shortLabel, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("compression_chip_${item.code.lowercase()}")
                    )
                }
            }
            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (summary != null) {
                Surface(
                    color = StatusSuccessContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Economia estimada: ${ImageCompressor.formatBytes(summary.originalBytes)} → ${ImageCompressor.formatBytes(summary.compressedBytes)} (-${summary.savedPercent}% no Supabase)",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusSuccess,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MultiPhotoPickerSection(
    title: String,
    existingUrls: List<String>,
    localUris: List<Uri>,
    compressionPreset: ImageCompressionPreset = ImageCompressionPreset.EQUILIBRADO,
    onCompressionPresetChange: (ImageCompressionPreset) -> Unit = {},
    onRemoveExistingUrl: (String) -> Unit,
    onAddLocalUris: (List<Uri>) -> Unit,
    onRemoveLocalUri: (Uri) -> Unit,
    onPreviewPhoto: (Any) -> Unit,
    maxPhotos: Int = 6
) {
    val context = LocalContext.current
    val totalCount = existingUrls.size + localUris.size
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var compressionSummary by remember { mutableStateOf<CompressionSummary?>(null) }

    LaunchedEffect(localUris, compressionPreset) {
        if (localUris.isEmpty()) {
            compressionSummary = null
        } else {
            compressionSummary = withContext(Dispatchers.IO) {
                var orig = 0L
                var comp = 0L
                for (uri in localUris) {
                    val res = ImageCompressor.compressUri(context, uri, compressionPreset)
                    if (res != null) {
                        orig += res.originalBytes
                        comp += res.compressedBytes.toLong()
                    }
                }
                if (orig > 0L && comp > 0L) {
                    val pct = (((orig - comp).toDouble() / orig.toDouble()) * 100.0).toInt().coerceIn(0, 99)
                    CompressionSummary(orig, comp, pct)
                } else null
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxPhotos)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = (maxPhotos - totalCount).coerceAtLeast(0)
            if (remaining > 0) {
                onAddLocalUris(uris.take(remaining))
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onAddLocalUris(listOf(uri))
        }
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permissão de câmera negada.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$title ($totalCount/$maxPhotos)",
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = totalCount < maxPhotos,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("os_gallery_photo_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Galeria")
            }

            OutlinedButton(
                onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        val uri = createTempCameraUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                enabled = totalCount < maxPhotos,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("os_camera_photo_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Câmera")
            }
        }

        if (totalCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                existingUrls.forEach { url ->
                    PhotoThumbnailWithRemove(
                        model = url,
                        onClick = { onPreviewPhoto(url) },
                        onRemove = { onRemoveExistingUrl(url) }
                    )
                }
                localUris.forEach { uri ->
                    PhotoThumbnailWithRemove(
                        model = uri,
                        onClick = { onPreviewPhoto(uri) },
                        onRemove = { onRemoveLocalUri(uri) }
                    )
                }
            }
        }

        CompressionPresetSelector(
            preset = compressionPreset,
            onPresetChange = onCompressionPresetChange,
            summary = compressionSummary
        )
    }
}

@Composable
fun SinglePhotoPickerSection(
    title: String,
    existingUrl: String,
    localUri: Uri?,
    compressionPreset: ImageCompressionPreset = ImageCompressionPreset.EQUILIBRADO,
    onCompressionPresetChange: (ImageCompressionPreset) -> Unit = {},
    onClearPhoto: () -> Unit,
    onSelectLocalUri: (Uri) -> Unit,
    onPreviewPhoto: (Any) -> Unit
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var compressionSummary by remember { mutableStateOf<CompressionSummary?>(null) }

    LaunchedEffect(localUri, compressionPreset) {
        if (localUri == null) {
            compressionSummary = null
        } else {
            compressionSummary = withContext(Dispatchers.IO) {
                val res = ImageCompressor.compressUri(context, localUri, compressionPreset)
                if (res != null && res.originalBytes > 0L) {
                    CompressionSummary(res.originalBytes, res.compressedBytes.toLong(), res.savedPercent)
                } else null
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onSelectLocalUri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onSelectLocalUri(uri)
        }
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permissão de câmera negada.", Toast.LENGTH_SHORT).show()
        }
    }

    val activeModel: Any? = localUri ?: existingUrl.takeIf { it.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (activeModel != null) {
                PhotoThumbnailWithRemove(
                    model = activeModel,
                    onClick = { onPreviewPhoto(activeModel) },
                    onRemove = onClearPhoto
                )
            }
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("peca_gallery_photo_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Galeria")
            }
            OutlinedButton(
                onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        val uri = createTempCameraUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("peca_camera_photo_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Câmera")
            }
        }

        CompressionPresetSelector(
            preset = compressionPreset,
            onPresetChange = onCompressionPresetChange,
            summary = compressionSummary
        )
    }
}

@Composable
private fun PhotoThumbnailWithRemove(
    model: Any,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.size(78.dp)) {
        AsyncImage(
            model = model,
            contentDescription = "Foto anexada",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
        )
        Surface(
            color = Color(0xCC0F172A),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(22.dp)
                .clickable(onClick = onRemove)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remover foto",
                tint = Color.White,
                modifier = Modifier.padding(3.dp)
            )
        }
    }
}

@Composable
fun FullScreenPhotoDialog(
    imageModel: Any,
    title: String = "Visualizar Foto",
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE0B1220))
                .padding(16.dp)
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar foto", tint = Color.White)
                }
            }
        }
    }
}
