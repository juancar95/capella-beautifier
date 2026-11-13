#!/usr/bin/env python3
"""Build the same Java 17 plugin on each platform using the target Capella APIs."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parent.parent
PATTERNS = ['org.polarsys.capella.core.sirius.analysis_*.jar', 'org.eclipse.core.commands_*.jar', 'org.eclipse.core.runtime_*.jar', 'org.eclipse.equinox.registry_*.jar', 'org.eclipse.sirius.diagram.model_*.jar', 'org.eclipse.sirius.diagram.ui_*.jar', 'org.eclipse.sirius.diagram_7*.jar', 'org.eclipse.sirius.model_*.jar', 'org.eclipse.gmf.runtime.diagram.ui_*.jar', 'org.eclipse.gmf.runtime.diagram.core_*.jar', 'org.eclipse.gmf.runtime.notation_*.jar', 'org.eclipse.gmf.runtime.common.core_*.jar', 'org.eclipse.gmf.runtime.emf.commands.core_*.jar', 'org.eclipse.emf.workspace_*.jar', 'org.eclipse.core.resources_*.jar', 'org.eclipse.core.jobs_*.jar', 'org.eclipse.emf.ecore.change_*.jar', 'org.eclipse.gef_*.jar', 'org.eclipse.swt_*.jar', 'org.eclipse.ui.workbench_*.jar', 'org.eclipse.ui.views.properties.tabbed_*.jar', 'org.eclipse.jface_*.jar', 'org.eclipse.equinox.common_*.jar', 'org.eclipse.emf.ecore_*.jar', 'org.eclipse.emf.common_*.jar', 'org.apache.log4j_*.jar', 'org.eclipse.gmf.runtime.diagram.ui.actions_*.jar', 'org.eclipse.gmf.runtime.common.ui.action_*.jar', 'org.eclipse.gmf.runtime.common.ui_*.jar', 'org.eclipse.draw2d_*.jar', 'org.eclipse.osgi_*.jar', 'org.eclipse.emf.transaction_*.jar', 'org.eclipse.sirius_7*.jar', 'org.eclipse.sirius.common_*.jar', 'org.eclipse.emf.edit_*.jar']


def main():
    default = 'C:/Program Files/Capella/eclipse' if os.name == 'nt' else '/Applications/Capella.app/Contents/Eclipse'
    capella = Path(os.environ.get('CAPELLA_HOME', default))
    plugins = capella / 'plugins'
    if not plugins.is_dir():
        raise SystemExit(f'Capella plugins not found: {plugins}. Set CAPELLA_HOME to the Eclipse directory.')
    native = 'win32.win32.*' if os.name == 'nt' else 'cocoa.macosx.*' if sys.platform == 'darwin' else 'gtk.linux.*'
    classpath = []
    for pattern in [*PATTERNS, f'org.eclipse.swt.{native}_*.jar']:
        matches = sorted(plugins.glob(pattern))
        if len(matches) != 1:
            raise SystemExit(f'Expected one match for {pattern}, found {len(matches)} in {plugins}')
        classpath.append(matches[0].resolve().as_posix())
    version = next(line.split(': ', 1)[1] for line in (ROOT / 'META-INF/MANIFEST.MF').read_text().splitlines()
                   if line.startswith('Bundle-Version: '))
    output = ROOT / 'dist' / f'capella-beautify-{version}.jar'
    output.parent.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='capella-build-') as temporary:
        classes = Path(temporary) / 'classes'
        classes.mkdir()
        arguments = ['--release', '17', '-sourcepath', '', '-cp', os.pathsep.join(classpath),
                     '-d', classes.as_posix(),
                     *[p.as_posix() for p in sorted((ROOT / 'src').rglob('*.java'))]]
        # A javac argument file avoids cmd.exe's command-length and quoting limits.
        argsfile = Path(temporary) / 'javac.args'
        argsfile.write_text(''.join('"' + a.replace('\\', '\\\\').replace('"', '\\"') + '"\n'
                                    for a in arguments), encoding='utf-8')
        print(f'Compiling against {capella}...', flush=True)
        subprocess.run(['javac', '@' + str(argsfile)], check=True, cwd=ROOT)
        subprocess.run(['jar', '--create', '--file', str(output), '--manifest', str(ROOT / 'META-INF/MANIFEST.MF'),
                        '--date=2026-01-01T00:00:00Z', '-C', str(classes), 'org',
                        '-C', str(ROOT), 'plugin.xml', '-C', str(ROOT), 'icons'], check=True)
    print(f'Built: {output}')


if __name__ == '__main__':
    main()
