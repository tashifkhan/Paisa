package codes.tashif.paisa.platform

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** 32 lowercase hex characters, the same shape as a dashless UUID. */
@OptIn(ExperimentalUuidApi::class)
fun randomHexId(): String = Uuid.random().toHexString()
