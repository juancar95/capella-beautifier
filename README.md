# Capella Beautifier

One-click diagram beautifier for [Capella MBSE](https://mbse-capella.org/). Automatically arranges SAB, LAB, PAB, SDFB and all diagram types using the ELK Layered algorithm. Just install and click.

## Features

- **Beautify H** — arranges diagrams left-to-right (horizontal flow)
- **Beautify V** — arranges diagrams top-to-bottom (vertical flow)
- **Plug-and-play** — install the JAR, restart Capella, done
- **Works on all diagram types** — SAB, LAB, PAB, SDFB, OAB, and more
- **ELK auto-configuration** — no manual setup required; injects optimal ELK Layered settings automatically
- **Hierarchical layout** — arranges elements inside containers too (INCLUDE_CHILDREN)

## Requirements

- **Capella 7.0.1**
- **ELK bundles** installed in Capella (included in the full installation package)

> **Important:** The plugin requires ELK (Eclipse Layout Kernel) to work. Without ELK, the buttons will appear but the layout will not work correctly. Use the **Full Installation** package below.

## Installation

### Recommended: Full Package (plugin + ELK)

1. Download **`capella-beautifier-v1.0.2-full.zip`** from [Releases](https://github.com/juancar95/capella-beautifier/releases)
2. Unzip the package
3. Copy `capella-beautify-1.0.2.jar` to your Capella **dropins** folder:
   - macOS: `/Applications/Capella.app/Contents/Eclipse/dropins/`
   - Windows: `C:\Program Files\Capella\eclipse\dropins\`
4. Copy **all JARs** from the `plugins/` folder to your Capella **plugins** folder:
   - macOS: `/Applications/Capella.app/Contents/Eclipse/plugins/`
   - Windows: `C:\Program Files\Capella\eclipse\plugins\`
5. Restart Capella (first launch may need `-clean` flag)
6. macOS only: `sudo xattr -cr /Applications/Capella.app` (if blocked by Gatekeeper)

### Quick Install (if you already have ELK)

If ELK is already installed in your Capella:

1. Download `capella-beautify-1.0.2.jar` from [Releases](https://github.com/juancar95/capella-beautifier/releases)
2. Copy it to `<Capella>/dropins/`
3. Restart Capella

### Verify ELK is installed

Check that your Capella `plugins/` folder contains these files:
- `org.eclipse.elk.core_*.jar`
- `org.eclipse.elk.alg.layered_*.jar`
- `org.eclipse.sirius.diagram.elk_*.jar`

If they're missing, use the **Full Package** above.

> **Note:** The compiled JAR is cross-platform — the same `.jar` works on macOS, Windows, and Linux.

## Build from Source

### macOS

```bash
./scripts/build-macos.sh
# Output: dist/capella-beautify-1.0.2.jar
```

### Windows

```batch
scripts\build-windows.bat
REM Output: dist\capella-beautify-1.0.2.jar
```

Requires Capella 7.0.1 installed (the build uses its plugin JARs as compile dependencies).

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

1. **On session open**: A `SessionManagerListener` injects ELK configuration into all `DiagramDescription` objects
2. **On button click**: The handler creates a fresh ELK config with the selected direction, applies it to the diagram description, and triggers a standard Sirius `ArrangeRequest`
3. **Post-processing**: Refreshes functional chain styles, physical path styles, and normalizes zero-sized nodes

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
