package com.footballpluse.footballapp.data.mapper

import com.footballpluse.footballapp.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MapperNullSafetyTest {

    @Test
    fun `toPlayerProfileStatisticsResponse handles null player name and rating`() {
        val apiPlayer = ApiPlayer(
            player_key = 123,
            player_id = "123",
            player_name = null,
            player_rating = null,
            player_age = "25",
            player_type = "Forward",
            player_image = null,
            player_number = "10",
            player_country = "Brazil",
            player_injured = "0",
            player_is_captain = "0",
            player_shots_total = "50",
            player_goals = "10",
            player_assists = "5",
            player_birthdate = "1999-01-01",
            player_match_played = "20",
            player_substitute_out = "2",
            player_substitutes_on_bench = "1",
            player_goals_conceded = "0",
            player_saves = "0",
            player_passes = "500",
            player_key_passes = "30",
            player_passes_accuracy = "85",
            player_tackles = "10",
            player_blocks = "5",
            player_interceptions = "8",
            player_duels_total = "100",
            player_duels_won = "60",
            player_dribble_attempts = "40",
            player_dribble_succ = "25",
            player_fouls_committed = "15",
            player_yellow_cards = "2",
            player_red_cards = "0",
            player_pen_won = "1",
            player_pen_comm = "0",
            player_pen_scored = "1",
            player_pen_missed = "0",
            player_crosses_total = "5",
            player_clearances = "10",
            player_dispossesed = "2",
            player_inside_box_saves = "0",
            player_woordworks = "0"
        )

        val response = apiPlayer.toPlayerProfileStatisticsResponse()

        assertNotNull(response.player)
        assertEquals(null, response.player?.name)
        val stats = response.statistics?.firstOrNull()
        assertNotNull(stats)
        assertEquals("0.0", stats?.games?.rating)
    }

    @Test
    fun `toIntOr returns default value for null or malformed strings`() {
        val nullString: String? = null
        assertEquals(0, nullString.toIntOr())
        assertEquals(5, nullString.toIntOr(5))
        assertEquals(0, "abc".toIntOr())
        assertEquals(10, "10".toIntOr())
    }

    @Test
    fun `toStandingRecord handles null goals and played matches`() {
        val apiStanding = ApiStanding(
            standing_place = "1",
            team_id = "101",
            team_name = "Team A",
            standing_W = null,
            standing_D = null,
            standing_L = null,
            standing_total = null,
            overall_GF = null,
            overall_GA = null,
            standing_PTS = "3",
            standing_group = "",
            standing_place_type = "Promotion",
            league_id = "1",
            league_name = "League A",
            league_logo = null,
            country_name = "Country A",
            team_badge = null
        )

        val record = apiStanding.toStandingRecord()

        assertEquals(0, record.all?.played)
        assertEquals(null, record.goalsDiff)
        assertEquals(3, record.points)
    }
}
