package codes.tashif.paisa.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.platform.PlatformBackHandler
import codes.tashif.paisa.ui.sms.smsImportSupported
import codes.tashif.paisa.ui.theme.spacing
import org.koin.compose.viewmodel.koinViewModel

/** The steps of first-run setup. SMS only shows where SMS import works. */
internal enum class SetupStep { WELCOME, CURRENCY, LOOK, SMS, DONE }

private val setupSteps: List<SetupStep> =
    SetupStep.entries.filter { it != SetupStep.SMS || smsImportSupported }

/**
 * First-run setup. A welcome, then the few things worth deciding up front:
 * currency, theme, and on Android whether to read bank SMS. It ends with
 * shortcuts to the next things people usually do. Each step saves as it
 * goes, so skipping out keeps whatever was already picked.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit = {},
    onRestoreBackup: (() -> Unit)? = null,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val haptics = rememberHaptics()
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = setupSteps[index]
    val smsPermissions = if (smsImportSupported) rememberSetupSmsPermissions() else null

    val finish: (String?) -> Unit = { destination ->
        haptics.confirm()
        viewModel.completeOnboarding(destination)
        onFinished()
    }
    val next: () -> Unit = {
        haptics.click()
        if (index < setupSteps.lastIndex) index++ else finish(null)
    }
    val back: () -> Unit = { if (index > 0) index-- }

    PlatformBackHandler(enabled = index > 0, onBack = back)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
            ) {
                if (step != SetupStep.WELCOME) {
                    SetupTopBar(
                        progress = index.toFloat() / setupSteps.lastIndex,
                        onBack = back,
                        onSkip = if (step != SetupStep.DONE) ({ finish(null) }) else null
                    )
                }

                AnimatedContent(
                    targetState = index,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally { w -> if (forward) w / 4 else -w / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { w -> if (forward) -w / 4 else w / 4 } + fadeOut())
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    label = "setup_step"
                ) { shown ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = MaterialTheme.spacing.large)
                            .padding(bottom = MaterialTheme.spacing.medium)
                    ) {
                        when (setupSteps[shown]) {
                            SetupStep.WELCOME -> WelcomeStep()
                            SetupStep.CURRENCY -> CurrencyStep(
                                selected = settings?.currency,
                                onSelect = {
                                    haptics.tick()
                                    viewModel.setCurrency(it)
                                }
                            )
                            SetupStep.LOOK -> LookStep(
                                themeMode = settings?.themeMode ?: "system",
                                paletteId = settings?.colorPalette,
                                onThemeMode = viewModel::setThemeMode,
                                onPalette = viewModel::setPalette
                            )
                            SetupStep.SMS -> smsPermissions?.let { SmsStep(it) }
                            SetupStep.DONE -> DoneStep(onOpen = { finish(it) })
                        }
                    }
                }

                SetupButtons(
                    step = step,
                    // Until SMS access is on, the step's own button is the main one.
                    quiet = step == SetupStep.SMS && smsPermissions?.granted == false,
                    onNext = next,
                    onRestoreBackup = onRestoreBackup?.let { restore ->
                        {
                            haptics.click()
                            restore()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SetupTopBar(progress: Float, onBack: () -> Unit, onSkip: (() -> Unit)?) {
    val shown by animateFloatAsState(progress, label = "setup_progress")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        LinearProgressIndicator(
            progress = { shown },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape),
            drawStopIndicator = {}
        )
        Box(Modifier.widthIn(min = 72.dp), contentAlignment = Alignment.CenterEnd) {
            if (onSkip != null) {
                TextButton(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}

@Composable
private fun SetupButtons(
    step: SetupStep,
    quiet: Boolean,
    onNext: () -> Unit,
    onRestoreBackup: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.large)
            .padding(bottom = MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
    ) {
        if (quiet) {
            OutlinedButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text("Not now", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = when (step) {
                        SetupStep.WELCOME -> "Get started"
                        SetupStep.DONE -> "Go to Paisa"
                        else -> "Continue"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (step == SetupStep.WELCOME && onRestoreBackup != null) {
            OutlinedButton(
                onClick = onRestoreBackup,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text("I have a backup", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** A step's heading and the line under it. */
@Composable
internal fun StepHeading(title: String, body: String) {
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(MaterialTheme.spacing.smaller))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(MaterialTheme.spacing.large))
}

/** An icon in a tonal rounded square, used down the welcome and done steps. */
@Composable
internal fun StepIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    Surface(shape = MaterialTheme.shapes.medium, color = container, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.5f))
        }
    }
}
