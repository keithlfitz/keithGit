package com.ridesandshares.passenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesandshares.passenger.demo.RideDemo
import com.ridesandshares.passenger.ui.theme.Ink
import com.ridesandshares.passenger.ui.theme.InkText
import com.ridesandshares.passenger.ui.theme.Muted
import com.ridesandshares.passenger.ui.theme.Paper
import com.ridesandshares.passenger.ui.theme.Rule

private data class DestinationChoice(
    val name: String,
    val miles: String,
    val minutes: String,
)

private val choices = listOf(
    DestinationChoice("Pike Place Market", "4.2", "12"),
    DestinationChoice("Seattle-Tacoma Airport", "18.4", "34"),
    DestinationChoice("Kerry Park", "6.8", "19"),
)

@Composable
fun DriverPhoneScreen(
    ride: RideDemo,
    onOpenTablet: () -> Unit,
    onOpenAdmin: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf("") }
    var miles by rememberSaveable { mutableStateOf("") }
    var arrival by rememberSaveable { mutableStateOf("") }
    var notice by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Rides and Shares",
                color = Paper.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Row {
                ScreenLink("Passenger tablet", "demo-tablet", onOpenTablet)
                ScreenLink("Admin", "demo-admin", onOpenAdmin)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(460.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Paper)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Driver",
                    color = InkText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Bluetooth",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                if (!ride.paired) {
                    Button(
                        onClick = {
                            ride.pair()
                            notice = "Paired. This phone can update the tablet."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pair-tablet"),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    ) {
                        Text("Pair tablet")
                    }
                } else {
                    Text(
                        text = "Paired with tablet ${RideDemo.TABLET_ID}",
                        color = InkText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Pick a destination",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                choices.forEach { choice ->
                    val selected = destination == choice.name
                    Text(
                        text = choice.name,
                        color = InkText,
                        fontSize = 18.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dest-${choice.name}")
                            .clickable {
                                destination = choice.name
                                miles = choice.miles
                                arrival = choice.minutes
                                notice = ""
                            }
                            .padding(vertical = 6.dp),
                    )
                }
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = InkText,
                    unfocusedTextColor = InkText,
                    focusedLabelColor = Muted,
                    unfocusedLabelColor = Muted,
                    cursorColor = InkText,
                    focusedBorderColor = InkText,
                    unfocusedBorderColor = Rule,
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Destination") },
                    singleLine = true,
                    colors = fieldColors,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = miles,
                        onValueChange = { miles = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Miles left") },
                        singleLine = true,
                        colors = fieldColors,
                    )
                    OutlinedTextField(
                        value = arrival,
                        onValueChange = { arrival = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Arrival") },
                        singleLine = true,
                        colors = fieldColors,
                    )
                }
                Button(
                    onClick = {
                        notice = sendRide(ride, destination, miles, arrival)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send-ride"),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                ) {
                    Text("Send to tablet")
                }
                if (notice.isNotBlank()) {
                    Text(text = notice, color = Muted, fontSize = 15.sp)
                }
            }
        }
    }
}

private fun sendRide(ride: RideDemo, destination: String, miles: String, arrival: String): String {
    if (!ride.paired) return "Pair the tablet first."
    val parsedMiles = miles.toDoubleOrNull()
    val parsedMinutes = arrival.toIntOrNull()
    if (destination.isBlank() || parsedMiles == null || parsedMiles <= 0.0 || parsedMinutes == null || parsedMinutes <= 0) {
        return "Enter a destination, the miles left, and the arrival in minutes."
    }
    ride.send(destination, parsedMiles, parsedMinutes)
    return "Sent to the tablet."
}

@Composable
private fun ScreenLink(label: String, tag: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Paper.copy(alpha = 0.8f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .testTag(tag)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
