package codes.tashif.paisa.ui.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ai.provider.BuiltInProviders

/**
 * A provider's avatar: its real logo on a tonal circle, ringed in the accent
 * once a key is saved, like Patchwork on Android. Custom servers get a server
 * glyph and RelayRoom a router.
 */
@Composable
internal fun ProviderAvatar(
    providerId: String,
    modifier: Modifier = Modifier,
    custom: Boolean = false,
    configured: Boolean = false,
    size: Dp = 44.dp
) {
    val mono = MaterialTheme.colorScheme.onSurface
    val logo = remember(providerId, custom, mono) {
        if (custom) null else ProviderLogos.forProvider(providerId, mono)
    }
    val ring = if (configured) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(size)
            .then(ring)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val glyph = size * 0.52f
        if (logo != null) {
            Image(imageVector = logo, contentDescription = null, modifier = Modifier.size(glyph))
        } else {
            Icon(
                imageVector = when {
                    custom -> Icons.Rounded.Dns
                    providerId == BuiltInProviders.RELAYROOM -> Icons.Rounded.Router
                    else -> Icons.Rounded.Api
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(glyph)
            )
        }
    }
}

/**
 * The logo to show next to a model. RelayRoom models are named
 * "provider/model", so they show the upstream provider's logo.
 */
internal fun modelLogoProvider(providerId: String, modelId: String): String =
    if (providerId == BuiltInProviders.RELAYROOM && modelId.contains('/')) {
        modelId.substringBefore('/')
    } else {
        providerId
    }
