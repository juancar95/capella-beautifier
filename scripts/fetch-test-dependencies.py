#!/usr/bin/env python3
"""Fetch release dependencies from their primary sources and verify pinned SHA-256."""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request
from io import BytesIO
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parent.parent
PINS = json.loads((ROOT / 'scripts/release-dependencies.json').read_text())
CAPELLA_SITE = 'https://download.eclipse.org/capella/core/updates/releases/7.1.0/org.polarsys.capella.rcp.site/plugins/'
XTEXT_SITE = 'https://download.eclipse.org/modeling/tmf/xtext/updates/releases/'


def source_url(name, version):
    if name == 'com.google.inject_6.0.0.jar':
        return 'https://repo.maven.apache.org/maven2/com/google/inject/guice/6.0.0/guice-6.0.0.jar'
    if name.startswith('jakarta.inject.'):
        return XTEXT_SITE + '2.34.0/plugins/' + name
    if name.startswith('com.google.inject_7.') or name.startswith('org.eclipse.xtext.xbase.lib_2.34.') or name.startswith('org.aopalliance_1.0.0.v20230720'):
        return XTEXT_SITE + '2.34.0/plugins/' + name
    if name.startswith(('org.eclipse.xtext.xbase.lib_2.30.', 'org.aopalliance_1.0.0.v20220404')):
        return XTEXT_SITE + '2.30.0/plugins/' + name
    if name.startswith('org.eclipse.sirius.diagram.elk_'):
        sirius = {'7.0.0': '7.4.1', '7.0.1': '7.4.5', '7.1.0': '7.4.14'}[version]
        return f'https://download.eclipse.org/sirius/updates/releases/{sirius}/2023-03/plugins/{name}'
    return CAPELLA_SITE + name


# The 7.0.x distributions ship Guava 31.1. The original Xbase 2.30 manifest
# unnecessarily excludes it; the earlier Beautifier release widened this range.
# Reproduce that manifest-only adaptation, preserving every library class byte.
XBASE_NAME = 'org.eclipse.xtext.xbase.lib_2.30.0.v20230227-1111.jar'
XBASE_SOURCE_SHA256 = '013c91fa2fa77a9c40a2e4b1cdaffaac62c655094d79993f0cdd7885e7b30c7e'


def adapt_xbase(data):
    if hashlib.sha256(data).hexdigest() != XBASE_SOURCE_SHA256:
        raise ValueError('Unexpected upstream Xbase artifact')
    output = BytesIO()
    with ZipFile(BytesIO(data)) as source, ZipFile(output, 'w') as target:
        for name in sorted(source.namelist()):
            if name.upper().startswith('META-INF/') and name.upper().endswith(('.SF', '.RSA', '.DSA', '.EC')):
                continue
            payload = source.read(name)
            if name == 'META-INF/MANIFEST.MF':
                text = payload.decode().replace('\r\n', '\n').replace('\n ', '').split('\n\n')[0]
                old = 'com.google.guava;bundle-version="[30.1.0,31.0.0)"'
                if text.count(old) != 1:
                    raise ValueError('Expected exactly one Xbase Guava dependency')
                text = text.replace(old, 'com.google.guava;bundle-version="[30.1.0,32.0.0)"')
                # JAR headers are UTF-8 and may occupy at most 72 bytes per line.
                lines = []
                for line in text.splitlines():
                    while len(line.encode()) > 70:
                        split = 70
                        while len(line[:split].encode()) > 70:
                            split -= 1
                        lines.append(line[:split]); line = ' ' + line[split:]
                    lines.append(line)
                payload = ('\r\n'.join(lines) + '\r\n\r\n').encode()
            info = ZipInfo(name, (2026, 1, 1, 0, 0, 0))
            info.create_system = 3
            info.compress_type = ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            target.writestr(info, payload)
    return output.getvalue()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('capella', choices=PINS)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    for name, expected in PINS[args.capella].items():
        destination = args.output / name
        if destination.exists() and hashlib.sha256(destination.read_bytes()).hexdigest() == expected:
            print(f'{name}: verified cached artifact')
            continue
        with urllib.request.urlopen(source_url(name, args.capella), timeout=60) as response:
            data = response.read()
        if name == XBASE_NAME:
            data = adapt_xbase(data)
        if hashlib.sha256(data).hexdigest() != expected:
            raise SystemExit(f'Checksum mismatch: {name}')
        destination.write_bytes(data)
        print(f'{name}: verified download')


if __name__ == '__main__':
    main()
