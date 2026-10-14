#!/usr/bin/env python3
"""Package one tested JAR with the dependency sets for Capella 7.0.0 and 7.0.1."""

import argparse
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capella_700_plugins", type=Path)
    parser.add_argument("capella_701_plugins", type=Path)
    args = parser.parse_args()
    project = Path(__file__).resolve().parent.parent
    headers = dict(
        line.split(": ", 1)
        for line in (project / "META-INF/MANIFEST.MF").read_text().splitlines()
        if ": " in line
    )
    version = headers["Bundle-Version"]
    plugin = project / "dist" / f"capella-beautify-{version}.jar"
    if not plugin.is_file():
        parser.error(f"Build the plugin first: {plugin}")
    with ZipFile(plugin) as jar:
        manifest = jar.read("META-INF/MANIFEST.MF").decode().replace("\r\n", "\n").replace("\n ", "")
        if f"Bundle-Version: {version}\n" not in manifest:
            parser.error("The built JAR version does not match the source manifest")
    packages = [
        ("7.0.0", "7.4.1.202405151520", args.capella_700_plugins),
        ("7.0.1", "7.4.5.202411261603", args.capella_701_plugins),
    ]
    dependencies = []
    for capella, sirius, directory in packages:
        jars = sorted(directory.glob("*.jar"))
        bridges = sorted(directory.glob("org.eclipse.sirius.diagram.elk_*.jar"))
        expected = directory / f"org.eclipse.sirius.diagram.elk_{sirius}.jar"
        if len(jars) != 10 or bridges != [expected]:
            parser.error(f"Expected ten dependency JARs and Sirius {sirius} for Capella {capella}")
        dependencies.append({jar.name: jar.read_bytes() for jar in jars if jar not in bridges})
    if dependencies[0] != dependencies[1]:
        parser.error("The dependency sets must differ only in the Sirius/ELK adapter")
    for capella, _, directory in packages:
        name = f"capella-beautifier-v{version}-capella-{capella}-full"
        output = project / "dist" / f"{name}.zip"
        with ZipFile(output, "w", compression=ZIP_DEFLATED) as archive:
            archive.write(plugin, f"{name}/{plugin.name}")
            for jar in sorted(directory.glob("*.jar")):
                archive.write(jar, f"{name}/plugins/{jar.name}")
            for license_file in ["LICENSE", "THIRD_PARTY_LICENSES.md"]:
                archive.write(project / license_file, f"{name}/{license_file}")
        print(output)


if __name__ == "__main__":
    main()
