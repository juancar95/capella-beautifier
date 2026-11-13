# Changelog

## [1.0.4] - 2026-11-13

### Added
- Capella 7.1.0 support, with a version-matched Sirius/ELK 7.4.14, Guice 7.0.0 and Xbase 2.34.0 package.
- Reproducible full drop-in packages, pinned dependency downloads, checksums and CI across Capella 7.0.0, 7.0.1 and 7.1.0.
- Actual ELK layout regression checks for horizontal/vertical, flat/nested graphs and ports.

### Fixed
- Beautify is one transactional undo/redo operation, including temporary configuration and style updates.
- Retry per container for the reproduced ELK 0.9.0 hierarchical-edge import null-pointer failure. Other failures still roll back.
- Replace the legacy Guice JAR with invalidated signature metadata by official Guice 6.0.0 in 7.0.x packages. Retain the Xbase 2.30 manifest adaptation reproducibly, with unchanged class bytes.
- Full packages use Eclipse's discoverable `dropins/<name>/eclipse/plugins` layout; installers back up older Beautifier copies from both `dropins` and `plugins` and preserve native bundles/configuration.

### Verification
- See [the validation record](docs/capella-7.1-validation.md) for executed checks, tested diagrams and platform limits.

## [1.0.3] - 2026-10-14

### Fixed
- Capella 7.0.0 could not load the plugin because the manifest required Capella 7.0.1 or newer. The dependency range is now `[7.0.0,7.1.0)`.
- Updated build scripts and JAR names for v1.0.3. The layout implementation is unchanged.

### Added
- Isolated OSGi bundle resolution and Java linkage check for compatibility verification.
- Separate full packages for Capella 7.0.0 (Sirius/ELK adapter 7.4.1) and 7.0.1 (adapter 7.4.5), containing the same plugin and ELK 0.9.0. The 7.0.1 package retains the v1.0.2 dependency JARs.

### Verification
- Unchanged Java sources compile against both versions, producing identical class bytes.
- The plugin, full-package dependencies, configuration factory and handler linkage pass isolated OSGi checks on both versions. The published v1.0.2 plugin fails resolution on 7.0.0 as expected.
- The maintainer manually confirmed Beautify H and Beautify V work on both Capella 7.0.0 and 7.0.1 on macOS. Windows and Linux interactive tests were not performed for this release.

## [1.0.2] - 2026-03-13

### Fixed
- ELK INCLUDE_CHILDREN crashing on Windows (NPE in LayerSweepCrossingMinimizer) — removed semiInteractive and feedbackEdges options that triggered the bug
- Physical Ports being resized by post-processing — removed manual node size enforcement, ELK handles sizing natively

### Removed
- Manual node size post-processing (collectNodeSizeChanges) — was interfering with port sizes and causing scrollbar issues

## [1.0.1] - 2026-03-11

### Fixed
- Ctrl+Z undo corrupting ports and edges on Windows — post-processing now runs inside a RecordingCommand

### Added
- Fallback layout without INCLUDE_CHILDREN for complex PABs with hierarchical edge crashes

### Changed
- ELK config is now transient — original layout config is restored after arrange

## [1.0.0] - 2026-03-11

### Added
- "Beautify H" button — horizontal layout (left-to-right flow)
- "Beautify V" button — vertical layout (top-to-bottom flow)
- Automatic ELK Layered configuration injection via SessionManagerListener
- Support for all Capella 7.0.1 diagram types (SAB, LAB, PAB, SDFB, OAB, etc.)
- Post-processing: functional chain style refresh, physical path style refresh, zero-sized node normalization
- Build scripts for macOS and Windows
- Installation scripts for macOS and Windows
