package com.ridesandshares.passenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesandshares.passenger.R
import com.ridesandshares.passenger.ui.theme.Ink
import com.ridesandshares.passenger.ui.theme.Paper
import com.ridesandshares.trip.TripLink
import com.ridesandshares.trip.TripProgress
import com.ridesandshares.trip.formatTripDistance
import com.ridesandshares.trip.formatTripDuration

@Composable
fun TripBanner(
    trip: TripProgress?,
    tabletAddress: String?,
    nowMs: Long = System.currentTimeMillis(),
) {
    val fresh = trip?.takeIf { it.isFresh(nowMs) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink)
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        if (fresh == null) {
            Text(
                text = stringResource(R.string.trip_waiting),
                color = Paper,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            if (!tabletAddress.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.trip_share_address, "$tabletAddress:${TripLink.PORT}"),
                    color = Paper.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = "${formatTripDistance(fresh.distanceMeters)}  ·  ${formatTripDuration(fresh.durationSeconds)}",
                color = Paper,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.trip_to_destination, fresh.destination),
                color = Paper.copy(alpha = 0.82f),
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
