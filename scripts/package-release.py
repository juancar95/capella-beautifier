#!/usr/bin/env python3
"""Package one JAR with checksum-pinned dependencies for each supported Capella."""

import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

PROJECT = Path(__file__).resolve().parent.parent
DEPENDENCIES = json.loads((PROJECT / 'scripts/release-dependencies.json').read_text())
ZIP_DATE = (2026, 1, 1, 0, 0, 0)


def validate_dependencies(version, directory):
    actual = {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
              for p in sorted(directory.glob('*.jar'))}
    if actual != DEPENDENCIES[version]:
        missing = set(DEPENDENCIES[version]) - set(actual)
        extra = set(actual) - set(DEPENDENCIES[version])
        changed = {name for name in actual.keys() & DEPENDENCIES[version].keys()
                   if actual[name] != DEPENDENCIES[version][name]}
        raise ValueError(f'Capella {version}: missing={sorted(missing)}, '
                         f'extra={sorted(extra)}, changed={sorted(changed)}')
    return [directory / name for name in actual]


def write_entry(archive, name, data):
    info = ZipInfo(name, ZIP_DATE)
    info.create_system = 3
    info.compress_type = ZIP_DEFLATED
    info.external_attr = 0o100644 << 16
    archive.writestr(info, data)


def package(version, capella, plugin, dependencies, output):
    root = f'capella-beautifier-v{version}-capella-{capella}-full'
    with ZipFile(output, 'w') as archive:
        for jar in [plugin, *dependencies]:
            write_entry(archive, f'{root}/dropins/capella-beautifier/eclipse/plugins/{jar.name}',
                        jar.read_bytes())
        for name in ['LICENSE', 'THIRD_PARTY_LICENSES.md']:
            write_entry(archive, f'{root}/{name}', (PROJECT / name).read_bytes())
        instructions = (
            f'Capella Beautifier {version} for Capella {capella}\n\n'
            'Close Capella. Remove older Beautifier JARs from BOTH dropins and plugins\n'
            '(also check nested folders). Remove any older capella-beautifier drop-in\n'
            'folder. Keep a backup outside the Capella installation if needed.\n\n'
            'Copy this package\'s capella-beautifier folder into your Capella dropins\n'
            'folder, preserving the eclipse/plugins subfolders. Restart Capella.\n'
            'Use -clean before -vmargs on the first launch if necessary.\n\n'
            'Do not copy these dependencies over the native plugins folder, or use\n'
            'a full package intended for a different Capella version.\n'
        )
        write_entry(archive, f'{root}/INSTALL.txt', instructions.encode())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for version in DEPENDENCIES:
        parser.add_argument('capella_' + version.replace('.', '') + '_plugins', type=Path)
    args = parser.parse_args()
    headers = dict(line.split(': ', 1)
                   for line in (PROJECT / 'META-INF/MANIFEST.MF').read_text().splitlines()
                   if ': ' in line)
    version = headers['Bundle-Version']
    plugin = PROJECT / 'dist' / f'capella-beautify-{version}.jar'
    if not plugin.is_file():
        parser.error(f'Build the plugin first: {plugin}')
    with ZipFile(plugin) as jar:
        manifest = jar.read('META-INF/MANIFEST.MF').decode().replace('\r\n', '\n').replace('\n ', '')
        source_manifest = (PROJECT / 'META-INF/MANIFEST.MF').read_text()
        for line in source_manifest.splitlines():
            if line.startswith(('Bundle-Version:', 'Require-Bundle:')) and line not in manifest:
                parser.error('The built JAR manifest does not match the source')
    # Validate every set before writing any output.
    try:
        sets = {v: validate_dependencies(v, getattr(args, 'capella_' + v.replace('.', '') + '_plugins'))
                for v in DEPENDENCIES}
    except ValueError as error:
        parser.error(str(error))
    outputs = [plugin]
    for capella, dependencies in sets.items():
        output = PROJECT / 'dist' / f'capella-beautifier-v{version}-capella-{capella}-full.zip'
        package(version, capella, plugin, dependencies, output)
        outputs.append(output)
        print(output)
    checksums = PROJECT / 'dist' / f'capella-beautifier-v{version}-SHA256SUMS.txt'
    checksums.write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n'
                                 for p in outputs))
    print(checksums)


if __name__ == '__main__':
    main()
