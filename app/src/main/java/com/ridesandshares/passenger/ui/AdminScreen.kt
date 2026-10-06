package com.ridesandshares.passenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesandshares.passenger.demo.AdminMock
import com.ridesandshares.passenger.ui.theme.Ink
import com.ridesandshares.passenger.ui.theme.InkText
import com.ridesandshares.passenger.ui.theme.Muted
import com.ridesandshares.passenger.ui.theme.Paper
import com.ridesandshares.passenger.ui.theme.Rule

private enum class AdminSection(val label: String) {
    Overview("Overview"),
    Campaigns("Campaigns"),
    Tablets("Tablets"),
    Rides("Rides"),
}

@Composable
fun AdminScreen(
    onOpenTablet: () -> Unit,
    onOpenDriver: () -> Unit,
) {
    var section by remember { mutableStateOf(AdminSection.Overview) }
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
    ) {
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(ColorWash)
                .padding(vertical = 20.dp),
        ) {
            Text(
                text = "Rides and Shares",
                color = InkText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Text(
                text = "Admin",
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
            AdminSection.entries.forEach { item ->
                val selected = item == section
                Text(
                    text = item.label,
                    color = if (selected) Paper else InkText,
                    fontSize = 16.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Ink else ColorWash)
                        .testTag("admin-${item.name}")
                        .clickable { section = item }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Passenger tablet",
                color = InkText,
                fontSize = 14.sp,
                modifier = Modifier
                    .testTag("demo-tablet")
                    .clickable(onClick = onOpenTablet)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Text(
                text = "Driver phone",
                color = InkText,
                fontSize = 14.sp,
                modifier = Modifier
                    .testTag("demo-driver")
                    .clickable(onClick = onOpenDriver)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = when (section) {
                    AdminSection.Overview -> "Overview"
                    AdminSection.Campaigns -> "Campaign performance"
                    AdminSection.Tablets -> "Tablet fleet"
                    AdminSection.Rides -> "Rides"
                },
                color = InkText,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            )
            when (section) {
                AdminSection.Overview -> OverviewBody()
                AdminSection.Campaigns -> CampaignList()
                AdminSection.Tablets -> TabletList()
                AdminSection.Rides -> RideList()
            }
        }
    }
}

@Composable
private fun OverviewBody() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard("Fleet online", AdminMock.FLEET_ONLINE, Modifier.weight(1f))
        KpiCard("Impressions today", AdminMock.IMPRESSIONS_TODAY, Modifier.weight(1f))
        KpiCard("Scans today", AdminMock.SCANS_TODAY, Modifier.weight(1f))
        KpiCard("Active campaigns", AdminMock.ACTIVE_CAMPAIGNS, Modifier.weight(1f))
    }
    Text(
        text = "Analytics",
        color = InkText,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )
    CampaignList()
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tablets", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            TabletList()
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recent rides", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            RideList()
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ColorWash)
            .padding(16.dp),
    ) {
        Text(text = label, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = InkText,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CampaignList() {
    val peak = AdminMock.campaigns.maxOf { it.impressions }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminMock.campaigns.forEach { campaign ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(campaign.businessName, color = InkText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "${campaign.impressionLabel()} impressions · ${campaign.scanRate} scans",
                        color = Muted,
                        fontSize = 14.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Rule),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(campaign.impressions.toFloat() / peak)
                            .height(8.dp)
                            .background(Ink),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminMock.tablets.forEach { tablet ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("${tablet.id} ${tablet.neighborhood}", color = InkText, fontSize = 16.sp)
                    Text("Showing ${tablet.nowShowing}", color = Muted, fontSize = 13.sp)
                }
                Text(
                    text = if (tablet.online) "Online" else "Offline",
                    color = if (tablet.online) Online else Muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun RideList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminMock.rides.forEach { ride ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(ride.destination, color = InkText, fontSize = 16.sp)
                    val detail = if (ride.arrival == null) ride.miles else "${ride.miles} · ${ride.arrival}"
                    Text(detail, color = Muted, fontSize = 13.sp)
                }
                Text(
                    text = ride.status,
                    color = InkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private val ColorWash = androidx.compose.ui.graphics.Color(0xFFEFEBE3)
private val Online = androidx.compose.ui.graphics.Color(0xFF1E6B45)
