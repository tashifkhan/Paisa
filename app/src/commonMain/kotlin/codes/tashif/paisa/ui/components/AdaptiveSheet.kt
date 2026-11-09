package codes.tashif.paisa.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import codes.tashif.paisa.ui.app.WindowWidth
import codes.tashif.paisa.ui.app.windowWidth
import codes.tashif.paisa.ui.platform.PlatformBackHandler

/** True when sheets open as centered dialogs, on tablets in landscape and desktop browsers. */
@Composable
fun sheetsAsDialogs(): Boolean = windowWidth() >= WindowWidth.EXPANDED

/**
 * A bottom sheet on phones and narrow windows. On wide windows a sheet
 * stretched across the bottom of a monitor reads badly and is far from the
 * mouse, so the same content opens in a centered dialog instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!sheetsAsDialogs()) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            containerColor = containerColor,
            content = content
        )
        return
    }
    val maxHeight = with(LocalDensity.current) {
        (LocalWindowInfo.current.containerSize.height * 0.96f).toDp()
    }
    // Esc and the browser's back button close the dialog, not the screen under it.
    PlatformBackHandler(onBack = onDismissRequest)
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .widthIn(min = 420.dp, max = 560.dp)
                .heightIn(max = maxHeight),
            shape = MaterialTheme.shapes.extraLarge,
            color = containerColor,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(top = 24.dp), content = content)
        }
    }
}
