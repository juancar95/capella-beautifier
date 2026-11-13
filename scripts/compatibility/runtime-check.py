#!/usr/bin/env python3
"""Run the same Java runtime checks on macOS, Windows and Linux."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

SOURCES = Path(__file__).resolve().parent


def run(command):
    subprocess.run(command, check=True, timeout=120)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('check', choices=['bundles', 'elk'])
    parser.add_argument('capella', type=Path)
    parser.add_argument('dependencies', type=Path)
    parser.add_argument('plugin', type=Path, nargs='?')
    parser.add_argument('expected', choices=['resolved', 'unresolved'], nargs='?')
    args = parser.parse_args()
    capella = args.capella.resolve()
    dependencies = args.dependencies.resolve()
    with tempfile.TemporaryDirectory(prefix='capella-runtime-check-') as temporary:
        directory = Path(temporary)
        if args.check == 'bundles':
            if not args.plugin or not args.expected:
                parser.error('bundles requires a plugin JAR and expected resolution')
            frameworks = list((capella / 'plugins').glob('org.eclipse.osgi_*.jar'))
            if len(frameworks) != 1:
                parser.error('Expected one Equinox framework JAR')
            framework = str(frameworks[0])
            run(['javac', '--release', '17', '-cp', framework, '-d', temporary,
                 str(SOURCES / 'BundleCompatibilityCheck.java')])
            run(['java', '-cp', os.pathsep.join([temporary, framework]), 'BundleCompatibilityCheck',
                 str(capella / 'plugins'), str(dependencies), str(args.plugin.resolve()),
                 str(directory / 'framework'), args.expected])
        else:
            classpath = os.pathsep.join([str(dependencies / '*'), str(capella / 'plugins' / '*')])
            run(['javac', '--release', '17', '-sourcepath', '', '-cp', classpath, '-d', temporary,
                 str(SOURCES / 'ElkLayoutCheck.java')])
            run(['java', '-cp', os.pathsep.join([temporary, classpath]), 'ElkLayoutCheck'])


if __name__ == '__main__':
    main()
