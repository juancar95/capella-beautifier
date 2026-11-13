# Capella Beautifier 1.0.4 validation

Executed on 13 November 2026. Supports Capella 7.0.0, 7.0.1 and 7.1.0 with
one plugin and a dependency package matched to each Capella version.

## Automated evidence

[The complete CI matrix passed](https://github.com/juancar95/capella-beautifier/actions/runs/43478097780)
at source commit `cf96cf075356f7e487f4adfa5e91fe96f46ab87d`. Subsequent changes
to this record and README are documentation only.

| Capella | macOS 15 | Windows |
|---------|----------|---------|
| 7.0.0 | Passed | Passed |
| 7.0.1 | Passed | Passed |
| 7.1.0 | Passed | Passed |

Every runtime job downloads the official product, verifies its SHA-512, builds
the plugin with Java 17 bytecode, resolves the full dependency graph in a fresh
Equinox framework, and checks configuration creation and handler linkage.
ELK executes inside the actual OSGi dependency wiring, including products with
multiple native Guava versions. Each job tests eight combinations: RIGHT/DOWN,
flat/nested and explicit ports/no ports. Assertions cover finite positive
geometry, flow order, node separation and orthogonal routed edges: **48 engine
cases across six product/platform combinations**.

Nine packaging, dependency adaptation and installer regression tests also pass.
They reject missing/extra/modified dependencies, check reproducible ZIP bytes,
preserve class and license bytes during the documented Xbase manifest adaptation,
and exercise the actual macOS shell or Windows PowerShell installer. Installer
fixtures cover paths with spaces, duplicate old plugins in both scanned trees,
backups, repeat installation, preservation of native bundles/configuration and
failure before mutation when a package is absent. The separate Ubuntu packaging
job passed too.

All dependencies are pinned in `scripts/release-dependencies.json`. The 7.1.0
product was checked against Eclipse's published SHA-512. Eclipse's sums service
has no entry for the older 7.0.0 macOS ARM archive: its explicit SHA-512 pin
matches independent downloads from the official server on 14 October and
13 November 2026; this exception is visible in the workflow.

## Interactive evidence

Tests used native Capella applications on macOS ARM64 and disposable copies of
each version's bundled In-Flight Entertainment example. H/V means both toolbar
actions produced different saved notation. Undo means **one** operation restored
the complete preceding GMF notation; redo restored the complete vertical result.
These are byte comparisons of the serialized notation subtree using
`scripts/compatibility/compare-diagram-state.py`, beyond visual inspection.

| Capella | Diagram | Executed and verified |
|---------|---------|-----------------------|
| 7.0.0 | LAB: [BUILD] All Components, Functions, CEs, FEs | H, V, undo, redo, save |
| 7.0.0 | PAB: [CTX] Watch Imposed Video on Cabin Screen FC | H, V, undo, save |
| 7.0.1 | LAB: [BUILD] All Components, Functions, CEs, FEs | H, V, undo, redo, save |
| 7.0.1 | PAB: [CTX] Watch Imposed Video on Cabin Screen FC | H, V, undo, save |
| 7.1.0 | LAB: IFE System - All Components, CEs | H, V, undo, redo, save, restart/reopen |
| 7.1.0 | PAB: Remote diagnostic equipment SW | H, V, undo, save; reproduced hierarchical-import fallback |
| 7.1.0 | PAB: Focus on Network Transport | H, V, undo, save; reproduced hierarchical-import fallback |
| 7.1.0 | SDFB: Top Level Functional Overview | H, V, undo, redo, save |
| 7.1.0 | SAB: High Level System Overview | H, V, undo, save |

All nine H/V results differed; all nine undo comparisons matched exactly; all
four redo comparisons matched exactly. The example's `.capella` and `.afm`
semantic files retained their original SHA-256 hashes in all three versions.
Saved `.aird` snapshots remain in the maintainer's ignored local validation
directories, separate from customer models and release packages.

Two defects were reproduced before correction: undo previously reverted only
the style post-processing, and ELK 0.9.0 failed in `ElkGraphImporter.transformEdge`
on the two PABs above. Beautify now runs as one transaction. Only that specific
hierarchical-import null-pointer failure triggers a single per-container retry;
other arrangement failures retain normal transaction failure handling.

## Package and installation evidence

The tested common plugin SHA-256 is:

```text
7151340751a5d5e2cfbc00e82b4b3b2f36a6c1d7d92c9e7f7cd6414319eb3323
```

The three full packages contain this same JAR. The release's `SHA256SUMS` file
records their individual hashes. Each contains 12 JARs in the discoverable
`dropins/capella-beautifier/eclipse/plugins` tree; native product plugins are
not overwritten.

A fresh official Capella 7.1.0 distribution plus the final full package was
installed as `/Applications/Capella-7.1.0.app`, using a separate example workspace.
All 12 installed JARs match the package byte for byte. Startup discovered the
drop-in without manual bundle registration; the installed app reopened the
saved LAB and passed both H/V actions and save. Existing 7.0.x applications and
the user's existing workspaces were preserved.

## Boundaries

Windows has automated build, real Equinox/ELK execution and native installer
coverage; **Windows interactive toolbar/undo tests were not performed**. Linux
GUI and Linux product runtime were not tested. Engine test graphs complement,
but do not replace, the interactive diagram tests.

These results do not establish correctness for every diagram. Dense diagrams
and functional-chain overlays can still need manual cleanup. The targeted
fallback does not address every possible ELK failure. No sequence-diagram-specific
layout behavior is promised.

Engineering guidance: ISO/IEC 25010 maintainability (small, explicit changes)
and NIST SSDF release evidence (traceable inputs and artifact hashes). This is
not a claim of certification or standards compliance.
