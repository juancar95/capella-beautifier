from io import BytesIO
import hashlib
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch
from zipfile import ZipFile

ROOT = Path(__file__).parents[1]
SPEC = importlib.util.spec_from_file_location('dependencies', ROOT / 'scripts/fetch-test-dependencies.py')
DEPENDENCIES = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(DEPENDENCIES)


class DependencyAdaptationTest(unittest.TestCase):
    def test_only_manifest_and_invalidated_signatures_change(self):
        original = BytesIO()
        with ZipFile(original, 'w') as jar:
            jar.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\r\nRequire-Bundle: com.google.guava;bundle-version="[30.1.0,31.0.0)";visibility:=reexport\r\n\r\n')
            jar.writestr('META-INF/OLD.SF', b'invalidated signature')
            jar.writestr('META-INF/OLD.RSA', b'invalidated certificate')
            jar.writestr('META-INF/LICENSE', b'upstream license')
            jar.writestr('example/Library.class', b'unchanged class bytes')
        source = original.getvalue()
        with patch.object(DEPENDENCIES, 'XBASE_SOURCE_SHA256', hashlib.sha256(source).hexdigest()):
            first = DEPENDENCIES.adapt_xbase(source)
            self.assertEqual(first, DEPENDENCIES.adapt_xbase(source))
        with ZipFile(BytesIO(first)) as jar:
            manifest = jar.read('META-INF/MANIFEST.MF').decode().replace('\r\n ', '')
            self.assertIn('[30.1.0,32.0.0)', manifest)
            self.assertTrue(all(len(line) <= 72 for line in jar.read('META-INF/MANIFEST.MF').splitlines()))
            self.assertEqual(jar.read('example/Library.class'), b'unchanged class bytes')
            self.assertEqual(jar.read('META-INF/LICENSE'), b'upstream license')
            self.assertNotIn('META-INF/OLD.SF', jar.namelist())
            self.assertNotIn('META-INF/OLD.RSA', jar.namelist())

    def test_rejects_unverified_upstream_bytes(self):
        with self.assertRaisesRegex(ValueError, 'Unexpected upstream'):
            DEPENDENCIES.adapt_xbase(b'unknown artifact')


class InstallationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='beautifier installer with spaces ')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.project = self.root / 'repo'
        (self.project / 'scripts').mkdir(parents=True)
        (self.project / 'META-INF').mkdir()
        (self.project / 'dist').mkdir()
        (self.project / 'META-INF/MANIFEST.MF').write_text('Bundle-Version: 1.0.4\n')
        self.script = self.project / 'scripts/install-macos.sh'
        shutil.copy2(ROOT / 'scripts/install-macos.sh', self.script)
        self.capella = self.root / 'Capella Eclipse'
        for n in ['plugins', 'dropins/other/eclipse/plugins', 'configuration']:
            (self.capella / n).mkdir(parents=True)
        (self.capella / 'plugins/org.polarsys.capella.core.sirius.analysis_7.1.0.test.jar').write_bytes(b'analysis')
        (self.capella / 'plugins/native.jar').write_bytes(b'preserve native library')
        (self.capella / 'configuration/config.ini').write_text('unchanged configuration')
        (self.capella / 'capella.ini').write_text('-vmargs\n-Xmx3g\n')
        for n in ['plugins/capella-beautify-1.0.2.jar', 'dropins/other/eclipse/plugins/capella-beautify-1.0.3.jar']:
            (self.capella / n).write_bytes(b'old plugin')
        root = 'capella-beautifier-v1.0.4-capella-7.1.0-full'
        with ZipFile(self.project / 'dist' / f'{root}.zip', 'w') as archive:
            archive.writestr(f'{root}/dropins/capella-beautifier/eclipse/plugins/capella-beautify-1.0.4.jar', b'new plugin')
            archive.writestr(f'{root}/dropins/capella-beautifier/eclipse/plugins/engine.jar', b'engine')

    def run_installer(self):
        return subprocess.run(['bash', str(self.script)], env={**os.environ, 'CAPELLA_HOME': str(self.capella)}, capture_output=True, text=True)

    def test_backs_up_all_old_copies_and_keeps_native_files(self):
        result = self.run_installer()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.capella / 'dropins/capella-beautifier/eclipse/plugins/capella-beautify-1.0.4.jar').read_bytes(), b'new plugin')
        self.assertEqual((self.capella / 'plugins/native.jar').read_bytes(), b'preserve native library')
        self.assertEqual((self.capella / 'configuration/config.ini').read_text(), 'unchanged configuration')
        self.assertEqual((self.capella / 'capella.ini').read_text(), '-vmargs\n-Xmx3g\n')
        self.assertFalse((self.capella / 'plugins/capella-beautify-1.0.2.jar').exists())
        self.assertFalse((self.capella / 'dropins/other/eclipse/plugins/capella-beautify-1.0.3.jar').exists())
        self.assertEqual(len(list((self.capella / 'backup').rglob('capella-beautify-*.jar'))), 2)
        self.assertEqual(self.run_installer().returncode, 0)
        self.assertEqual(len(list((self.capella / 'dropins').rglob('capella-beautify-*.jar'))), 1)

    def test_missing_package_fails_before_mutation(self):
        next((self.project / 'dist').glob('*.zip')).unlink()
        result = self.run_installer()
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue((self.capella / 'plugins/capella-beautify-1.0.2.jar').exists())
        self.assertFalse((self.capella / 'backup').exists())


if __name__ == '__main__':
    unittest.main()
