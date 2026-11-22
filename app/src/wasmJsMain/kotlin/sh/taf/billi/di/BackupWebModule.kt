package sh.taf.billi.di

import sh.taf.billi.backup.BackupPrefs
import sh.taf.billi.backup.CloudPlatform
import sh.taf.billi.backup.WebBackupHooks
import sh.taf.billi.backup.WebCloudPlatform
import sh.taf.billi.platform.LocalStorageSettingsStore
import sh.taf.billi.backup.BackupServiceHooks
import org.koin.dsl.module

/** The browser's side of backup and sync: localStorage settings, downloads, a CORS proxy for Nextcloud. */
val backupWebModule = module {
    single { BackupPrefs(LocalStorageSettingsStore(BackupPrefs.PREFS_NAME)) }
    single<CloudPlatform> { WebCloudPlatform(get()) }
    single<BackupServiceHooks> { WebBackupHooks(get()) }
}
