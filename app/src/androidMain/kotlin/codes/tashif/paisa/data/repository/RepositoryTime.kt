package codes.tashif.paisa.data.repository

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local time as stored in every createdAt / updatedAt column. */
internal fun nowIso(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
