package codes.tashif.paisa.data.sql

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Wall clock in milliseconds since the epoch. */
@OptIn(ExperimentalTime::class)
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** 32 lowercase hex characters, the same shape as a dashless UUID. */
@OptIn(ExperimentalUuidApi::class)
fun randomHexId(): String = Uuid.random().toHexString()
