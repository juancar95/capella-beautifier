# Third-Party Licenses

The full installation package (zip) redistributes the following third-party libraries. Each JAR contains its own license in `META-INF/`.

| Library | Version | License |
|---------|---------|---------|
| Eclipse ELK Core | 0.9.0 | EPL 2.0 |
| Eclipse ELK Layered | 0.9.0 | EPL 2.0 |
| Eclipse ELK Common | 0.9.0 | EPL 2.0 |
| Eclipse ELK Rect Packing | 0.9.0 | EPL 2.0 |
| Eclipse ELK Core Service | 0.9.0 | EPL 2.0 |
| Eclipse ELK Graph | 0.9.0 | EPL 2.0 |
| Sirius Diagram ELK | 7.4.1 (Capella 7.0.0 package) / 7.4.5 (Capella 7.0.1 package) / 7.4.14 (Capella 7.1.0 package) | EPL 2.0 |
| Google Guice | 6.0.0 (7.0.x) / 7.0.0 (7.1.0) | Apache 2.0 |
| Eclipse Xtext Xbase Lib | 2.30.0 (7.0.x) / 2.34.0 (7.1.0) | EPL 2.0 |
| AOP Alliance | 1.0.0 | Public Domain |
| Jakarta Inject API | 2.0.1 | Apache 2.0 |

## License Links

Redistributed JARs are pinned by SHA-256 in `scripts/release-dependencies.json`. All are unmodified upstream artifacts except the Xbase 2.30 manifest adaptation described below. `scripts/fetch-test-dependencies.py` records their download locations: Eclipse Sirius, the official Capella ELK update site, Eclipse Xtext, and Maven Central for Google Guice 6.0.0. The legacy modified Guice 5.0.1 JAR is no longer distributed.

For Capella 7.0.x, Xbase 2.30 retains the Guava dependency range adaptation used by earlier Beautifier releases: `[30.1.0,31.0.0)` becomes `[30.1.0,32.0.0)` to accept Capella's Guava 31.1. The script reproduces this from the checksum-verified official Eclipse artifact, removes any invalidated signature metadata, and preserves all 117 Java class files byte for byte. No Xbase Java source is changed. The script and upstream artifact URL provide the complete manifest patch; the library remains under EPL-2.0.

ELK remains at 0.9.0 in all packages. Jakarta Inject API 2.0.1 is included for Guice 6/7. Library license notices remain inside each JAR.

- **EPL 2.0**: https://www.eclipse.org/legal/epl-2.0/
- **Apache 2.0**: https://www.apache.org/licenses/LICENSE-2.0
