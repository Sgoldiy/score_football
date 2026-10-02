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

League keys look like `CountryNameLeagueName` without spaces (e.g. `IndiaIFAShield`, `ArgentinaPrimCMetro`, `NetherlandsEredivisie`). The reliable discovery source is `search_v3`: query a famous club of the league and read the `leagueKey` embedded in its team result (feed rows also carry `leagueKey`, but their numeric `leagueid` is **not** the app's legacy id and must never be used for resolution).

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
Mapper tests: `UitslagenMapperTest.kt` parses the captured payloads and verifies the legacy-model mapping end to end.

---

## Integration status (mission-uitslagen)

The app's single data source is now `UitslagenAdapter` (`data/remote/uitslagen/`), which implements the legacy `ApiService` interface over this upstream. Hilt wiring is in `di/NetworkModule.kt` (global `lang`/`version` params + `User-Agent: FootballPulse/1.0` via interceptor; 15 s timeouts).

### Request-reducing cache (`ApiCacheInterceptor`, 2026-10-02)

The upstream is unofficial with no published quota, so every response passes through a request-reducing cache (added **after** the params interceptor so keys are final URLs, and unit-pinned by `ApiCacheInterceptorTest` — 12 tests):

1. **Fresh serve** — responses younger than their TTL are answered without a network call. TTLs per endpoint class: live feed 45 s (below the 60 s politeness window, so the app's pollers coalesce to ≤1 upstream hit/min), day feeds 60 s, everything else 120 s.
2. **Stale-while-revalidate** — past the TTL the cached copy is served immediately while a single-threaded daemon executor refreshes it once in the background; concurrent callers share that one refresh.
3. **Coalescing** — simultaneous cold callers of the same URL share one network response.
4. **Rate-limit cooldown** — an observed 429 (or a rate-limit error body) puts that URL on a 60 s cooldown during which only cache answers.
5. **Failure fallback** — a network error serves the last known copy instead of a blank screen.
6. **Disk persistence** — entries are written under `filesDir/http_cache/` (atomic tmp→rename, MD5-named files, newest-256 prune, 24 h stale horizon) so a cold app start serves from disk instead of re-fetching.

Error envelopes (`{"error":{...}}`) and non-JSON bodies are never cached. The cache is Android-import-free and takes an injectable `Clock`, which is why its whole behavior is JVM-tested. The FootballCharts layer, its Bearer auth and the apiv3 badge-CDN fallback were deleted.

Coverage per legacy call:

| ApiService method | Source |
|---|---|
| `getLivescore` | `feed_livenow.json` — real scores/status/minute |
| `getEvents` | league block fixtures / team-page fixtures / day feed (window-filtered) |
| `getEventById` | `matches/{id}.json` — incl. goals, cards, venue, formations |
| `getStandings` | league block `table` (real positions, points, form) |
| `getTopScorers` | league block `topscorers.tournaments[].players` — **populated** (verified live 2026-09-30: EPL/LaLiga carry 50 players each); the older `topscorers.tournaments[].topscorers` list key remains a lenient fallback |
| `getLineups` / `getMatchStatistics` | match detail `lineups` / `stats` |
| `getHeadToHead` | team-page fixtures; mutual meetings matched by numeric team ids first (name-normalized fallback) |
| `getTeams` / `getPlayers` | league block / `team_gs` / `players` / `search_v3` |
| `getCountries` / `getLeagues` | discovered live from the day feed |
| `getOdds` / `getPredictions` | empty — no upstream equivalent; UI removed |

Removed features (no upstream data): season projection, luck/xPts, model track-record tab, club advanced stats (xG/possession), odds, FC goal-timing bands (Stats tab bands are zeros).

---

## Live verification of the whole adapter (2026-09-30)

`LiveAdapterBehaviorTest` (ignored by default; run with `--tests "*.LiveAdapterBehaviorTest"`) drives the real `UitslagenAdapter` — production Retrofit/Moshi/interceptors — through all nine app flows against the live upstream. All 9 green, with field-completeness assertions:

| Flow | Evidence |
|---|---|
| Home feed (live + day, ±1 day) | 210 rows; 0 missing ids/teams/date/status/country/league/badges |
| Live scores | rows flagged `match_live=1` with scores + minute |
| LaLiga standings (id 302) | 20/20 rows, position 1 first, every field incl. badge present |
| LaLiga events (±200 d) | 320 rows, core fields complete |
| Match detail (finished FT match) | status FT, score, stadium; lineups 11 v 11 with names/numbers; H2H stats mapped |
| Search + team page | player search hits (e.g. Lamine Yamal); team page w/ name, venue, badge |
| H2H | both last-10 lists + 1 mutual meeting found |
| Countries/leagues | 38 countries discovered from the day feed |
| Top scorers | **0 — upstream block genuinely empty in-season** (payload lenient; will fill when the upstream populates it) — superseded 2026-09-30: the block *is* populated under `players`, a DTO key mismatch hid it; fixed + pinned by `UitslagenMapperTest` |

Bugs this run caught and fixed: garbage logo URLs for blank/`_a`-suffixed team ids (now null → UI placeholder); H2H mutual meetings = 0 under name-only matching (now id-first matching); match-detail referee never surfaced (now `Referee #<id>` from `refereeId`).

League-key map: `UitslagenLeagues.BY_ID` is a **static, hand-maintained table** — every entry was verified live on 2026-09-29 (via `fixtures_v2/{key}_small.json` or the `leagueKey` embedded in `search_v3` team results): `EnglandPremierLeague`, `SpainPrimeraDivision`, `ItalySerieA`, `GermanyBundesliga`, `FranceLigue1`, `NetherlandsEredivisie`, `PortugalPrimeiraLiga`, `SaudiArabiaProLeague`, `BelgiumProLeague`, `SwitzerlandSuperLeague`. An earlier runtime-discovery idea (self-registering `leagueid`→`leagueKey` pairs from feed rows) was removed as unworkable: footapi's numeric `leagueid` is a different numbering system from the app's legacy ids (1079, 1203, 1272… vs 152, 302, 207…) and is not unique (1201 spans four leagues), so such a bridge can never fire. A legacy id missing from the table resolves to null and the adapter honestly serves an empty league block — extend `BY_ID` (after verifying the key live) rather than expecting self-healing.
