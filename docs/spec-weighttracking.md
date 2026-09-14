# Weight tracking — decision summary

## Purpose

- Track **body weight** with a **weekly average** and direction: **up / down / maintained**
- Optional **diet phase**: None, Maintain, Gain, Lose

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

## Diet phase

- Shown on the Weight tab only when an official average exists (≥3 logs this week or last 7 days).
  That average is the **start-weight snapshot**.
- **None** turns the feature off (no check, no chart overlays).
- The dashboard shows the **current** phase only (kind, target, rate, weeks left) and links to a
  **Diet phase** screen to create or change it. Changing a phase ends the current row and starts a
  new one.
- **Maintain** has a target weight (prefilled with the start average). On-track and chart band:
  **target ± 0.50 kg**.
- **Gain / Lose** have duration **1–30 weeks** and a target weight. Weekly rate =
  `(target − start) / weeks`.
- Apply **rejects** wrong-direction targets, duration outside 1–30, Gain faster than **0.5% of start
  weight per week**, and Lose faster than **1% of start weight per week**.
- **On-track** (green check on the average card, not a checkbox):
  - Maintain: current average within **±0.50 kg** of target
  - Gain: week-to-week Δ in `0 … max(0.50 kg, min(2×rate, 0.5% start))`
  - Lose: week-to-week Δ in `−max(0.50 kg, min(2×|rate|, 1% start)) … 0`
  - A reverse week is off track. Hide the check when Δ cannot be computed.
- After Gain/Lose **ends**, effective phase is **Maintain** at the last official average.
- Chart: while a phase is on, extend **4 weeks** ahead. Maintain draws high/low lines. Gain/Lose
  draws a projected line until the end date, then the maintain band.

## Persistence

- **Local persistence** required for v1 (survive process death; reinstall may wipe). No cloud in v1

## Weight tab (product shape)

- **Empty**: primary **Log weight** CTA only
- **Current week** as hero: provisional/official avg, comparison when baseline exists, **Mon→Sun**
  entry list
- Diet phase summary (when an official average exists) linking to the editor
- Weekly average chart; diet-phase overlays when a phase is active
- **Past list**: previous official weeks only (not duplicating current week)
- Today CTA when there’s no entry for today still fits; exact layout can refine per feature

## Explicitly deferred

- Date picker end date (duration in weeks is the v1 input)
- Date picker / backfill / full history edit / delete
- Cloud sync, lb
