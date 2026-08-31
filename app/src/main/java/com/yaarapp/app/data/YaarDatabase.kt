package com.yaarapp.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [User::class, Shop::class, Product::class, CartItem::class, Interest::class, AdCampaign::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class YaarDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun shopDao(): ShopDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun interestDao(): InterestDao
    abstract fun adCampaignDao(): AdCampaignDao

    companion object {
        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN firebaseUid TEXT")
                db.execSQL("ALTER TABLE shops ADD COLUMN ownerUid TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN availableCities TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN ownerUid TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN shopRemoteId TEXT")
            }
        }

        @Volatile
        private var INSTANCE: YaarDatabase? = null

        fun getInstance(context: Context): YaarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    YaarDatabase::class.java,
                    "yaar_app.db"
                )
                    // Version 7 ajoute l'identité Firebase stable du compte, le ciblage
                    // multi-villes des produits et les identifiants Firebase des boutiques.
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
