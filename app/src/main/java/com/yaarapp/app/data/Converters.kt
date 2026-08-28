package com.yaarapp.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromCountry(country: Country): String = country.name

    @TypeConverter
    fun toCountry(value: String): Country = Country.valueOf(value)

    @TypeConverter
    fun fromInterestStatus(status: InterestStatus): String = status.name

    @TypeConverter
    fun toInterestStatus(value: String): InterestStatus = InterestStatus.valueOf(value)

    @TypeConverter
    fun fromCertificationStatus(status: CertificationStatus): String = status.name

    @TypeConverter
    fun toCertificationStatus(value: String): CertificationStatus = CertificationStatus.valueOf(value)

    /** Utilisé pour Shop.categories (liste de catégories choisies par le vendeur, max 3). */
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString("‖")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("‖").filter { it.isNotBlank() }
}
