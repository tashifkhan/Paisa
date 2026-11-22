package sh.taf.billi.data

import android.content.Context
import androidx.room3.Room

/** The app's one database, in the app's private files. */
object BilliDatabase {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder<AppDatabase>(context.applicationContext, DATABASE_NAME)
            .withBilliDefaults()
            .build()
            .also { instance = it }
    }
}
