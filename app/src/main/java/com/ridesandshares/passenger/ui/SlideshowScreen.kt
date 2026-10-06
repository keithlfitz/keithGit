package com.ridesandshares.passenger.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesandshares.passenger.R
import com.ridesandshares.passenger.data.Advertisement
import com.ridesandshares.passenger.data.Catalog
import com.ridesandshares.passenger.slideshow.SlideshowTiming
import com.ridesandshares.passenger.ui.theme.Ink
import com.ridesandshares.passenger.ui.theme.InkText
import com.ridesandshares.passenger.ui.theme.Muted
import com.ridesandshares.passenger.ui.theme.Paper
import com.ridesandshares.passenger.ui.theme.Rule
import java.io.IOException

@Composable
fun SlideshowScreen(catalog: Catalog) {
    when (catalog) {
        is Catalog.Invalid -> StatusMessage(
            title = stringResource(R.string.ads_failed),
            detail = catalog.problems.joinToString("\n"),
        )
        is Catalog.Ready -> {
            if (catalog.ads.isEmpty()) {
                StatusMessage(title = stringResource(R.string.empty_ads))
            } else {
                Player(catalog.ads)
            }
        }
    }
}

@Composable
private fun Player(ads: List<Advertisement>) {
    var cycle by remember { mutableIntStateOf(0) }
    val page = cycle.floorMod(ads.size)
    val ad = ads[page]
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        Crossfade(
            targetState = ad,
            animationSpec = tween(durationMillis = 400),
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight(),
            label = "creative",
        ) { current ->
            CreativePane(current)
        }
        OfferRail(
            ad = ad,
            cycle = cycle,
            page = page,
            count = ads.size,
            onAdvance = { cycle += 1 },
            modifier = Modifier
                .weight(0.75f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun CreativePane(ad: Advertisement) {
    val context = LocalContext.current
    val image = remember(ad.image) { decodeAsset(context, ad.image) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fallbackColor(ad.id)),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = stringResource(R.string.ad_image_description, ad.businessName),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = ad.businessName,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontSize = 48.sp,
            )
        }
    }
}

@Composable
private fun OfferRail(
    ad: Advertisement,
    cycle: Int,
    page: Int,
    count: Int,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.background(Paper),
    ) {
        val qrSide = minOf(maxWidth - 72.dp, maxHeight * 0.5f).coerceAtMost(440.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.brand_eyebrow).uppercase(),
                color = Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = ad.businessName,
                    color = InkText,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = ad.tagline,
                    color = Muted,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.weight(1f))
            QrCode(
                contents = ad.infoUrl,
                contentDescription = stringResource(R.string.qr_description, ad.businessName),
                modifier = Modifier.size(qrSide),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.scan_for_details),
                color = InkText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(18.dp))
            SlideProgress(cycle = cycle, page = page, count = count, onAdvance = onAdvance)
        }
    }
}

@Composable
private fun SlideProgress(cycle: Int, page: Int, count: Int, onAdvance: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(cycle, count) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = SlideshowTiming.ADVANCE_EVERY_MS,
                easing = LinearEasing,
            ),
        )
        onAdvance()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = InkText,
            trackColor = Rule,
        )
        Text(
            text = stringResource(R.string.slide_position, page + 1, count),
            color = Muted,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun StatusMessage(title: String, detail: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = Paper,
            fontFamily = FontFamily.Serif,
            fontSize = 40.sp,
        )
        if (!detail.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(text = detail, color = Paper.copy(alpha = 0.75f), fontSize = 18.sp)
        }
    }
}

private fun decodeAsset(context: Context, path: String): ImageBitmap? {
    return try {
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    } catch (_: IOException) {
        null
    }
}

private fun fallbackColor(id: String): Color {
    val hue = (id.hashCode() and 0xFFFF) % 360
    return Color.hsl(hue.toFloat(), 0.28f, 0.24f)
}

private fun Int.floorMod(divisor: Int): Int {
    val remainder = this % divisor
    return if (remainder < 0) remainder + divisor else remainder
}
