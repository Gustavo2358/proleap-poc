# W7 performance

All 12 measured shape variants PASS with the same exact dependency oracle used for their structural tests. Final replay was sequential, Java21, `-Xmx2g`, per-stage external `timeout --kill-after=2s 180s`; `/usr/bin/time` reports peak RSS. Times include process/JVM startup, are single-run observations and are not statistically calibrated thresholds. No semantic cutoff was introduced. Complete stage metrics (including frontend/CFG RSS), AIR byte/operation counts and source hashes: `performance.csv` / `performance.json`.

| Shape | SP statements | AIR blocks | CFG nodes/edges | Frontend s | Lower s | CFG s | Dependencies s | Lower/analysis RSS MiB |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| perf-if-1 | 6 | 10 | 12/11 | 1.216 | 0.816 | 0.314 | 0.415 | 160.7/84.0 |
| perf-if-5 | 10 | 22 | 24/27 | 1.216 | 0.815 | 0.314 | 0.415 | 171.2/97.8 |
| perf-if-20 | 25 | 67 | 69/87 | 1.817 | 0.915 | 0.415 | 0.515 | 192.2/97.6 |
| perf-evaluate-2 | 7 | 14 | 16/16 | 1.216 | 0.815 | 0.314 | 0.415 | 167.1/87.6 |
| perf-evaluate-10 | 15 | 46 | 48/56 | 1.316 | 0.916 | 0.415 | 0.515 | 183.7/94.7 |
| perf-evaluate-40 | 45 | 166 | 168/206 | 1.316 | 1.116 | 0.515 | 0.615 | 221.3/128.2 |
| perf-mixed-1 | 6 | 10 | 12/11 | 1.216 | 0.815 | 0.314 | 0.415 | 162.9/84.7 |
| perf-mixed-5 | 10 | 22 | 24/27 | 1.316 | 0.915 | 0.314 | 0.415 | 161.8/98.8 |
| perf-mixed-20 | 25 | 67 | 69/87 | 62.370 | 0.915 | 0.415 | 0.515 | 194.1/96.4 |
| perf-goto-fanout-2 | 8 | 8 | 10/11 | 1.216 | 0.815 | 0.314 | 0.415 | 119.8/87.3 |
| perf-goto-fanout-10 | 24 | 24 | 26/35 | 1.216 | 0.815 | 0.415 | 0.515 | 149.5/93.7 |
| perf-goto-fanout-40 | 84 | 84 | 86/125 | 1.316 | 1.016 | 0.415 | 0.615 | 184.2/109.4 |

IF depth1/5/20 produces 10/22/67 blocks; EVALUATE2/10/40 arms produces 14/46/166 blocks. Mixed depth1/5/20 produces 10/22/67 blocks; supported conditional GO TO fanout2/10/40 produces 8/24/84 blocks. The measured families do not enumerate branch-path combinations. This does not establish a universal bound on arbitrary programs or W6 activation DAGs.

## Deep mixed parsing qualification

The first mixed-depth-20 trial (`new-cases-trial-3/`) exceeded its 60s frontend budget and is preserved as PIPELINE_FAILED; downstream was NOT_REACHED. A read-only JVM stack of the approved W6-R1 baseline (`baseline-mixed20-jstack.txt`) showed ANTLR prediction, not activation expansion. Matched 180s runs: baseline frontend62.871s then lower failure0.816s; W7 frontend67.582s, lower1.016s and full PASS. The final W7 sequential run repeats PASS with frontend62.370s and lower0.915s. There is no unresolved W7 composition first loss here; the high parsing cost is preexisting. No parser change, exception swallowing or arbitrary depth restriction was made.

## Retained operational limits

Live shared activation DAGs can still expand by context. Any published FILE operation use selects eager activation scheduling, including operations without proven callbacks. W6-R1's 47-case replay covers its approved cold/live demand, width/depth and repetition controls; those remain green. W7 adds no activation strategy and no new branch cloning. General parsing performance and FILE scheduling refinement are recorded limits, not folded into W8 memory semantics or hidden by oracle changes.
