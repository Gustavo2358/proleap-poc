# W6-R1 scale qualification

External per-stage budget: timeout40s (+2s kill grace), JVM heap768MiB. Peak RSS is measured per process with /usr/bin/time; it includes non-heap memory and is not limited to768MiB. One CLI trial per row, including startup/validation/serialization. Do not interpret small timing differences as statistically significant. Final R1 trials were sequential after local FAST and qualified corpus finished.

Staged escalation: DAG2/4, then6/8, then10, then12. Baseline cold12 exhausted Java heap in AirJson.encode; larger baseline DAGs were not launched. Chains16/64/128 were measured on W6; R1 additionally qualified cold/live256. Operational failure is retained, never converted into semantic truncation or an oracle PASS.

| Version/case | Source statements / activations | AIR blocks / operations | Contexts / max depth / descriptor chars | CFG nodes / edges | Lower s / peak KiB | Analysis s / peak KiB | Result |
|---|---|---|---|---|---|---|---|
| W6 F03-dag-cold-002 | 9 / 5 | 44 / 56 | 15 / 3 / 44 | 46 / 41 | 0.815 / 135112 | 0.415 / 94476 | PASS |
| R1 F03-dag-cold-002 | 9 / 5 | 9 / 11 | 0 / 0 / 0 | 11 / 4 | 0.815 / 128324 | 0.415 / 89476 | PASS |
| W6 F03-dag-cold-004 | 13 / 9 | 216 / 264 | 83 / 5 / 76 | 218 / 211 | 0.915 / 171268 | 0.515 / 121204 | PASS |
| R1 F03-dag-cold-004 | 13 / 9 | 13 / 15 | 0 / 0 / 0 | 15 / 4 | 0.815 / 147500 | 0.415 / 101644 | PASS |
| W6 F03-dag-cold-006 | 17 / 13 | 928 / 1120 | 367 / 7 / 108 | 930 / 921 | 1.116 / 221632 | 0.715 / 180652 | PASS |
| R1 F03-dag-cold-006 | 17 / 13 | 17 / 19 | 0 / 0 / 0 | 19 / 4 | 0.815 / 138248 | 0.415 / 94324 | PASS |
| W6 F03-dag-cold-008 | 21 / 17 | 3800 / 4568 | 1515 / 9 / 140 | 3802 / 3791 | 1.516 / 337948 | 1.316 / 464356 | PASS |
| R1 F03-dag-cold-008 | 21 / 17 | 21 / 23 | 0 / 0 / 0 | 23 / 4 | 0.815 / 143840 | 0.415 / 96020 | PASS |
| W6 F03-dag-cold-010 | 25 / 21 | 15312 / 18384 | 6119 / 11 / 172 | 15314 / 15301 | 2.819 / 580448 | 3.219 / 972708 | PASS |
| R1 F03-dag-cold-010 | 25 / 21 | 25 / 27 | 0 / 0 / 0 | 27 / 4 | 0.915 / 148820 | 0.415 / 98220 | PASS |
| W6 F03-dag-cold-012 | 29 / 25 | — / — | — / — / — | — / — | 6.027 / 950512 | — / — | PIPELINE_FAILED |
| R1 F03-dag-cold-012 | 29 / 25 | 29 / 31 | 0 / 0 / 0 | 31 / 4 | 0.915 / 174144 | 0.515 / 99104 | PASS |
| W6 F03-dag-live-002 | 10 / 6 | 62 / 78 | 22 / 3 / 44 | 64 / 59 | 0.815 / 150924 | 0.415 / 95588 | PASS |
| R1 F03-dag-live-002 | 10 / 6 | 27 / 33 | 7 / 3 / 44 | 29 / 22 | 0.815 / 146680 | 0.414 / 101476 | PASS |
| W6 F03-dag-live-004 | 14 / 10 | 294 / 358 | 114 / 5 / 76 | 296 / 289 | 0.915 / 184608 | 0.615 / 133084 | PASS |
| R1 F03-dag-live-004 | 14 / 10 | 91 / 109 | 31 / 5 / 76 | 93 / 82 | 0.915 / 159044 | 0.515 / 98320 | PASS |
| W6 F03-dag-live-006 | 18 / 14 | 1246 / 1502 | 494 / 7 / 108 | 1248 / 1239 | 1.216 / 214008 | 0.815 / 206816 | PASS |
| R1 F03-dag-live-006 | 18 / 14 | 335 / 401 | 127 / 7 / 108 | 337 / 322 | 1.015 / 196096 | 0.615 / 139892 | PASS |
| W6 F03-dag-live-008 | 22 / 18 | 5078 / 6102 | 2026 / 9 / 140 | 5080 / 5069 | 1.617 / 356704 | 1.617 / 568124 | PASS |
| R1 F03-dag-live-008 | 22 / 18 | 1299 / 1557 | 511 / 9 / 140 | 1301 / 1282 | 1.216 / 222848 | 0.815 / 200908 | PASS |
| W6 F03-dag-live-010 | 26 / 22 | 20430 / 24526 | 8166 / 11 / 172 | 20432 / 20419 | 3.520 / 841512 | 4.022 / 1000856 | PASS |
| R1 F03-dag-live-010 | 26 / 22 | 5143 / 6169 | 2047 / 11 / 172 | 5145 / 5122 | 1.717 / 347028 | 1.617 / 500432 | PASS |
| R1 F03-dag-live-012 | 30 / 26 | 20507 / 24605 | 8191 / 13 / 204 | 20509 / 20482 | 3.620 / 748808 | 4.121 / 999592 | PASS |
| R1 F03-chain-cold-004 | 9 / 5 | 9 / 11 | 0 / 0 / 0 | 11 / 4 | 0.815 / 144408 | 0.415 / 100308 | PASS |
| R1 F03-chain-cold-016 | 21 / 17 | 21 / 23 | 0 / 0 / 0 | 23 / 4 | 0.815 / 150060 | 0.515 / 96476 | PASS |
| R1 F03-chain-cold-064 | 69 / 65 | 69 / 71 | 0 / 0 / 0 | 71 / 4 | 1.016 / 207944 | 0.515 / 109260 | PASS |
| R1 F03-chain-cold-128 | 133 / 129 | 133 / 135 | 0 / 0 / 0 | 135 / 4 | 1.216 / 211300 | 0.615 / 137024 | PASS |
| R1 F03-chain-cold-256 | 261 / 257 | 261 / 263 | 0 / 0 / 0 | 263 / 4 | 1.416 / 238928 | 0.815 / 189336 | PASS |
| R1 F03-chain-live-004 | 10 / 6 | 20 / 23 | 5 / 5 / 74 | 22 / 15 | 0.815 / 145412 | 0.415 / 100560 | PASS |
| W6 F03-chain-live-016 | 22 / 18 | 362 / 382 | 170 / 17 / 266 | 364 / 345 | 1.016 / 193928 | 0.615 / 139636 | PASS |
| R1 F03-chain-live-016 | 22 / 18 | 56 / 59 | 17 / 17 / 266 | 58 / 39 | 0.915 / 161904 | 0.515 / 101568 | PASS |
| W6 F03-chain-live-064 | 70 / 66 | 4490 / 4558 | 2210 / 65 / 1034 | 4492 / 4425 | 1.717 / 378960 | 1.416 / 491320 | PASS |
| R1 F03-chain-live-064 | 70 / 66 | 200 / 203 | 65 / 65 / 1034 | 202 / 135 | 1.116 / 212272 | 0.615 / 141544 | PASS |
| W6 F03-chain-live-128 | 134 / 130 | 17162 / 17294 | 8514 / 129 / 2092 | 17164 / 17033 | 3.320 / 646128 | 3.219 / 965200 | PASS |
| R1 F03-chain-live-128 | 134 / 130 | 392 / 395 | 129 / 129 / 2092 | 394 / 263 | 1.316 / 219000 | 0.715 / 182756 | PASS |
| R1 F03-chain-live-256 | 262 / 258 | 776 / 779 | 257 / 257 / 4268 | 778 / 519 | 1.717 / 329568 | 1.016 / 349148 | PASS |

