package com.yaarapp.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [User::class, Shop::class, Product::class, CartItem::class, Interest::class, AdCampaign::class],
    version = 5,
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
        @Volatile
        private var INSTANCE: YaarDatabase? = null

        fun getInstance(context: Context): YaarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    YaarDatabase::class.java,
                    "yaar_app.db"
                )
                    // Version 2 a introduit pays/ville/sexe et l'expiration des produits ;
                    // version 3 a ajouté logo/description/catégories de boutique, la
                    // promotion et les notifications "intéressé" ; version 4 a remplacé
                    // le système de forfaits par la capacité de produits payante (5→20)
                    // et les campagnes publicitaires calculées, plus la certification de
                    // boutique ; version 5 ajoute remoteId (synchronisation Firestore).
                    // Comme il s'agit d'une base locale de démonstration (pas de
                    // données critiques côté serveur), on repart d'une base propre au lieu
                    // d'écrire une migration détaillée. À remplacer par une vraie migration
                    // Room (ou par la bascule vers Firestore, voir /BACKEND_FIREBASE.md)
                    // avant toute mise en production avec de vraies données utilisateurs.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
