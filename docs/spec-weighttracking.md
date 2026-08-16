# Weight tracking — decision summary

## Purpose

- Track **body weight** with a **weekly average** and direction: **up / down / maintained**
- **Observe only** for now; goal mode (cut/bulk/maintain) later

## Logging

- **≤1 entry per calendar day** (`LocalDate`, no timezone on the record)
- **kg only**, precision **0.05 kg**
- Input: text field + **±0.05 / ±0.5** steppers; default = **last logged weight**; if none → **empty
  field** (must enter a value)
- **Overwrite** same day allowed
- **Edit/delete history** and **any-date logging**: later; first build can assume **today only**
  until revisited
- Until delete exists: **overwrite-only** corrections

## Week & average

- Week = **Monday–Sunday** (ISO)
- Average = **mean of logged days only** (no filling missing days)
- **≥3 logs** → official weekly average
- **1–2 logs** → still show average, labeled **provisional** (soft label, no “need X more” count)

## Comparison (up / down / maintained)

- Compare week avg vs **baseline**: prefer previous ISO week; if ineligible, walk back to latest
  earlier week with an **official** avg (**≥3 logs**), **max 8 weeks** back
- **±0.2 kg** → maintained; outside that → up/down
- **Current week**: comparison allowed even with **&lt;3** logs, but marked **provisional**; *
  *official** when current week has **≥3** + eligible baseline
- **No eligible baseline** → **hide** comparison (still show avg)
- **Past weeks** (list): only weeks with **≥3**; each can show direction vs **its** baseline (same
  rules). Thin past weeks **omitted**

## Persistence

- **Local persistence** required for v1 (survive process death; reinstall may wipe). No cloud in v1

## Weight tab (product shape)

- **Empty**: primary **Log weight** CTA only
- **Current week** as hero: provisional/official avg, comparison when baseline exists, **Mon→Sun**
  entry list
- **Past list**: previous official weeks only (not duplicating current week)
- Today CTA when there’s no entry for today still fits; exact layout can refine per feature

## Explicitly deferred

- Goal-oriented good/bad framing
- Date picker / backfill / full history edit / delete
- Cloud sync, lb, charts beyond the rules above
