package codes.tashif.paisa.di

import codes.tashif.paisa.backup.BackupPrefs
import codes.tashif.paisa.backup.CloudPlatform
import codes.tashif.paisa.backup.WebBackupHooks
import codes.tashif.paisa.backup.WebCloudPlatform
import codes.tashif.paisa.platform.LocalStorageSettingsStore
import codes.tashif.paisa.ui.backup.BackupServiceHooks
import org.koin.dsl.module

/** The browser's side of backup and sync: localStorage settings, downloads, a CORS proxy for Nextcloud. */
val backupWebModule = module {
    single { BackupPrefs(LocalStorageSettingsStore(BackupPrefs.PREFS_NAME)) }
    single<CloudPlatform> { WebCloudPlatform(get()) }
    single<BackupServiceHooks> { WebBackupHooks(get()) }
}
