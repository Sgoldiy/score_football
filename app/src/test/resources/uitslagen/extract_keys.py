"""Extract unique (country, league, key) tuples from captured day feed into results.txt."""
import json

seen = {}
with open("feed_matches_aggregated.json", encoding="utf-8") as f:
    data = json.load(f)
for c in data:
    for lg in c.get("leagues", []):
        k = lg.get("key")
        if k:
            seen[k] = (c.get("country"), lg.get("league"), len(lg.get("matches", [])))

with open("results.txt", "w", encoding="utf-8") as f:
    for k, (country, league, n) in sorted(seen.items()):
        f.write(f"{k} | {country} | {league} | {n} matches\n")
print(f"wrote {len(seen)} keys")
