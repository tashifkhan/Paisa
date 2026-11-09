package codes.tashif.paisa.ui.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

@Composable
internal fun AmbientBlobs(pageIndex: Int) {
    val primary = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    val secondary = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    val tertiary = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.40f)
    // Rotate which blob is “loudest” per page for motion without heavy animation
    val rot by animateFloatAsState(
        targetValue = pageIndex * 12f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "blob_rot"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(x = (-28).dp, y = 56.dp)
                .size(110.dp)
                .rotate(rot)
                .clip(
                    RoundedCornerShape(
                        topStart = 56.dp,
                        topEnd = 28.dp,
                        bottomStart = 32.dp,
                        bottomEnd = 56.dp
                    )
                )
                .background(primary)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 32.dp, y = 28.dp)
                .size(88.dp)
                .rotate(-rot * 0.6f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 44.dp,
                        bottomStart = 44.dp,
                        bottomEnd = 14.dp
                    )
                )
                .background(secondary)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-12).dp, y = (-96).dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(tertiary)
        )
    }
}
