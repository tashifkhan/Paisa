package codes.tashif.paisa.data

import android.content.Context
import androidx.room3.Room

/** The app's one database, in the app's private files. */
object PaisaDatabase {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder<AppDatabase>(context.applicationContext, DATABASE_NAME)
            .withPaisaDefaults()
            .build()
            .also { instance = it }
    }
}
