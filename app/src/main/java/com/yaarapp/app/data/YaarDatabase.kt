package com.yaarapp.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [User::class, Shop::class, Product::class, CartItem::class, Interest::class, AdCampaign::class],
    version = 10,
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

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shops ADD COLUMN certificationPaidAt INTEGER")
                db.execSQL("ALTER TABLE shops ADD COLUMN certificationExpiresAt INTEGER")
            }
        }


        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN email TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shops ADD COLUMN nameChangedAt INTEGER")
                db.execSQL("ALTER TABLE products ADD COLUMN internalDiscussionEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE products ADD COLUMN whatsappDiscussionEnabled INTEGER NOT NULL DEFAULT 1")
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
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
