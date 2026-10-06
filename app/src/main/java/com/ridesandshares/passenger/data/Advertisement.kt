package com.ridesandshares.passenger.data

import kotlinx.serialization.Serializable

/**
 * One advertisement in the passenger slideshow.
 *
 * The JSON catalog uses these exact names so a later advertiser portal can
 * replace assets/ads.json without changing the app.
 */
@Serializable
data class Advertisement(
    val id: String,
    val businessName: String,
    val tagline: String,
    val image: String,
    val infoUrl: String,
) {
    fun problems(): List<String> {
        val who = id.ifBlank { "(missing id)" }
        val issues = mutableListOf<String>()
        if (id.isBlank()) issues += "$who: id is required"
        if (businessName.isBlank()) issues += "$who: businessName is required"
        if (tagline.isBlank()) issues += "$who: tagline is required"
        if (image.isBlank()) issues += "$who: image is required"
        val hasHost = infoUrl.startsWith("https://") && infoUrl.removePrefix("https://").isNotBlank()
        if (!hasHost) issues += "$who: infoUrl must be an https URL"
        return issues
    }
}

sealed interface Catalog {
    data class Ready(val ads: List<Advertisement>) : Catalog
    data class Invalid(val problems: List<String>) : Catalog

    companion object {
        fun from(ads: List<Advertisement>): Catalog {
            val problems = ads.flatMap { it.problems() }
            return if (problems.isEmpty()) Ready(ads) else Invalid(problems)
        }
    }
}
