package sh.taf.billi.di

import sh.taf.billi.backup.AndroidCloudPlatform
import sh.taf.billi.backup.BackupPrefs
import sh.taf.billi.backup.CloudPlatform
import sh.taf.billi.backup.androidBackupPrefs
import sh.taf.billi.backup.AndroidBackupHooks
import sh.taf.billi.backup.BackupServiceHooks
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Android's side of backup and sync: encrypted prefs, WorkManager, Play sign-in, content files. */
val backupModule = module {
    single<BackupPrefs> { androidBackupPrefs(androidContext()) }
    single<CloudPlatform> { AndroidCloudPlatform(androidContext(), get()) }
    single<BackupServiceHooks> { AndroidBackupHooks(androidContext()) }
}
