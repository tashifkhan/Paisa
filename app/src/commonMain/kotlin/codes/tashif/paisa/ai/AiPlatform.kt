package codes.tashif.paisa.ai

import kotlinx.coroutines.CoroutineDispatcher

/** Where AI code does blocking disk and file work: Dispatchers.IO on Android, Default on the web. */
internal expect val aiIoDispatcher: CoroutineDispatcher
