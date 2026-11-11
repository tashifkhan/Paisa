package codes.tashif.paisa.ai

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val aiIoDispatcher: CoroutineDispatcher = Dispatchers.IO
