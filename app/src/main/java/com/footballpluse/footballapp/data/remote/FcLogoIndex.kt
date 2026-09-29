package com.footballpluse.footballapp.data.remote

import java.util.concurrent.ConcurrentHashMap

/**
 * The FootballCharts API identifies teams by NAME/slug (no integer ids) and serves
 * real logo URLs from its CDN. The rest of the app (Room, favorites, navigation)
 * works with stable Int team ids, so we:
 *  1. derive a deterministic Int id from the team name (FcTeamIds), and
 *  2. remember every logo URL the API hands us (team lists, goal timing, team pages)
 *     so mappers can attach real logos to matches/standings without extra calls.
 */
object FcTeamIds {

    private val nameToId = ConcurrentHashMap<String, Int>()
    private val idToName = ConcurrentHashMap<Int, String>()

    /**
     * Team ids persisted before the FootballCharts switch (previous provider) mapped
     * to the EXACT team names FC uses in its payloads. Seeding these keeps every
     * stored club id (Room, DataStore, Firestore) resolvable: a lookup of the id
     * returns the FC name, and FC payloads for that same name resolve back to the
     * same stable id. Teams the FC source does not cover are simply absent here and
     * surface the app's existing empty states.
     */
    private val LEGACY_ALIASES: Map<Int, String> = mapOf(
        // Premier League
        80 to "Manchester City", 141 to "Arsenal", 84 to "Liverpool", 88 to "Chelsea",
        102 to "Manchester Utd", 164 to "Tottenham", 3088 to "Aston Villa",
        3100 to "Newcastle", 3079 to "Brighton", 3429 to "Crystal Palace",
        3073 to "Everton", 3085 to "Fulham", 3086 to "Brentford",
        3089 to "Nottingham", 3071 to "Bournemouth", 3111 to "Sunderland",
        3103 to "Leeds",
        // La Liga
        76 to "Real Madrid", 97 to "Barcelona", 73 to "Atl. Madrid", 89 to "Sevilla",
        7272 to "Valencia", 7258 to "Ath Bilbao", 162 to "Villarreal",
        7261 to "Betis", 153 to "Real Sociedad", 7288 to "Getafe",
        7290 to "Celta Vigo", 7269 to "Osasuna", 7264 to "Rayo Vallecano",
        7259 to "Levante", 7275 to "Alaves", 7268 to "Espanyol", 7274 to "Elche",
        // Serie A
        79 to "Inter", 96 to "Juventus", 159 to "AC Milan", 152 to "Napoli",
        139 to "AS Roma", 93 to "Lazio", 85 to "Atalanta", 4974 to "Fiorentina",
        4983 to "Bologna", 4973 to "Torino", 4984 to "Udinese", 8239 to "Como",
        5010 to "Lecce", 4981 to "Cagliari", 4978 to "Parma", 4975 to "Sassuolo",
        4986 to "Genoa",
        // Bundesliga
        72 to "Bayern Munich", 92 to "Dortmund", 101 to "RB Leipzig",
        143 to "Bayer Leverkusen", 3945 to "Eintracht Frankfurt", 3933 to "Stuttgart",
        3962 to "Freiburg", 170 to "Hoffenheim", 3939 to "Mainz", 3934 to "Augsburg",
        3930 to "Werder Bremen", 3936 to "Union Berlin", 3912 to "Hamburger SV",
        3932 to "FC Koln", 3920 to "Paderborn",
        // Ligue 1
        100 to "PSG", 83 to "Marseille", 3817 to "Monaco", 3815 to "Lyon",
        160 to "Lille", 145 to "Nice", 91 to "Rennes", 3821 to "Lens",
        3794 to "Toulouse", 3823 to "Brest", 3818 to "Strasbourg",
        3796 to "Paris FC", 3804 to "Le Havre", 3827 to "Angers", 3797 to "Auxerre",
        3814 to "Lorient"
    )

    init {
        // Runs before any network flow touches this object, so legacy ids win.
        LEGACY_ALIASES.forEach { (id, name) -> seed(name, id) }
    }

    private fun seed(name: String, id: Int) {
        nameToId[name.lowercase().trim()] = id
        idToName[id] = name
    }

    private fun hashOf(key: String): Int {
        var h = 1125899906842597L
        for (c in key) h = 31L * h + c.code
        return (h xor (h ushr 32)).toInt()
    }

    /** Deterministic, stable Int id for a team name. Same name -> same id forever. */
    fun id(name: String?): Int {
        if (name.isNullOrBlank()) return 0
        val key = name.lowercase().trim()
        return nameToId.getOrPut(key) { hashOf(key) }.also { id -> idToName[id] = name.trim() }
    }

    fun name(id: Int): String? = idToName[id]
}

object FcLogoIndex {

    private val byName = ConcurrentHashMap<String, String>()
    private val bySlug = ConcurrentHashMap<String, String>()

    fun put(leagueSlug: String?, teamName: String?, logoUrl: String?) {
        if (teamName.isNullOrBlank() || logoUrl.isNullOrBlank()) return
        byName[teamName.lowercase().trim()] = logoUrl
        val teamSlug = ApiConfig.teamSlug(teamName)
        if (leagueSlug != null && teamSlug != null) {
            bySlug["$leagueSlug/$teamSlug"] = logoUrl
        }
    }

    fun forName(teamName: String?): String? {
        if (teamName.isNullOrBlank()) return null
        return byName[teamName.lowercase().trim()]
    }

    fun forSlug(leagueSlug: String?, teamSlug: String?): String? {
        if (leagueSlug == null || teamSlug == null) return null
        return bySlug["$leagueSlug/$teamSlug"]
    }

    /** Seed known slugs up-front so first-launch screens already show logos. */
    fun seedDefaultLogos() {
        // Nothing hardcoded: logos arrive from /teams/ + /goal-timing/ responses and
        // are remembered here for the rest of the session.
    }
}
