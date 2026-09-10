# What the programming rules are based on

The generator's system instruction tells a model how to program a training week.
Every rule in it is answerable to something published; this file is where the
answer lives, so the prompt can stay short and a later change can be argued
against a source rather than a preference.

The onboarding questions are here too, because a question that no rule consumes
is a question worth removing, and a rule with no question behind it is a rule
the model has to guess at.

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

## Rules and the source behind each

**Weekly volume, per muscle group.** Four working sets per muscle per week is
the floor worth programming (TIME). Growth is graded in weekly sets — each
additional set is worth about 0.37% more gain, and ten or more per muscle beats
fewer (VOL). The prompt therefore carries a weekly set target keyed to
experience rather than leaving volume to the model's taste.

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
non-failure was better for strength where volume was not equated (FAIL). One to
three repetitions in reserve is the default, expressed in the RIR vocabulary
(RIR), and beginners stay at the cautious end.

**Progression.** Add 2–10% load once the client can complete one or two
repetitions beyond target (ACSM-2009). Below one loadable increment there is no
increase to add, so reps or seconds move instead.

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
13–17: technique before load, no maximal attempts.

**Flexibility.** Sixty seconds per muscle-tendon group, at least two days a week
(ACSM-2011).

**Screening.** Screening is about current activity level, symptoms or known
cardiovascular, metabolic or renal disease, and the intensity intended — not a
risk-factor tally (SCREEN). The app asks one question in that shape and softens
the plan rather than blocking anyone, which is the direction ACSM moved for
exactly this reason: over-referral keeps people from starting.

**Enjoyment.** Autonomous motivation predicts who is still training later
(ADHERE), and ACSM-2011 names enjoyable exercise as an adherence factor. That is
the argument for asking what someone wants to do, not only what they need.

## Questions the plan actually uses

Every question below reaches the model. A question that does not is either
deleted or wired up; nothing is collected to be looked at once on a review
screen.

| Question | What it decides | Source |
| --- | --- | --- |
| Age | Balance work, maximal-load ceiling | OLD, WHO-2020 |
| Height, weight | Load starting points, impact tolerance | — |
| Experience | Rep range, weekly sets, session frequency | ACSM-2009, VOL |
| Goal | Rep range, rest, cardio dose | ACSM-2009, ACSM-WL |
| Style | The week's split between lifting and conditioning | CONC |
| Location and equipment | Exercise selection, whether load exists at all | LOAD, TIME |
| Heaviest weight available | Whether a prescribed load can be made | — |
| Days per week | Split, weekly volume, per-muscle frequency | FREQ, ACSM-2009 |
| Session length | The number of sets that fit, warm-up and rest included | REST |
| Injuries | Movements to avoid and what replaces them | — |
| Health flag | Intensity ceiling | SCREEN |
