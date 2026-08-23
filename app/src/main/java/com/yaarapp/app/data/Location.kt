package com.yaarapp.app.data

/**
 * Les 4 pays de lancement de Yaar-App, avec leur drapeau (emoji) et leur
 * indicatif téléphonique international (sans le "+").
 *
 * Pour ajouter un futur pays : ajouter une entrée ici + sa liste de villes
 * dans [CityRepository.citiesByCountry]. Rien d'autre à changer, tout le
 * reste de l'application (inscription, recherche, tri par ville) s'adapte
 * automatiquement.
 */
enum class Country(
    val displayName: String,
    val flagEmoji: String,
    val callingCode: String
) {
    BENIN("Bénin", "\uD83C\uDDE7\uD83C\uDDEF", "229"),
    BURKINA_FASO("Burkina Faso", "\uD83C\uDDE7\uD83C\uDDEB", "226"),
    COTE_DIVOIRE("Côte d'Ivoire", "\uD83C\uDDE8\uD83C\uDDEE", "225"),
    TOGO("Togo", "\uD83C\uDDF9\uD83C\uDDEC", "228");

    /** Ex : "🇹🇬 Togo" — à afficher dans les listes de sélection. */
    val labelWithFlag: String get() = "$flagEmoji $displayName"
}

/**
 * Répertoire des villes/régions disponibles par pays, trié par ordre
 * alphabétique. Cette liste couvre les principales villes, chefs-lieux de
 * région/province et communes de chaque pays — elle n'est pas exhaustive à
 * 100 % (un pays peut compter plusieurs centaines de localités), mais elle
 * est volontairement large pour couvrir l'immense majorité des utilisateurs
 * dès le lancement. Il suffit d'ajouter une ville à la liste correspondante
 * ci-dessous pour qu'elle apparaisse automatiquement dans l'app (inscription
 * et filtres de recherche).
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
        Country.TOGO to listOf(
            "Adéta", "Aného", "Agbélouvé", "Amlamé", "Anié", "Atakpamé", "Badou",
            "Bafilo", "Bassar", "Blitta", "Cinkassé", "Dapaong", "Élavagnon",
            "Glidji", "Kandé", "Kanté", "Kara", "Kévé", "Kpalimé", "Kpéssi",
            "Lomé", "Mango", "Niamtougou", "Notsé", "Pagouda", "Sokodé",
            "Sotouboua", "Tabligbo", "Tchamba", "Tohoun", "Tsévié", "Vogan", "Wahala"
        )
    )

    fun citiesFor(country: Country): List<String> =
        citiesByCountry[country]?.sorted().orEmpty()
}
