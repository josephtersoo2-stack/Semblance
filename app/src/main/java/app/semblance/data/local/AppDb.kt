package app.semblance.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import app.semblance.data.local.dao.EventDao
import app.semblance.data.local.dao.ProfileDao
import app.semblance.data.local.dao.TaskDao
import app.semblance.data.local.entity.EventEntity
import app.semblance.data.local.entity.ProfileEntity
import app.semblance.data.local.entity.TaskEntity

@Database(
    entities = [
        ProfileEntity::class,
        TaskEntity::class,
        EventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
}
