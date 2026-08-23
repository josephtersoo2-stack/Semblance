package app.semblance.di

import android.content.Context
import androidx.room.Room
import app.semblance.data.local.AppDb
import app.semblance.data.local.dao.EventDao
import app.semblance.data.local.dao.ProfileDao
import app.semblance.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDb(
        @ApplicationContext context: Context
    ): AppDb {
        return Room.databaseBuilder(
            context,
            AppDb::class.java,
            "semblance.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideProfileDao(db: AppDb): ProfileDao = db.profileDao()

    @Provides
    fun provideTaskDao(db: AppDb): TaskDao = db.taskDao()

    @Provides
    fun provideEventDao(db: AppDb): EventDao = db.eventDao()
}
