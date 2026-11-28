/*
LotteryDetailsScreen.kt

Purpose: Shows the details of one lottery. Opened by clicking a lottery in the Lotteries list on a
campsite's Details page. From top to bottom: the campsite name(s), whether registration is open
(and when it closes), a row of entrants / winners / waitlist places, the campsites in the lottery,
the registration and booking dates, and a button that opens the edit lottery screen.

Issues:
 - the waitlist cap is shared by every campsite in the lottery, so each campsite row shows the
   lottery-wide cap
 */

package com.example.toastytraveltools.view

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.toastytraveltools.controller.CampsiteRepository
import com.example.toastytraveltools.controller.LotteryRepository
import com.example.toastytraveltools.model.Campsite
import com.example.toastytraveltools.model.Lottery
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/** Where a lottery's registration period is relative to [now]. */
enum class RegistrationState { NOT_STARTED, OPEN, CLOSED }

/** Works out whether registration is open. A lottery with missing dates counts as closed. */
fun registrationState(lottery: Lottery, now: Timestamp = Timestamp.now()): RegistrationState {
    val start = lottery.registrationStart
    val end = lottery.registrationEnd
    return when {
        start == null || end == null -> RegistrationState.CLOSED
        now < start -> RegistrationState.NOT_STARTED
        now > end -> RegistrationState.CLOSED
        else -> RegistrationState.OPEN
    }
}

/** Everyone who entered. Winners are taken off the waitlist when the lottery is drawn, so count both. */
fun lotteryEntrantCount(lottery: Lottery): Int =
    (lottery.waitlist + lottery.winners).distinct().size

/** Winners drawn so far, or how many will be drawn if the lottery hasn't been drawn yet. */
fun lotteryWinnerCount(lottery: Lottery): Int =
    if (lottery.status == "drawn") lottery.winners.size else lottery.numberOfWinners

/** Places left on the waitlist, or null when the lottery has no waitlist cap. */
fun waitlistPlacesLeft(lottery: Lottery): Int? =
    lottery.waitlistCapacity?.let { (it - lotteryEntrantCount(lottery)).coerceAtLeast(0) }

/** The heading for a lottery: its campsite names, in the order the lottery lists them. */
fun lotteryHeading(lottery: Lottery, campsites: List<Campsite>): String {
    val names = lottery.campsiteIds.mapNotNull { id -> campsites.find { it.id == id }?.name }
    return names.joinToString(", ").ifBlank { lottery.lotteryName.ifBlank { "Lottery" } }
}

private fun formatLotteryDate(timestamp: Timestamp?): String =
    timestamp?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it.toDate()) } ?: "Not set"

/** One line saying whether registration is open, e.g. "Registration open · closes Dec 5, 2026". */
fun registrationSummary(lottery: Lottery, now: Timestamp = Timestamp.now()): String =
    when (registrationState(lottery, now)) {
        RegistrationState.OPEN -> "Registration open · closes ${formatLotteryDate(lottery.registrationEnd)}"
        RegistrationState.NOT_STARTED -> "Registration not open yet · opens ${formatLotteryDate(lottery.registrationStart)}"
        RegistrationState.CLOSED -> "Registration closed"
    }

@Composable
fun LotteryDetailsScreen(
    lotteryId: String,
    lotteryRepository: LotteryRepository,
    campsiteRepository: CampsiteRepository,
    onBack: () -> Unit,
    onEditLottery: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    var lottery by remember { mutableStateOf<Lottery?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lotteryId) {
        val registration = lotteryRepository.listenToLottery(
            lotteryId = lotteryId,
            onResult = {
                lottery = it
                loaded = true
            },
            onFailure = {
                loadError = "Could not load this lottery: ${it.message}"
                loaded = true
            }
        )
        onDispose { registration.remove() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lotteryPageColor())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("Back")
        }

        val current = lottery
        if (current == null) {
            Text(
                text = when {
                    loadError != null -> loadError!!
                    loaded -> "This lottery no longer exists."
                    else -> "Loading..."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (loadError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            return@Column
        }

        val campsites = current.campsiteIds.mapNotNull { id ->
            campsiteRepository.campsites.find { it.id == id }
        }
        val state = registrationState(current)
        val entrants = lotteryEntrantCount(current)
        val winners = lotteryWinnerCount(current)
        val placesLeft = waitlistPlacesLeft(current)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // heading: campsite name(s), then whether registration is open
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = lotteryHeading(current, campsites),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = registrationSummary(current),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state == RegistrationState.OPEN) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // entrants / winners / waitlist places
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LotteryStatBox("$entrants", "Entrants", Modifier.weight(1f))
                LotteryStatBox("$winners", "Winners", Modifier.weight(1f))
                LotteryStatBox(placesLeft?.toString() ?: "∞", "Waitlist places", Modifier.weight(1f))
            }

            // selected campsites
            Text(
                text = "Selected campsites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LotteryCard {
                if (campsites.isEmpty()) {
                    Text(
                        text = if (campsiteRepository.isLoading) "Loading campsites..." else "No campsites found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                campsites.forEachIndexed { index, site ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SelectedCampsiteRow(
                        campsite = site,
                        waitlistCapacity = current.waitlistCapacity
                    )
                }
            }

            // registration and booking dates
            Text(
                text = "Dates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LotteryCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LotteryDateRow("Registration opens", formatLotteryDate(current.registrationStart))
                    LotteryDateRow("Registration closes", formatLotteryDate(current.registrationEnd))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LotteryDateRow("Booking opens", formatLotteryDate(current.bookingStartDate))
                    LotteryDateRow("Booking closes", formatLotteryDate(current.bookingEndDate))
                }
            }
        }

        // pinned to the bottom of the screen; only the lottery's owner can edit it
        if (isCampsiteOwner(current.ownerId, deviceId)) {
            Button(
                onClick = { onEditLottery(current.id) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("Edit lottery")
            }
        }
    }
}

@Composable
private fun LotteryCard(content: @Composable () -> Unit) {
    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun LotteryStatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SelectedCampsiteRow(campsite: Campsite, waitlistCapacity: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = campsite.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (campsite.location.isNotBlank()) {
                Text(
                    text = campsite.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (waitlistCapacity != null) {
            Text("Waitlist cap $waitlistCapacity", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LotteryDateRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

/** One lottery in the Lotteries list on a campsite's Details page. Clicking it opens its details. */
@Composable
fun LotteryListItem(lottery: Lottery, onClick: () -> Unit) {
    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = registrationSummary(lottery),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Booking ${formatLotteryDate(lottery.bookingStartDate)} – ${formatLotteryDate(lottery.bookingEndDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** White page, or the normal dark background in dark theme. */
@Composable
private fun lotteryPageColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color.White
    else MaterialTheme.colorScheme.background
