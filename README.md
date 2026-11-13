# Capella Beautifier

One-click diagram beautifier for [Capella MBSE](https://mbse-capella.org/). Automatically arranges SAB, LAB, PAB, SDFB and other architecture/functional diagrams using the ELK Layered algorithm. Just install and click.

## Features

- **Beautify H** — arranges diagrams left-to-right (horizontal flow)
- **Beautify V** — arranges diagrams top-to-bottom (vertical flow)
- **Plug-and-play** — install the JAR, restart Capella, done
- **Architecture and functional diagrams** — SAB, LAB, PAB, SDFB, OAB, and more; complex diagrams may have layout limitations
- **ELK auto-configuration** — no manual setup required; injects optimal ELK Layered settings automatically
- **Hierarchical layout** — arranges elements inside containers; retries per container for the known ELK hierarchical-edge import failure
- **Single-step undo/redo** — one Beautify operation includes layout and style updates

## Requirements

- **Capella 7.0.0, 7.0.1 or 7.1.0**
- **ELK bundles** installed in Capella (included in the full installation package)

> **Important:** The plugin requires ELK (Eclipse Layout Kernel) to work. Without ELK, the buttons will appear but the layout will not work correctly. Use the **Full Installation** package below.

## Installation

### Recommended: Full Package (plugin + ELK)

1. Download the package matching your Capella version from [Releases](https://github.com/juancar95/capella-beautifier/releases/tag/v1.0.4):
   - **Capella 7.0.0:** `capella-beautifier-v1.0.4-capella-7.0.0-full.zip`
   - **Capella 7.0.1:** `capella-beautifier-v1.0.4-capella-7.0.1-full.zip`
   - **Capella 7.1.0:** `capella-beautifier-v1.0.4-capella-7.1.0-full.zip`
2. Close Capella. Remove older `capella-beautify-*.jar` files from **both `dropins` and `plugins`**, including nested folders. Remove any older `dropins/capella-beautifier` folder. Keep backups outside the scanned folders.
3. Unzip the package and copy its `dropins/capella-beautifier` folder into your Capella `dropins` folder, preserving `eclipse/plugins` inside it:
   - macOS: `<Capella.app>/Contents/Eclipse/dropins/capella-beautifier/eclipse/plugins/`
   - Windows/Linux: `<Capella Eclipse directory>/dropins/capella-beautifier/eclipse/plugins/`
4. Restart Capella. Use `-clean` on the first launch if necessary; it must appear **before `-vmargs`** in `capella.ini`.

The whole dependency set stays in the drop-in. **Do not copy it over Capella's native `plugins` folder.** Eclipse discovers and registers this layout on startup. Each package contains the same Beautifier JAR and ELK 0.9.0, with version-matched dependencies:

| Capella | Sirius/ELK adapter | Guice | Xtext Xbase |
|---------|--------------------|-------|-------------|
| 7.0.0 | 7.4.1 | 6.0.0 | 2.30.0 |
| 7.0.1 | 7.4.5 | 6.0.0 | 2.30.0 |
| 7.1.0 | 7.4.14 | 7.0.0 | 2.34.0 |

The optional Capella ELK feature is not required in addition to this full package. If you already installed another ELK configuration, use a separate stock Capella installation for the full package, or verify the dependencies before using the plugin-only download.

### Plugin only (for an already compatible ELK installation)

Download `capella-beautify-1.0.4.jar` from the release, remove older Beautifier copies from both `dropins` and `plugins`, and copy the new JAR into `dropins`. Restart Capella. This download does not install the layout engine.

### Verify ELK is installed

Check **Help > About Capella > Installation Details > Plug-ins** for these resolved bundles (the files may be in `dropins`):
- `org.eclipse.elk.core_*.jar`
- `org.eclipse.elk.alg.layered_*.jar`
- `org.eclipse.sirius.diagram.elk_*.jar`

If they are missing, use the **Full Package** above.

> **Note:** The compiled JAR is cross-platform — the same `.jar` works on macOS, Windows, and Linux.

## Build from Source

### macOS

```bash
./scripts/build-macos.sh
# Output: dist/capella-beautify-1.0.4.jar
```

### Windows

```batch
scripts\build-windows.bat
REM Output: dist\capella-beautify-1.0.4.jar
```

Requires Capella 7.0.0, 7.0.1 or 7.1.0 installed (the build uses its plugin JARs as compile dependencies). Set `CAPELLA_HOME` to the installation's Eclipse directory when using a non-default path.

## ELK Configuration

The plugin automatically injects the following ELK Layered options into all diagram descriptions:

| Option | Value | Purpose |
|--------|-------|---------|
| `elk.algorithm` | `layered` | Layered (Sugiyama) algorithm |
| `elk.direction` | `RIGHT` / `DOWN` | Flow direction (per button) |
| `elk.edgeRouting` | `ORTHOGONAL` | Right-angle edge bends |
| `elk.layered.layering.strategy` | `NETWORK_SIMPLEX` | Minimizes total edge length |
| `elk.layered.nodePlacement.strategy` | `BRANDES_KOEPF` | Compact node placement |
| `elk.hierarchyHandling` | `INCLUDE_CHILDREN` | Layout inside containers |
| `elk.layered.crossingMinimization` | `LAYER_SWEEP` | Reduces edge crossings |
| `elk.spacing.nodeNode` | `8` | Tight node spacing |
| `elk.spacing.nodeNodeBetweenLayers` | `12` | Spacing between layers |
| `elk.padding` | `3` | Container padding |

## How It Works

1. **On session open**: A `SessionManagerListener` injects ELK configuration into diagram descriptions.
2. **On button click**: One transactional diagram operation sets the direction, runs the Sirius arrange command, refreshes functional-chain/physical-path styles, and restores the original layout configuration.
3. **Known hierarchical-edge failure**: If ELK's `ElkGraphImporter.transformEdge` throws its known null-pointer failure, the handler retries once with each container laid out separately. Other errors are not silently treated as success. Undo/redo includes the complete operation.

## Validation and limitations

See [the release validation record](docs/capella-7.1-validation.md) for exact checks and platform coverage. CI compiles and checks bundle resolution and actual ELK graph layouts against all three Capella versions. Interactive testing uses disposable copies of the bundled IFE example; it cannot establish correctness for every customer diagram.

The fallback addresses the reproduced ELK 0.9.0 hierarchical-import crash, not every possible ELK failure. Dense diagrams and functional-chain overlays can still need manual cleanup. No sequence-diagram-specific layout behavior is promised.

To repeat the automated runtime checks:

```bash
python3 scripts/fetch-test-dependencies.py 7.1.0 /tmp/beautifier-deps-710
./scripts/check-bundle-compatibility.sh "$CAPELLA_HOME" /tmp/beautifier-deps-710 dist/capella-beautify-1.0.4.jar resolved
./scripts/check-elk-layout.sh "$CAPELLA_HOME" /tmp/beautifier-deps-710
python3 -m unittest discover -s tests -v
```

## Acknowledgments

This plugin was inspired by and builds upon ideas from:

- [CapellaLayoutPatch](https://github.com/open-modeling/CapellaLayoutPatch) by Open Modeling — demonstrated that ELK Layered produces excellent results on Capella diagrams via `.odesign` configuration
- [MBSE-Capella Forum: Effectively beautifying diagrams](https://forum.mbse-capella.org/t/effectively-beatifying-diagrams-with-automatic-functions-how/6516) — community discussion on diagram layout approaches
- [Obeo Blog: A picture is worth a thousand words](https://blog.obeosoft.com/a-picture-is-worth-a-thousand-words) — Sirius/ELK integration concepts

This plugin takes a different architectural approach (programmatic injection via `SessionManagerListener` + `RecordingCommand` instead of permanent `.odesign` modification) and does not share code with any of the above projects.

## Author

**Juan Carlos López Calvo**
[LinkedIn](https://www.linkedin.com/in/juancarloslopezcalvo/)

## License

[MIT](LICENSE)
