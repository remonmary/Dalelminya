package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AdminLogEntity
import com.example.data.model.BannerEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.FavoriteEntity
import com.example.data.model.FeaturedNumberEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PhoneNumberCategoryEntity
import com.example.data.model.PhoneNumberEntity
import com.example.data.model.ReportEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.SettingEntity
import com.example.data.model.SyncQueueEntity
import com.example.data.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        PhoneNumberEntity::class,
        PhoneNumberCategoryEntity::class,
        FeaturedNumberEntity::class,
        BannerEntity::class,
        FavoriteEntity::class,
        ReportEntity::class,
        ReviewEntity::class,
        NotificationEntity::class,
        AdminLogEntity::class,
        SettingEntity::class,
        SyncQueueEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun directoryDao(): DirectoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nazlet_obeid_directory.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
