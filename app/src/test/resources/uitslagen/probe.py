"""Polite probe of uitslagen.live/footapi — capture real payloads + status-code evidence.

Step 1 of the mission. No app-logic changes. Run with:  py probe.py
"""
import json
import sys
import time
import urllib.request

BASE = "https://uitslagen.live/footapi"
UA = "FootballPulse/1.0 (mission-uitslagen step-1 probe)"
TIMEOUT = 15
GAP = 2.5  # seconds between calls — politeness contract


def get(path, params=None):
    url = BASE + path
    if params:
        url += "?" + "&".join(f"{k}={v}" for k, v in params.items())
    req = urllib.request.Request(url, headers={
        "Accept": "application/json",
        "User-Agent": UA,
    })
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:
        return -1, str(e)


def save(name, status, text):
    with open(name, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"  saved {name} (http {status}, {len(text)} bytes)")


def summarize_livenow(text):
    """Return (countries, leagues, rows, status-code evidence rows)."""
    try:
        data = json.loads(text)
    except Exception as e:
        return 0, 0, 0, [("parse-error", str(e)[:120])]
    countries = leagues = rows = 0
    evidence = []
    for c in data:
        countries += 1
        for lg in c.get("leagues", []):
            leagues += 1
            for m in lg.get("matches", []):
                rows += 1
                evidence.append((
                    m.get("id"), m.get("status"), m.get("scoretime"), m.get("time"),
                    m.get("ht"), m.get("isET"), m.get("isOT"),
                    m.get("localteam"), m.get("visitorteam"),
                    m.get("leaguename") or m.get("leagueKey"),
                ))
    return countries, leagues, rows, evidence


