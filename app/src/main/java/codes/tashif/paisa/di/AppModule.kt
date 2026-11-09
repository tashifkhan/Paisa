package codes.tashif.paisa.di

import android.content.Context
import androidx.work.WorkManager
import codes.tashif.paisa.ai.LlmClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    /** LlmClient's constructor takes an optional OkHttpClient, so it's provided here. */
    @Provides
    @Singleton
    fun provideLlmClient(): LlmClient = LlmClient()
}
