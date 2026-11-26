/*
CampsiteImages.kt

Purpose: Reusable composables for showing and picking campsite photos that live in
Firebase Storage under campsite_images/{campsiteId}/.
 - CampsiteThumbnail: small square preview used in search results
 - CampsiteImageCarousel: swipeable gallery used on the details screen
 - CampsiteImagePicker: add/remove photos while creating or editing a campsite
 */

package com.example.toastytraveltools.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/** Max number of photos the picker lets an owner choose in one go. */
const val MAX_CAMPSITE_IMAGES_PER_PICK = 10

/**
 * Square preview of a campsite's first photo, or a neutral placeholder when it has none.
 *
 * @param imageUrl download URL of the photo to show, or null for the placeholder
 * @param size width and height of the thumbnail
 */
@Composable
fun CampsiteThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp
) {
    val shape = RoundedCornerShape(10.dp)
    if (imageUrl.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No photo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Campsite photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

/**
 * Swipeable gallery of campsite photos with a page counter and dot indicator.
 * Tapping a photo opens it full screen. Shows a placeholder when there are no photos.
 *
 * @param imageUrls download URLs of the campsite's photos
 * @param emptyContent what to show when [imageUrls] is empty
 */
@Composable
fun CampsiteImageCarousel(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    emptyContent: @Composable () -> Unit = { NoPhotosPlaceholder(height) }
) {
    if (imageUrls.isEmpty()) {
        emptyContent()
        return
    }

    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    var fullscreenStartPage by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = "Campsite photo ${page + 1} of ${imageUrls.size}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { fullscreenStartPage = page }
                )
            }
            if (imageUrls.size > 1) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (imageUrls.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(imageUrls.size) { index ->
                    val selected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }

    fullscreenStartPage?.let { start ->
        FullscreenImageViewer(
            imageUrls = imageUrls,
            startPage = start,
            onDismiss = { fullscreenStartPage = null }
        )
    }
}

@Composable
private fun NoPhotosPlaceholder(height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No photos yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FullscreenImageViewer(
    imageUrls: List<String>,
    startPage: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val pagerState = rememberPagerState(initialPage = startPage, pageCount = { imageUrls.size })
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = "Campsite photo ${page + 1} of ${imageUrls.size}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = "Close",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
            if (imageUrls.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                )
            }
        }
    }
}

/**
 * Lets an owner attach any number of photos (including none) to a campsite.
 *
 * Shows already-uploaded photos ([existingUrls]) and newly picked, not-yet-uploaded
 * photos ([newImages]) side by side, each with a remove button.
 *
 * @param existingUrls photos already stored in Firebase Storage for this campsite
 * @param newImages local photos picked in this session, uploaded on save
 * @param onAddImages called with the photos the user just picked
 * @param onRemoveExisting called when an already-uploaded photo is removed
 * @param onRemoveNew called when a newly picked photo is removed
 */
@Composable
fun CampsiteImagePicker(
    existingUrls: List<String>,
    newImages: List<Uri>,
    onAddImages: (List<Uri>) -> Unit,
    onRemoveExisting: (String) -> Unit,
    onRemoveNew: (Uri) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_CAMPSITE_IMAGES_PER_PICK)
    ) { uris ->
        if (uris.isNotEmpty()) onAddImages(uris)
    }
    val total = existingUrls.size + newImages.size

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (total == 0) "Photos (optional)" else "Photos ($total)",
                style = MaterialTheme.typography.titleSmall
            )
            OutlinedButton(
                onClick = {
                    launcher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = enabled
            ) {
                Text("Add photos")
            }
        }

        if (total == 0) {
            Text(
                text = "No photos added. You can add some now or later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(existingUrls, key = { "existing:$it" }) { url ->
                    RemovableThumbnail(
                        model = url,
                        enabled = enabled,
                        onRemove = { onRemoveExisting(url) }
                    )
                }
                items(newImages, key = { "new:$it" }) { uri ->
                    RemovableThumbnail(
                        model = uri,
                        enabled = enabled,
                        label = "New",
                        onRemove = { onRemoveNew(uri) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RemovableThumbnail(
    model: Any,
    enabled: Boolean,
    onRemove: () -> Unit,
    label: String? = null
) {
    Box(modifier = Modifier.size(96.dp)) {
        AsyncImage(
            model = model,
            contentDescription = "Campsite photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        if (label != null) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(26.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled) { onRemove() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("✕", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