def main():
    day = time.strftime("%d/%m/%Y")
    print("Today:", day)

    # 1) live-now feed, capture 1
    print("\n[1] feed_livenow (capture 1)")
    st, t = get("/fixtures/feed_livenow.json", {"lang": "en", "version": "2800"})
    save("feed_livenow_1.json", st, t)
    c1, l1, r1, ev1 = summarize_livenow(t)
    print(f"  countries={c1} leagues={l1} rows={r1}")
    first_rows = ev1[:12]
    live_rows = [e for e in ev1 if str(e[1]).isdigit() and str(e[2]) not in (" - ", "-")]
    print("  sample rows (id, status, score, time, ht, isET, isOT, home, away, league):")
    for e in first_rows:
        print("   ", e)

    time.sleep(GAP)

    # 2) full-day aggregated feed for today
    print("\n[2] feed_matches_aggregated (today)")
    st, t = get("/fixtures/feed_matches_aggregated.json",
                {"lang": "en", "version": "2800", "date": day.replace("/", "%2F"), "tzoffset": "0"})
    save("feed_matches_aggregated.json", st, t)
    try:
        data = json.loads(t)
        countries = len(data)
        rows = sum(len(lg.get("matches", []))
                   for c in data for lg in c.get("leagues", []))
        print(f"  countries={countries} rows={rows}")
        ev = summarize_livenow(t)[3]
        statuses = {}
        for e in ev:
            s = str(e[1])
            statuses.setdefault(s, 0)
            statuses[s] += 1
        print("  status distribution:", dict(sorted(statuses.items())))
    except Exception as e:
        print("  parse error:", e)

    time.sleep(GAP)

    # 3) fixtures_v2 for a league key seen in today's feed
    key = None
    try:
        for c in json.loads(t if 't' in dir() else "[]"):
            for lg in c.get("leagues", []):
                if lg.get("matches"):
                    key = lg.get("key") or lg.get("leagueKey")
                    break
            if key:
                break
    except Exception:
        pass
    if not key:
        key = "IndiaIFAShield"
    print(f"\n[3] fixtures_v2/{key}.json (full + _small)")
    st, t2 = get(f"/fixtures_v2/{key}.json", {"lang": "en", "version": "2800"})
    save("fixtures_v2_full.json", st, t2)
    time.sleep(GAP)
    st, t2b = get(f"/fixtures_v2/{key}_small.json", {"lang": "en", "version": "2800"})
    save("fixtures_v2_small.json", st, t2b)
    try:
        d = json.loads(t2)
        top = d if isinstance(d, list) else [d]
        allm = [m for blk in top for m in (blk.get("matches", []) if isinstance(blk, dict) else [])]
        print(f"  blocks={len(top)} matches={len(allm)}")
        for m in allm[:5]:
            print("   ", m.get("id"), "|", m.get("date"), "|", m.get("time"), "|",
                  m.get("status"), "|", m.get("scoretime"), "|", m.get("ht"), "|",
                  m.get("localteam"), m.get("visitorteam"))
    except Exception as e:
        print("  parse error:", e)

    time.sleep(GAP)

    # 4) search
    print("\n[4] search_v3?q=ajax")
    st, t = get("/search_v3", {"lang": "en", "version": "2800", "q": "ajax"})
    save("search_v3.json", st, t)
    print("  head:", t[:200].replace("\n", " "))

    time.sleep(GAP)

    # 5) match detail — pick a live or recent match id from capture 1
    mid = None
    for e in live_rows + first_rows:
        if e[0]:
            mid = e[0]
            break
    if not mid:
        mid = "3951238"  # known-good fallback from earlier probe
    print(f"\n[5] matches/{mid}.json?h2h=1")
    st, t = get(f"/matches/{mid}.json", {"lang": "en", "version": "2800", "h2h": "1"})
    save("matches_detail.json", st, t)
    try:
        d = json.loads(t)
        if isinstance(d, dict):
            print("  keys:", sorted(d.keys()))
            print("  status:", d.get("status"), "| venue:", d.get("venue"),
                  "| events:", len(d.get("events") or []))
            lu = d.get("lineups") or {}
            print("  lineups keys:", {k: (len(v) if isinstance(v, (list, dict)) else v)
                                      for k, v in lu.items()})
    except Exception as e:
        print("  parse error:", e)

    time.sleep(GAP)

    # 6) team + player by the ids from the detail match
    lid = vid = pid = None
    try:
        d = json.loads(open("matches_detail.json", encoding="utf-8").read())
        lid, vid = d.get("gs_localteamid"), d.get("gs_visitorteamid")
        if d.get("events"):
            pid = (d["events"][0].get("player_id")
                   or d["events"][0].get("playerId")
                   or d["events"][0].get("id"))
    except Exception:
        pass
    if not lid:
        lid, vid = "34291", "24035"  # fallback: known ids from earlier probe
    print(f"\n[6] team_gs/{lid}.json")
    st, t = get(f"/team_gs/{lid}.json", {"lang": "en", "version": "2800"})
    save("team_gs.json", st, t)
    print("  head:", t[:200].replace("\n", " "))
    time.sleep(GAP)
    print(f"[7] players/{pid or 474972}.json")
    st, t = get(f"/players/{pid or 474972}.json", {"lang": "en", "version": "2800"})
    save("players.json", st, t)
    print("  head:", t[:200].replace("\n", " "))

    time.sleep(GAP + 3)

    # 8) live-now capture 2 — spaced in time for status evidence
    print("\n[8] feed_livenow (capture 2, spaced)")
    st, t = get("/fixtures/feed_livenow.json", {"lang": "en", "version": "2800"})
    save("feed_livenow_2.json", st, t)
    c2, l2, r2, ev2 = summarize_livenow(t)
    print(f"  countries={c2} leagues={l2} rows={r2}")

    # 9) status-code evidence table — which codes changed between captures
    print("\n=== status evidence: capture1 -> capture2 (same match id) ===")
    byid1 = {e[0]: e for e in ev1}
    changed = 0
    for e in ev2:
        prev = byid1.get(e[0])
        if prev and (prev[1] != e[1] or prev[2] != e[2]):
            changed += 1
            print("  ", prev[:6], "->", e[:6])
    print(f"  {changed} matches changed status/score between captures")
    allcodes = {}
    for e in ev1 + ev2:
        s = str(e[1])
        allcodes.setdefault(s, set()).add((e[2], e[3], e[8]))
    print("\n=== distinct status codes seen (code: (score,time,league) samples) ===")
    for code in sorted(allcodes, key=lambda x: (not x.isdigit(), x)):
        samples = list(allcodes[code])[:4]
        print(f"  {code!r}: {samples}")


if __name__ == "__main__":
    main()
