package codes.tashif.paisa.ai

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** The browser has one thread; storage and file reads here are short. */
internal actual val aiIoDispatcher: CoroutineDispatcher = Dispatchers.Default
