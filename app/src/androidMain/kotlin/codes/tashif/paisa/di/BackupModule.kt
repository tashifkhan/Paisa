package codes.tashif.paisa.di

import codes.tashif.paisa.ui.backup.AndroidBackupService
import codes.tashif.paisa.ui.backup.BackupService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Backup and sync for BackupViewModel, backed by BackupManager and SyncManager. */
val backupModule = module {
    single<BackupService> { AndroidBackupService(androidContext(), get(), get()) }
}
