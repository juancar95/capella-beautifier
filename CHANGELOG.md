# Changelog

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
