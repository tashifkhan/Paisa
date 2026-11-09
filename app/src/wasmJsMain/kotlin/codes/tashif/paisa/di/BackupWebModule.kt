package codes.tashif.paisa.di

import codes.tashif.paisa.ui.backup.BackupService
import codes.tashif.paisa.ui.backup.BackupViewModel
import codes.tashif.paisa.ui.backup.WebBackupService
import codes.tashif.paisa.ui.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Local `.paisa` file backup on the web, plus the onboarding viewmodel.
 * Needs AppDatabase, SettingsRepository and AppLockGuard bound elsewhere.
 */
val backupWebModule = module {
    single<BackupService> { WebBackupService(get()) }
    viewModelOf(::BackupViewModel)
    viewModelOf(::OnboardingViewModel)
}
