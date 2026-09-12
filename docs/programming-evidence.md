# What the programming rules are based on

The rules that program a training week live in code — `SessionBudget`,
`RepWindow`, `SeedLoad`, `ProgressionEngine`, `DeloadCheck`, `InjuryGuard` and
`LoadStep`, mirrored in Swift — and the model only chooses which movement fills
each slot. Every rule is answerable to something published, or says plainly
that it is a heuristic; this file is where the answer lives, so a later change
can be argued against a source rather than a preference.

The onboarding questions are here too, because a question that no rule consumes
is a question worth removing, and a rule with no question behind it is a rule
the app has to guess at.

## Sources

| Tag | Source |
| --- | --- |
| ACSM-2009 | ACSM Position Stand. Progression models in resistance training for healthy adults. *Med Sci Sports Exerc* 41(3):687–708, 2009. [PMID 19204579](https://pubmed.ncbi.nlm.nih.gov/19204579/) |
| ACSM-2011 | Garber et al. ACSM Position Stand. Quantity and quality of exercise. *Med Sci Sports Exerc* 43(7):1334–59, 2011. [PMID 21694556](https://pubmed.ncbi.nlm.nih.gov/21694556/) |
| ACSM-WL | Donnelly et al. ACSM Position Stand. Physical activity strategies for weight loss. *Med Sci Sports Exerc* 41(2):459–71, 2009. [PMID 19127177](https://pubmed.ncbi.nlm.nih.gov/19127177/) |
| WHO-2020 | Bull et al. WHO 2020 guidelines on physical activity and sedentary behaviour. *Br J Sports Med* 54(24):1451–62. [PMID 33239350](https://pubmed.ncbi.nlm.nih.gov/33239350/) |
| VOL | Schoenfeld, Ogborn & Krieger. Dose–response between weekly volume and muscle mass. *J Sports Sci* 35(11):1073–82, 2017. [PMID 27433992](https://pubmed.ncbi.nlm.nih.gov/27433992/) |
| FREQ | Schoenfeld, Ogborn & Krieger. Resistance training frequency and hypertrophy. *Sports Med* 46(11):1689–97, 2016. [PMID 27102172](https://pubmed.ncbi.nlm.nih.gov/27102172/) |
| LOAD | Schoenfeld et al. Low- vs high-load resistance training. *J Strength Cond Res* 31(12):3508–23, 2017. [PMID 28834797](https://pubmed.ncbi.nlm.nih.gov/28834797/) |
| REST | Schoenfeld et al. Longer interset rest periods. *J Strength Cond Res* 30(7):1805–12, 2016. [PMID 26605807](https://pubmed.ncbi.nlm.nih.gov/26605807/) |
| TIME | Iversen, Norum, Schoenfeld & Fimland. No time to lift? Time-efficient programs. *Sports Med* 51(10):2079–95, 2021. [PMID 34125411](https://pubmed.ncbi.nlm.nih.gov/34125411/) |
| FAIL | Grgic et al. Training to failure or non-failure. *J Sport Health Sci* 11(2):202–11, 2022. [PMID 33497853](https://pubmed.ncbi.nlm.nih.gov/33497853/) |
| RIR | Zourdos et al. RPE scale measuring repetitions in reserve. *J Strength Cond Res* 30(1):267–75, 2016. [PMID 26049792](https://pubmed.ncbi.nlm.nih.gov/26049792/) |
| CONC | Wilson et al. Concurrent training meta-analysis. *J Strength Cond Res* 26(8):2293–307, 2012. [PMID 22002517](https://pubmed.ncbi.nlm.nih.gov/22002517/) |
| SCREEN | Riebe et al. Updating ACSM's exercise preparticipation health screening. *Med Sci Sports Exerc* 47(11):2473–9, 2015. [PMID 26473759](https://pubmed.ncbi.nlm.nih.gov/26473759/) |
| OLD | Fragala et al. Resistance training for older adults: NSCA position statement. *J Strength Cond Res* 33(8):2019–52, 2019. [PMID 31343601](https://pubmed.ncbi.nlm.nih.gov/31343601/) |
| ADHERE | Teixeira et al. Exercise, physical activity, and self-determination theory. *Int J Behav Nutr Phys Act* 9:78, 2012. [PMID 22726453](https://pubmed.ncbi.nlm.nih.gov/22726453/) |
| NUNES | Nunes et al. 2021. Resistance exercise order and its effect on strength and hypertrophy, systematic review and meta-analysis. *Eur J Sport Sci*. |
| REYNOLDS | Reynolds, Gordon & Robergs 2006. Predicting 1RM from multiple-RM testing and anthropometry. [PMID 16937972](https://pubmed.ncbi.nlm.nih.gov/16937972/) |
| MILLER | Miller et al. 1993. Sex differences in strength and muscle fibre characteristics. [PMID 8477683](https://pubmed.ncbi.nlm.nih.gov/8477683/) |
| OGASAWARA | Ogasawara et al. 2013. Continuous versus periodic strength training and hypertrophy. [PMID 23053130](https://pubmed.ncbi.nlm.nih.gov/23053130/) |
| BICKEL | Bickel, Cross & Bamman 2011. Exercise dosing to retain resistance training adaptations in young and older adults. [PMID 21131862](https://pubmed.ncbi.nlm.nih.gov/21131862/) |
| COLEMAN | Coleman et al. 2024. A one-week deload during supervised resistance training. *PeerJ* 12:e16777. [PMID 38274324](https://pubmed.ncbi.nlm.nih.gov/38274324/) |
| ROGERSON | Rogerson et al. 2024. Deloading practices in strength and physique sports. [PMID 38499934](https://pubmed.ncbi.nlm.nih.gov/38499934/) |
| STEELE | Steele et al. 2017. Predicting repetitions to momentary failure. [PMID 29204323](https://pubmed.ncbi.nlm.nih.gov/29204323/) |
| HALPERIN | Halperin et al. 2022. Accuracy of intra-set repetitions-in-reserve predictions. [PMID 34542869](https://pubmed.ncbi.nlm.nih.gov/34542869/) |
| PELLAND | Pelland et al. 2026. Meta-regression of weekly volume against strength and hypertrophy. [PMID 41343037](https://pubmed.ncbi.nlm.nih.gov/41343037/) |
| ENES | Enes et al. 2024. Progressive volume increases in trained lifters. [PMID 37796222](https://pubmed.ncbi.nlm.nih.gov/37796222/) |
| MORENO | Moreno et al. 2025. Volume progression versus maintenance. [PMID 39557664](https://pubmed.ncbi.nlm.nih.gov/39557664/) |
| BARSUHN | Barsuhn et al. 2025. Volume progression versus maintenance. [PMID 39665246](https://pubmed.ncbi.nlm.nih.gov/39665246/) |
| ROBINSON | Robinson et al. 2024. Proximity to failure and muscle hypertrophy, dose–response. [PMID 38970765](https://pubmed.ncbi.nlm.nih.gov/38970765/) |
| REFALO | Refalo et al. 2023. Proximity to failure, strength and hypertrophy, meta-analysis. [PMID 36334240](https://pubmed.ncbi.nlm.nih.gov/36334240/) |

## Rules and the source behind each

**Weekly volume, per muscle group.** Four working sets per muscle per week is
the floor worth programming (TIME). Growth is graded in weekly sets — each
additional set is worth about 0.37% more gain, and ten or more per muscle beats
fewer (VOL). The skeleton therefore deals a weekly set target keyed to
experience rather than leaving volume to taste.

The target is capped by what the client's sessions can hold. Ten sets across
nine regions is ninety sets a week, and three 45-minute hypertrophy sessions pay
for forty-two — so where the target and the session set cap cannot both be met,
the cap wins. A set counts once for the muscle its movement trains and half for
each muscle it assists, the direct/indirect convention the volume literature
uses (VOL, PELLAND). Where even the four-set floor does not fit — one or two
short sessions a week — the skeleton leans on compound slots rather than chase a
target the week cannot buy. Weekly volume does not ramp: the trials that found
a benefit are small and the ones that looked again found none (ENES, MORENO,
BARSUHN).

**Session length.** The session set cap is a proxy for time and it misprices
timed work: three thirty-minute walks are three sets and a ninety-minute day. The
skeleton is fitted to the answered length at the reps a set is typically done
at, and never past half again that length even at the top of every rep window,
so no week of the ladder can break it. The parser holds every week to the same
ceiling.

**Conditioning and mobility slots.** How many of each the skeleton lays out is
keyed to the goal: a weight-loss week that must reach 250 minutes of moderate
work (WHO-2020, ACSM-WL) needs walking and cycling on offer, and a flexibility
week needs stretching and mobility work. A weight-loss or endurance session
takes whatever its length has left as conditioning. The squat/press/pull
requirement (TIME) is not imposed on a flexibility goal, since rejecting the
week for lacking a squat overrules the answer the client gave.

**Frequency, per muscle group.** Twice a week beats once at matched volume
(FREQ). Session frequency by training age: 2–3 days novice, 3–4 intermediate,
4–5 advanced (ACSM-2009). The split rules (full body at 2–3 days, upper/lower at
4, push/pull/legs at 5–6) exist to reach twice-weekly coverage at every day
count, not for their own sake.

**Load and repetitions.** Hypertrophy is similar across a wide loading range;
maximal strength is not, and needs heavy loads (LOAD). Novices belong at 8–12RM;
1–6RM with 3–5 minutes of rest is for intermediate and advanced lifters
(ACSM-2009). This is why a strength goal with no external load cannot be served
by pretending: bodyweight work gets harder leverage, not a fictional 3-rep max.

**Rest.** Three minutes beat one minute for both strength and size in trained
lifters (REST); ACSM-2009 asks 3–5 minutes for heavy work, 1–2 for hypertrophy,
under 90 seconds for muscular endurance. Multi-joint work gets the longer end.

**Effort.** Training to failure is not required for either strength or size, and
non-failure was better for strength where volume was not equated (FAIL). The rep
windows sit below failure by construction, and progression is driven by the reps
the client actually completed rather than a reported reps in reserve: nothing in
the app logs effort, and self-reported RIR is least accurate furthest from
failure and in beginners (STEELE, HALPERIN).

**Progression.** Add load once the client completes the top of the rep window
(ACSM-2009), as a ladder: a met week adds a rep, and a second met week at the
top adds load — 5% on a lower-body compound, 3.5% on an upper-body one, 2.5% on
isolation work and on anything for a client of 65 or over, and never less than
one loadable increment. The increment is the gym's, counted in the unit its kit
is marked in: a 20 kg / 45 lb bar in 2.5 kg / 5 lb steps, dumbbells in 2.5 kg /
5 lb per bell, machines in 5 kg / 10 lb, kettlebells only in the sizes they are
cast in (`LoadStep`). A stall takes about a tenth off and is counted; no week
moves a load by more than a fifth, except that a single increment is never
blocked.

**Warm-up.** Exercise-specific ramp-up sets, not a general routine, and no
static stretching unless flexibility is the goal (TIME).

**Cardio dose.** 150–300 minutes of moderate or 75–150 of vigorous activity a
week for health (WHO-2020, ACSM-2011). For weight loss, 150–250 minutes prevents
gain and produces modest loss; clinically meaningful loss is associated with more
than 250 minutes a week (ACSM-WL). Resistance training does not itself drive
weight loss, but it preserves fat-free mass (ACSM-WL) — which is why a weight
loss goal still gets lifted.

**Mixing cardio with lifting.** Interference is real and scales with the
frequency and duration of the endurance work; running interferes where cycling
does not (CONC). A muscle or strength goal keeps conditioning short, low-impact
and away from the legs' hard sessions.

**Age.** Over 65: balance and neuromotor work alongside strength, supported
variations first (OLD, WHO-2020, ACSM-2011 asks 2–3 days of neuromotor work).
13–17: technique before load, no maximal attempts. In the rep windows that is two
more reps on both bounds and a load step no larger than 2.5% from 65, and never
fewer than 8–12 reps under 18.

**Flexibility.** Sixty seconds per muscle-tendon group, at least two days a week
(ACSM-2011).

**Screening.** Screening is about current activity level, symptoms or known
cardiovascular, metabolic or renal disease, and the intensity intended — not a
risk-factor tally (SCREEN). The app asks one question in that shape and softens
the plan rather than blocking anyone, which is the direction ACSM moved for
exactly this reason: over-referral keeps people from starting.

**Enjoyment.** Autonomous motivation predicts who is still training later
(ADHERE), and ACSM-2011 names enjoyable exercise as an adherence factor. The app
used to ask for a training style as well; it went, because the goal already
decides the balance of lifting and conditioning and two answers could
contradict each other. Which movement fills each slot is still the part a coach
chooses for this client.

**Exercise order.** Whatever is trained first in a session gains most
(NUNES), so the skeleton's slot tiers are a fatigue rule: the warm-up, then
the main lift, the second lift, accessory work, isolation, core, conditioning
and the cool-down.

**Injuries.** A declared injury rules out a few movements outright and asks
care with most (`InjuryGuard`). The exclusions are the clauses the coaching
brief used to state in prose, made enforceable; the cautions are one line on
the card about what to watch. This is general fitness information, not
medical advice, and the list is the app's own, to be reviewed rather than
treated as settled.

## Heuristics, stated as such

These are starting points, not findings. Each lives as a named constant so it
can be tuned against what logged weeks show.

- **The starting weight.** No validated equation predicts one from bodyweight,
  age and sex; the validated route is a performed rep maximum (REYNOLDS). The
  seed is bodyweight × a coefficient by equipment and movement pattern × a
  muscle factor × sex (1.00 for a client who said male; 0.55 upper and 0.70 lower body for
  everyone else, after MILLER) × age (1% a year off past 40, never below 0.60, and 0.80 under 18) ×
  experience (0.65, 1.0, 1.3), taken as a ten-rep maximum and moved to the rep
  target by Epley's formula, capped at twelve reps. A movement done with two
  dumbbells is halved per bell. The card calls such a weight an estimate until
  the movement has been logged, and a bad guess is reseeded from what was lifted.
- **Time away**, counted from the last session of that movement (OGASAWARA,
  BICKEL, whose limits are real: small samples, and strength rather than
  every adaptation): up to 10 days carries on; 11–21 repeats the last load;
  22–42 takes a tenth off and ramps back at 5% a session; more than 42 starts
  again from 70%.
- **A lighter week** fires on any two of three triggers: two or more movements
  stalling; fewer than 60% of sets completed over two weeks; six weeks (four
  from age 50) since any load last came down. It keeps the movements and the
  load and halves the sets, for one week — a volume cut rather than a week off,
  which is what practitioners do (ROGERSON) and what the one controlled test of
  a planned week off argues for (COLEMAN). Never in a beginner's first eight
  weeks.
- **Timed work** grows 5 s a week to 90 s, drops 10 s after misses, and never
  goes under 20 s. Conditioning grows 10% in 30-second grains, at least a minute
  at a time, from a floor of five minutes.

## Questions the plan actually uses

Every question below reaches the generator. A question that does not is either
deleted or wired up; nothing is collected to be looked at once on a review
screen.

| Question | What it decides | Source |
| --- | --- | --- |
| Age | Balance work, maximal-load ceiling | OLD, WHO-2020 |
| Weight | The starting-weight estimate | REYNOLDS |
| Height | Nothing since the model stopped reading the profile: a candidate for removal | — |
| Experience | Rep range, weekly sets, session frequency | ACSM-2009, VOL |
| Goal | Rep range, rest, cardio dose | ACSM-2009, ACSM-WL |
| Equipment | Which movements are offered, whether load exists at all, the load step | LOAD, TIME |
| Heaviest weight available | Not asked yet; per-equipment ceilings (250, 50, 48, 200, 25 kg) stand in | — |
| Days per week | Split, weekly volume, per-muscle frequency | FREQ, ACSM-2009 |
| Session length | The number of sets that fit, warm-up and rest included | REST |
| Injuries | Movements ruled out, and a caution line on the ones that need care | — |
| Health flag | Intensity ceiling | SCREEN |
