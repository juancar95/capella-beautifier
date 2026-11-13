# Capella 7.1.0 compatibility

## Design and release gates

Extend the existing plugin to Capella 7.1.0 while retaining 7.0.0 and 7.0.1.
Use the ELK/Sirius integration supplied by 7.1.0; never replace its native
dependencies with the 7.0.x package. Keep the layout implementation unchanged
unless a reproduced incompatibility requires a correction.

Engineering guidance: ISO/IEC 25010 maintainability (small, explicit changes)
and NIST SSDF release evidence (traceable inputs and artifact hashes). This is
not a claim of certification or standards compliance.

Before publication:

1. Verify the official Capella download against its published checksum.
2. Reproduce rejection of v1.0.3 on 7.1.0 in a fresh Equinox framework.
3. Compile and resolve the new plugin and complete dependencies on all three
   versions; exercise configuration creation and handler linkage.
4. Test actual H/V layout, undo/redo, save and reopen on disposable copies of
   the bundled example project. Record the diagrams and platform actually tested.
5. Test installation from the release package in a fresh distribution without
   manually registering bundles; verify old plugin copies are addressed.
6. Inspect the final diff, package the tested JAR, and verify published asset
   hashes. Document known diagram limitations and untested platforms honestly.

## Evidence

Pending execution. A passing compiler or dependency check alone does not prove
interactive layout or undo correctness.
