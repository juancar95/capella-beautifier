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
- **ELK bundles** installed in Capella (see [Full Installation](#full-installation) below)

## Quick Installation

If you already have ELK installed in your Capella:

1. Download `capella-beautify-1.0.0.jar` from [Releases](https://github.com/juancar95/capella-beautifier/releases)
2. Copy it to `<Capella>/dropins/`
3. Restart Capella
4. Open any diagram — you'll see **Beautify H** and **Beautify V** buttons in the toolbar

## Full Installation

If you don't have ELK installed, use the provided installer script:

### macOS

```bash
# 1. Build the plugin (or download the JAR from Releases)
./scripts/build-macos.sh

# 2. Run the installer
./scripts/install-macos.sh

# 3. Remove quarantine (if needed)
sudo xattr -cr /Applications/Capella.app

# 4. Open Capella
open /Applications/Capella.app
```

### Windows

```batch
REM 1. Build the plugin (or download the JAR from Releases)
scripts\build-windows.bat

REM 2. Copy the JAR to Capella dropins
copy dist\capella-beautify-1.0.0.jar "C:\Program Files\Capella\eclipse\dropins\"

REM 3. Restart Capella
```

> **Note:** The compiled JAR is cross-platform — the same `.jar` works on macOS, Windows, and Linux. Only the build/install scripts differ per platform.

## Build from Source

### macOS

```bash
./scripts/build-macos.sh
# Output: dist/capella-beautify-1.0.0.jar
```

### Windows

```batch
scripts\build-windows.bat
REM Output: dist\capella-beautify-1.0.0.jar
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
| `elk.layered.crossingMinimization` | `LAYER_SWEEP` + semi-interactive | Reduces edge crossings |
| `elk.spacing.nodeNode` | `10` | Tight node spacing |
| `elk.spacing.nodeNodeBetweenLayers` | `15` | Spacing between layers |
| `elk.padding` | `5` | Container padding |

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
