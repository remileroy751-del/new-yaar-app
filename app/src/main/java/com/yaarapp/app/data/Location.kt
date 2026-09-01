package com.yaarapp.app.data

/** Pays disponibles dans Yaar-App, avec drapeau et indicatif international. */
enum class Country(
    val displayName: String,
    val flagEmoji: String,
    val callingCode: String
) {
    BENIN("Bénin", "\uD83C\uDDE7\uD83C\uDDEF", "229"),
    BURKINA_FASO("Burkina Faso", "\uD83C\uDDE7\uD83C\uDDEB", "226"),
    COTE_DIVOIRE("Côte d'Ivoire", "\uD83C\uDDE8\uD83C\uDDEE", "225"),
    MALI("Mali", "\uD83C\uDDF2\uD83C\uDDED", "223"),
    NIGER("Niger", "\uD83C\uDDF3\uD83C\uDDEA", "227"),
    SENEGAL("Sénégal", "\uD83C\uDDF8\uD83C\uDDF3", "221"),
    TOGO("Togo", "\uD83C\uDDF9\uDDEC", "228");

    val labelWithFlag: String get() = "$flagEmoji $displayName"
}

/**
 * Villes disponibles par pays. L'application trie toujours ces listes par ordre
 * alphabétique avant affichage. Les listes sont volontairement centrées sur les
 * principales villes et communes urbaines utiles pour une marketplace.
 */
object CityRepository {
    private val citiesByCountry: Map<Country, List<String>> = mapOf(
        Country.BENIN to listOf(
            "Abomey", "Abomey-Calavi", "Adjarra", "Adjohoun", "Aplahoué", "Athiémé",
            "Avrankou", "Banikoara", "Bassila", "Bembèrèkè", "Bohicon", "Bopa",
            "Cotonou", "Comè", "Cové", "Djougou", "Dogbo", "Grand-Popo", "Kandi",
            "Kétou", "Kouandé", "Lokossa", "Malanville", "Natitingou", "Nikki",
            "Ouidah", "Ouinhi", "Parakou", "Pobè", "Porto-Novo", "Sakété", "Savalou",
            "Savè", "Ségbana", "Sèmè-Kpodji", "Tanguiéta", "Tchaourou", "Toffo",
            "Tori-Bossito", "Zagnanado", "Za-Kpota", "Zè"
        ),
        Country.BURKINA_FASO to listOf(
            "Banfora", "Batié", "Bobo-Dioulasso", "Boromo", "Boulsa", "Dédougou",
            "Diapaga", "Diébougou", "Djibo", "Dori", "Fada N'Gourma", "Gaoua",
            "Garango", "Gorom-Gorom", "Gourcy", "Houndé", "Kaya", "Kombissiri",
            "Koudougou", "Koupéla", "Léo", "Manga", "Nouna", "Ouagadougou",
            "Ouahigouya", "Orodara", "Pama", "Pô", "Réo", "Sebba", "Solenzo",
            "Tenkodogo", "Titao", "Toma", "Yako", "Ziniaré", "Zorgo"
        ),
        Country.COTE_DIVOIRE to listOf(
            "Abengourou", "Abidjan", "Aboisso", "Adzopé", "Agboville", "Agnibilékrou",
            "Bingerville", "Bondoukou", "Bouaké", "Bouna", "Boundiali", "Dabou",
            "Daloa", "Danané", "Daoukro", "Divo", "Duékoué", "Ferkessédougou",
            "Gagnoa", "Grand-Bassam", "Guiglo", "Issia", "Katiola", "Korhogo",
            "Man", "Mankono", "Odienné", "Oumé", "San-Pédro", "Sassandra",
            "Séguéla", "Soubré", "Tabou", "Tiassalé", "Toumodi", "Yamoussoukro"
        ),
        Country.MALI to listOf(
            "Bamako", "Banamba", "Bandiagara", "Bla", "Bougouni", "Bourem", "Dioïla",
            "Diré", "Djenné", "Douentza", "Gao", "Goundam", "Kadiolo", "Kangaba",
            "Kati", "Kayes", "Kidal", "Kita", "Kolokani", "Koulikoro", "Koutiala",
            "Markala", "Mopti", "Nioro du Sahel", "San", "Ségou", "Sikasso",
            "Tenenkou", "Tombouctou", "Yanfolila", "Yorosso"
        ),
        Country.NIGER to listOf(
            "Agadez", "Arlit", "Balleyara", "Birni N'Konni", "Diffa", "Dosso", "Filingué",
            "Gaya", "Gouré", "Guidan Roumdji", "Illéla", "Keita", "Loga", "Magaria",
            "Maradi", "Madaoua", "Mayahi", "Mirriah", "Niamey", "Ouallam", "Tahoua",
            "Tchintabaraden", "Tessaoua", "Tillabéri", "Téra", "Torodi", "Zinder"
        ),
        Country.SENEGAL to listOf(
            "Bargny", "Dakar", "Diourbel", "Fatick", "Guédiawaye", "Joal-Fadiouth",
            "Kaffrine", "Kaolack", "Kédougou", "Kolda", "Louga", "Mbacké", "Matam",
            "Mbour", "Nioro du Rip", "Pikine", "Podor", "Richard-Toll", "Rufisque",
            "Saint-Louis", "Sédhiou", "Tambacounda", "Thiadiaye", "Thiès", "Tivaouane",
            "Touba", "Ziguinchor"
        ),
        Country.TOGO to listOf(
            "Adéta", "Agbélouvé", "Amlamé", "Aného", "Anié", "Atakpamé", "Badou",
            "Bafilo", "Bassar", "Blitta", "Cinkassé", "Dapaong", "Élavagnon",
            "Glidji", "Kandé", "Kanté", "Kara", "Kévé", "Kpalimé", "Kpéssi",
            "Lomé", "Mango", "Niamtougou", "Notsé", "Pagouda", "Sokodé",
            "Sotouboua", "Tabligbo", "Tchamba", "Tohoun", "Tsévié", "Vogan", "Wahala"
        )
    )

    fun citiesFor(country: Country): List<String> = citiesByCountry[country]?.sorted() ?: emptyList()
}
