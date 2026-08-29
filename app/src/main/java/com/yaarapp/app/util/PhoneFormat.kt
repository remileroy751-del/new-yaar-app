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

    /**
     * Longueur d'un numéro local en Afrique de l'Ouest : la plupart des pays sont à
     * 8 chiffres, mais la Côte d'Ivoire est passée à 10 chiffres après l'indicatif
     * depuis 2021 — on accepte donc de 8 à 10 chiffres pour rester compatible avec
     * tous les pays proposés, sans bloquer un numéro ivoirien valide à 10 chiffres.
     */
    fun isValidLocalNumber(localNumber: String): Boolean {
        val digits = localDigitsOnly(localNumber)
        return digits.length in 8..10
    }
}
