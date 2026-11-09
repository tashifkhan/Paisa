package codes.tashif.paisa.di

import codes.tashif.paisa.backup.AndroidCloudPlatform
import codes.tashif.paisa.backup.BackupPrefs
import codes.tashif.paisa.backup.CloudPlatform
import codes.tashif.paisa.backup.androidBackupPrefs
import codes.tashif.paisa.ui.backup.AndroidBackupHooks
import codes.tashif.paisa.ui.backup.BackupServiceHooks
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Android's side of backup and sync: encrypted prefs, WorkManager, Play sign-in, content files. */
val backupModule = module {
    single<BackupPrefs> { androidBackupPrefs(androidContext()) }
    single<CloudPlatform> { AndroidCloudPlatform(androidContext(), get()) }
    single<BackupServiceHooks> { AndroidBackupHooks(androidContext()) }
}
