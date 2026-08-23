package com.yaarapp.app.util

import com.yaarapp.app.data.Country

/**
 * Construit le numéro WhatsApp au format standard demandé : "00" + indicatif pays +
 * numéro local, sans espaces (ex : Togo + "90000000" -> "0022890000000").
 * Ce format facilite les envois automatiques de messages vers WhatsApp.
 */
object PhoneFormat {

    fun localDigitsOnly(input: String): String = input.filter { it.isDigit() }

    fun formatWhatsapp(country: Country, localNumber: String): String {
        val digits = localDigitsOnly(localNumber)
        return "00${country.callingCode}$digits"
    }

    /** Un numéro local raisonnable pour l'Afrique de l'Ouest fait entre 8 et 9 chiffres. */
    fun isValidLocalNumber(localNumber: String): Boolean {
        val digits = localDigitsOnly(localNumber)
        return digits.length in 8..9
    }
}
