import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from zipfile import ZipFile

SPEC = importlib.util.spec_from_file_location('packaging', Path(__file__).parents[1] / 'scripts/package-release.py')
PACKAGING = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PACKAGING)


class ReleasePackagingTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        self.jar = self.directory / 'adapter.jar'
        self.jar.write_bytes(b'verified dependency')
        self.pins = {'7.1.0': {self.jar.name: hashlib.sha256(self.jar.read_bytes()).hexdigest()}}
        self.patcher = patch.object(PACKAGING, 'DEPENDENCIES', self.pins)
        self.patcher.start()
        self.addCleanup(self.patcher.stop)

    def test_accepts_exact_verified_dependency_set(self):
        self.assertEqual(PACKAGING.validate_dependencies('7.1.0', self.directory), [self.jar])

    def test_rejects_modified_dependency(self):
        self.jar.write_bytes(b'wrong version with same filename')
        with self.assertRaisesRegex(ValueError, 'changed=.*adapter.jar'):
            PACKAGING.validate_dependencies('7.1.0', self.directory)

    def test_rejects_missing_dependency(self):
        self.jar.unlink()
        with self.assertRaisesRegex(ValueError, 'missing=.*adapter.jar'):
            PACKAGING.validate_dependencies('7.1.0', self.directory)

    def test_rejects_extra_old_plugin(self):
        (self.directory / 'old-adapter.jar').write_bytes(b'stale')
        with self.assertRaisesRegex(ValueError, 'extra=.*old-adapter.jar'):
            PACKAGING.validate_dependencies('7.1.0', self.directory)

    def test_release_uses_discoverable_dropins_tree_and_reproducible_bytes(self):
        plugin = self.directory / 'capella-beautify-1.0.4.jar'
        plugin.write_bytes(b'tested plugin')
        first = self.directory / 'first.zip'
        second = self.directory / 'second.zip'
        PACKAGING.package('1.0.4', '7.1.0', plugin, [self.jar], first)
        # Timestamps and host file permissions must not alter release contents.
        plugin.chmod(0o600)
        PACKAGING.package('1.0.4', '7.1.0', plugin, [self.jar], second)
        self.assertEqual(first.read_bytes(), second.read_bytes())
        with ZipFile(first) as archive:
            root = 'capella-beautifier-v1.0.4-capella-7.1.0-full'
            self.assertEqual(archive.read(f'{root}/dropins/capella-beautifier/eclipse/plugins/{plugin.name}'), plugin.read_bytes())
            self.assertEqual(archive.read(f'{root}/dropins/capella-beautifier/eclipse/plugins/{self.jar.name}'), self.jar.read_bytes())
            self.assertFalse(any(n.startswith(f'{root}/plugins/') for n in archive.namelist()))
            self.assertIn('BOTH dropins and plugins', archive.read(f'{root}/INSTALL.txt').decode())


if __name__ == '__main__':
    unittest.main()