Context counts and descriptor lengths are reconstructed from emitted AIR activation-entry/resume rules, source-correlated operation IDs and explicit edges, including unreachable emitted components. The reconstruction checks paired pops. It does not use COBOL text or synthesize executable edges. `maxCoverageKeyLength` alone is not a context-length measure for logical MOVE bodies; those keys can contain only hashed IDs. See context_metrics.py and performance.json for both activation and synthetic-resume operation descriptor lengths.

R1 cold DAG has no materialized activation contexts; all ordinary source statements remain inventoried. In particular cold12 now produces29 AIR blocks instead of failing during serialization. Live DAG depth12 emits8191 contexts: the real cost of this finite static representation remains exponential with shared nesting. Cold scheduling fixes wasted expansion, not universal live graph compactness. The explicit task deque removes recursive Java append calls; chain256 is measured, and the permanent chain64 suite also ran with -Xss256k.

FILE declaratives/SORT may add auxiliary callback edges outside the SP statement scheduling graph. Such units deliberately keep eager scheduling; this optimization limit is separate from executable semantics. May-return scheduling can also retain some contexts whose actual body will never return. No all-label fallback, arbitrary semantic cap or hidden truncation is used.

Decision: retain activation specialization for this revision. Required cold cases now scale with source inventory, live cases through the measured budget pass, and exact return pairing remains covered. Compact arbitrary live DAGs would require summaries/sharing or a distinct control.local integration design; these measurements do not certify unlimited graph sizes or require a new IR in W6-R1.
