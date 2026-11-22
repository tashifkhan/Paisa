package sh.taf.billi.ai

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val aiIoDispatcher: CoroutineDispatcher = Dispatchers.IO
