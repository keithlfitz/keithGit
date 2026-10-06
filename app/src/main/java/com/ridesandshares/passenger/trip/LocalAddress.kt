package com.ridesandshares.passenger.trip

import java.net.Inet4Address
import java.net.NetworkInterface

fun localIpv4(): String? {
    val interfaces = try {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
    } catch (_: Exception) {
        return null
    }
    return interfaces
        .asSequence()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList().asSequence() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { !it.isLoopbackAddress }
        ?.hostAddress
}
