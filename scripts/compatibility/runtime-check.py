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
        frameworks = list((capella / 'plugins').glob('org.eclipse.osgi_*.jar'))
        if len(frameworks) != 1:
            parser.error('Expected one Equinox framework JAR')
        framework = str(frameworks[0])
        run(['javac', '--release', '17', '-cp', framework, '-d', temporary,
             str(SOURCES / 'BundleCompatibilityCheck.java')])
        test_arguments = []
        if args.check == 'bundles':
            if not args.plugin or not args.expected:
                parser.error('bundles requires a plugin JAR and expected resolution')
            plugin = args.plugin.resolve()
            expected = args.expected
        else:
            root = SOURCES.parent.parent
            version = next(line.split(': ', 1)[1] for line in (root / 'META-INF/MANIFEST.MF').read_text().splitlines()
                           if line.startswith('Bundle-Version: '))
            plugin = root / 'dist' / f'capella-beautify-{version}.jar'
            expected = 'resolved'
            classpath = os.pathsep.join([str(dependencies / '*'), str(capella / 'plugins' / '*')])
            classes = directory / 'test-classes'
            classes.mkdir()
            run(['javac', '--release', '17', '-sourcepath', '', '-cp', classpath, '-d', str(classes),
                 str(SOURCES / 'ElkLayoutCheck.java')])
            manifest = directory / 'MANIFEST.MF'
            manifest.write_text(
                'Manifest-Version: 1.0\nBundle-ManifestVersion: 2\n'
                'Bundle-SymbolicName: org.polarsys.capella.beautify.runtimecheck\n'
                'Bundle-Version: 1.0.0\n'
                'Require-Bundle: com.google.inject,org.eclipse.elk.core,\n'
                ' org.eclipse.elk.graph,org.eclipse.elk.alg.layered\n'
                'Bundle-RequiredExecutionEnvironment: JavaSE-17\n\n')
            bundle = directory / 'elk-check.jar'
            run(['jar', '--create', '--file', str(bundle), '--manifest', str(manifest), '-C', str(classes), '.'])
            test_arguments = [str(bundle)]
        run(['java', '-cp', os.pathsep.join([temporary, framework]), 'BundleCompatibilityCheck',
             str(capella / 'plugins'), str(dependencies), str(plugin),
             str(directory / 'framework'), expected, *test_arguments])


if __name__ == '__main__':
    main()
