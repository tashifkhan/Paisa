package sh.taf.billi.data.repository

import sh.taf.billi.platform.formatDate
import sh.taf.billi.platform.nowMillis

/** Local time as stored in every createdAt / updatedAt column. */
internal fun nowIso(): String =
    formatDate(nowMillis(), "yyyy-MM-dd'T'HH:mm:ss")
