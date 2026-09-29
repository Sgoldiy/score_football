# Live feed: `uitslagen.live/footapi` (mission-uitslagen, Step 1)

> Discovery source: [`holoduke/livescore-mcp`](https://github.com/holoduke/livescore-mcp) is a thin MCP/SSE pass-through to this same upstream. We consume the JSON directly — no MCP, no key, no server of ours.

Base URL: `https://uitslagen.live/footapi`
Auth: **none**. Every request should append `lang=en&version=2800` (this is what the MCP server sends).
Politeness: unofficial API — send a custom `User-Agent`, 45–60 s min interval on live polling, 15 s timeouts, never parallel-blast (reuse the app's `Semaphore(4)` pattern), and degrade silently.

All times/dates in payloads are **GMT/UTC**. Dates are `dd/MM/yyyy`, times `HH:MM`.

---

## Endpoints

| Endpoint | Purpose | Verified |
|---|---|---|
| `GET fixtures/feed_livenow.json` | Matches in play **now** (plus today's games per tracked league), grouped `[{country, leagues:[{league, key, matches:[M]}]}]` | 2026-09-29, 4 countries / 21 rows |
| `GET fixtures/feed_matches_aggregated.json&date=DD/MM/YYYY&tzoffset=0` | Whole day, all countries (~104 KB, 34 countries, 187 rows on capture day) | 2026-09-29 |
| `GET matches/{id}.json&h2h=0|1` | Match detail: events, commentaries, lineups, stats, h2hMatches, venue, refereeId, shapes, isET/isOT, injurytime/injuryminute | 2026-09-29 (id 3951238) |
| `GET fixtures_v2/{leagueKey}_small.json` | League block: fixtures (finished + upcoming), **table**, **tableHistory**, **topscorers**, trophies, info.weeks | 2026-09-29 (ArgentinaPrimCMetro) |
| `GET fixtures_v2/{leagueKey}.json` | Same family, full variant — **returned 156 bytes (empty)** for our probe key while `_small` had 274 KB; prefer `_small` | 2026-09-29 |
| `GET search_v3&q=...&country=...` | Global search: `result[]` with `type` = `"t"` (team) or `"p"` (player); teams carry `id_gs`, popularity, embedded `matches[]`; players carry numeric `id_gs` | 2026-09-29 ("ajax") |
| `GET team_gs/{teamId}.json` | Team page: squad (id/number/age/position), stats record (win/draw/lost home/away), fixtures, venue, coach, trophies, shape | 2026-09-29 (34291) |
| `GET players/{playerId}.json` | Player page | 2026-09-29 (92076 via search id_gs) |
| `GET images/teams_gs/{teamId}.png` | Team logo | source-verified; not probed this step |

League keys look like `CountryNameLeagueName` without spaces (e.g. `IndiaIFAShield`, `ArgentinaPrimCMetro`, `NetherlandsEredivisie`) and come from search results / feed rows (`leagueKey`).

### Match id quirk
League-fixture rows can carry a **`_f` suffix** (e.g. `3949779_f`). The detail endpoint accepts the suffixed form; `UitslagenStatus.baseId()` strips it when a numeric id is needed.

---

## Status codes — evidence and the reliable interpretation

### What we captured on 2026-09-29 (probe ≈ 10:16–10:21 UTC)

| Match (UTC kickoff) | feed_livenow | detail status | score | events (min) | notes |
|---|---|---|---|---|---|
| 09:00 Mohun Bagan v NorthEast Utd | `57` | `59` | 1–0 | goal 49' | injurytime 2 / injuryminute 3; an earlier same-day capture showed `status "3"` at 0–0 |
| 09:30 France U20 v England U20 | `43` | `45` | 0–2 | goals 8', 8', 10', 16' | all 1H goals |
| 10:00 Brazil (WCQ) | `5` | `7` | 0–1 | goal 3' | 21' elapsed at probe |
| 10:00 J-League Cup (×6) | `15`/`14` | `17`/`16` | 0–0/0–3 | — | 21' elapsed at probe |
| 10:00 Portugal U20 v Switzerland U20 | `17` | `19` | 0–0 | — | 21' elapsed at probe |

Day feed (`feed_matches_aggregated.json` same day) uses **literal strings**: `FT` ×12, `Not Started` ×30, `Postp.` ×3, kickoff times, plus numeric codes for the two in-play matches. Team page fixtures likewise: `FT`, `Not Started`, `Cancelled`, and numeric codes while live.

### Finding: the numeric codes are NOT reliably decodable

- The **same match** reported different codes on two endpoints at the same moment (`43→45`, `57→59`, `5→7`, `14→16`, `15→17`, `17→19`).
- Earlier capture of 3951238 showed `status "3"` at 0–0 pre-49' while `45/57` appeared near/after 90' — consistent with "code ≈ current wall-clock minute or an internal progress marker", not a football state (1H/HT/FT).
- Therefore: **never derive football phase from the numeric code.**

### The reliable interpretation (implemented in `UitslagenStatus.kt`)

1. `HH:MM` status string → scheduled ("Not Started").
2. Literal words (`FT`/`AET`/`AP`, `Not Started`, `Postp.`, `Cancelled`, `Abandoned`, `Suspended`) → mapped phase.
3. Numeric code:
   - without kickoff time → treat as live, minute unknown ("In Play").
   - with kickoff time → **derive from elapsed UTC playing time**: ≤45' first half (minute = elapsed), 45–60' HT, 60–105' second half (minute = elapsed − 15), 105–110' stoppage grace (minute shows 90'), >110' finished ("FT").
4. Minute display convention (mirrors the site): after the break, minute = elapsed − 15 (the half-time break is included in elapsed wall-clock).

### Bonus finding for later missions

`fixtures_v2/{leagueKey}_small.json` embeds `table`, `tableHistory` and `topscorers` — **tables and top scorers are available from this source** after all. Not wired anywhere yet; relevant when the "remove tables/predictions" decision is revisited.

---

## Test resources

Captured payloads live in `app/src/test/resources/uitslagen/`:
`feed_livenow_1.json`, `feed_livenow_2.json`, `feed_matches_aggregated.json`, `fixtures_v2_full.json` (empty-block case), `fixtures_v2_small.json`, `matches_detail.json`, `team_gs.json`, `players.json`, `search_v3.json`, plus `probe.py` (the polite capture script — rerunnable for fresh evidence).

Decoder tests: `app/src/test/java/com/footballpluse/footballapp/data/remote/uitslagen/UitslagenStatusTest.kt` — pin the exact evidence rows above (codes 3/5/14/15/17/43/57 with their kickoffs) so a future capture that contradicts them shows up as a red test.
