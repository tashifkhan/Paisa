package codes.tashif.paisa.data.repository

import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.nowMillis

/** Local time as stored in every createdAt / updatedAt column. */
internal fun nowIso(): String =
    formatDate(nowMillis(), "yyyy-MM-dd'T'HH:mm:ss")
