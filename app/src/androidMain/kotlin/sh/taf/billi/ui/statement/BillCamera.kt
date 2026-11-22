package sh.taf.billi.ui.statement

import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.concurrent.Executors

/**
 * In-app still capture. CameraX binds the back camera (front if that's all
 * there is) and writes a JPEG through [ImageCapture], the same Camera2 path
 * Pixel Camera uses for stills. The checkerboard on an emulator is the
 * virtual sensor, not this screen.
 */
@Composable
internal fun BillCameraScreen(
    onDismiss: () -> Unit,
    onCaptured: (File) -> Unit,
    onFailed: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var failed by remember { mutableStateOf(false) }
    var shooting by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = runCatching { future.get() }.getOrNull()
            if (provider == null) {
                failed = true
                return@addListener
            }
            val selector = when {
                provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                else -> {
                    failed = true
                    return@addListener
                }
            }
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(95)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                imageCapture = capture
            } catch (_: Exception) {
                failed = true
            }
        }, mainExecutor)
        onDispose {
            runCatching { future.get().unbindAll() }
            captureExecutor.shutdown()
        }
    }

    LaunchedEffect(failed) {
        if (failed) onFailed()
    }

    Dialog(
        onDismissRequest = { if (!shooting) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = "Bill photo",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 18.dp)
                    .align(Alignment.TopCenter)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp)
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(if (shooting) Color.White.copy(alpha = 0.45f) else Color.White)
                    .clickable(enabled = imageCapture != null && !shooting) {
                        val capture = imageCapture ?: return@clickable
                        val file = BillPhoto.newCaptureFile(context)
                        shooting = true
                        capture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                        capture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            captureExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    mainExecutor.execute { onCaptured(file) }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    file.delete()
                                    mainExecutor.execute {
                                        shooting = false
                                        onFailed()
                                    }
                                }
                            }
                        )
                    }
            )
        }
    }
}
