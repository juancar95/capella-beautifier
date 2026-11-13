#!/usr/bin/env python3
"""Compare complete GMF notation for a named diagram in two saved Sirius files."""
import argparse
import hashlib
import xml.etree.ElementTree as ET


def notation(path, name):
    root = ET.parse(path).getroot()
    descriptors = [x for x in root.iter('ownedRepresentationDescriptors') if x.get('name') == name]
    if len(descriptors) != 1:
        raise ValueError(f'Expected one descriptor for {name}, found {len(descriptors)}')
    uid = descriptors[0].get('repPath', '').removeprefix('#')
    representation = next(x for x in root if x.get('uid') == uid)
    annotation = next(x for x in representation if x.get('source') == 'GMF_DIAGRAMS')
    return ET.tostring(annotation)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('before')
    parser.add_argument('after')
    parser.add_argument('diagram')
    parser.add_argument('--expect', choices=['equal', 'different'], required=True)
    args = parser.parse_args()
    before = notation(args.before, args.diagram)
    after = notation(args.after, args.diagram)
    equal = before == after
    for name, data in [('before', before), ('after', after)]:
        print(f'{name}: {len(data)} bytes; SHA-256 {hashlib.sha256(data).hexdigest()}')
    if equal != (args.expect == 'equal'):
        raise SystemExit(f'FAIL: expected notation to be {args.expect}')
    print(f'PASS: notation is {args.expect}')


if __name__ == '__main__':
    main()
