package com.ridesandshares.passenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesandshares.passenger.R
import com.ridesandshares.passenger.demo.RideDemo
import com.ridesandshares.passenger.ui.theme.Ink
import com.ridesandshares.passenger.ui.theme.Paper
import kotlinx.coroutines.delay

@Composable
fun RideClock(ride: RideDemo) {
    LaunchedEffect(ride) {
        while (true) {
            delay(1_000)
            ride.tick()
        }
    }
}

@Composable
fun RideBar(
    ride: RideDemo,
    onOpenDriver: () -> Unit,
    onOpenAdmin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink)
            .padding(horizontal = 28.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RouteTrack(ride.progress)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ride.statusLabel(),
                    color = Paper,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
                val miles = ride.milesLabel()
                val destination = ride.destinationLabel()
                if (miles != null) {
                    Text(
                        text = miles,
                        color = Paper.copy(alpha = 0.82f),
                        fontSize = 16.sp,
                    )
                }
                if (destination != null) {
                    Text(
                        text = destination,
                        color = Paper.copy(alpha = 0.82f),
                        fontSize = 16.sp,
                    )
                }
            }
            Text(
                text = ride.countdownLabel(),
                color = Paper,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DemoLink("Driver phone", "demo-driver", onOpenDriver)
            DemoLink("Admin", "demo-admin", onOpenAdmin)
        }
    }
}

@Composable
private fun DemoLink(label: String, tag: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Paper.copy(alpha = 0.72f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .testTag(tag)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun RouteTrack(progress: Float) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
    ) {
        val clamped = progress.coerceIn(0f, 1f)
        val travel = (maxWidth - 36.dp) * clamped
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Paper.copy(alpha = 0.28f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = travel)
                .size(36.dp)
                .clip(CircleShape)
                .background(Paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_route_car),
                contentDescription = "Car on the route",
                tint = Ink,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
