#!/usr/bin/env python3
"""Verify the published discovery without executing any product or probe."""
from pathlib import Path, PurePosixPath
import hashlib
import json
import re
import tarfile

ROOT = Path(__file__).resolve().parent
INVENTORY = json.loads((ROOT / 'PUBLICATION_INVENTORY.json').read_text())
D0 = 'artefatos-e2e/carddemo-architectural-reassessment-d0-20260923/'
CANON = 'artefatos-e2e/carddemo-validation-20260923/'


def sha(stream):
    result = hashlib.sha256()
    while block := stream.read(1024 * 1024):
        result.update(block)
    return result.hexdigest()


def file_sha(path):
    with path.open('rb') as stream:
        return sha(stream)


expected = {item['path']: item for item in INVENTORY['files']}
assert len(expected) == len(INVENTORY['files']), 'Duplicate evidence paths'
observed, originals = {}, {}
for archive in INVENTORY['archives']:
    path = ROOT / archive['path']
    assert file_sha(path) == archive['sha256'], path
    count = 0
    with tarfile.open(path, 'r|gz') as stream:
        for member in stream:
            entry = PurePosixPath(member.name)
            assert member.isfile() and not entry.is_absolute() and '..' not in entry.parts, member.name
            assert entry.suffix not in {'.class', '.pyc', '.jar'}, member.name
            item = expected[member.name]
            assert item['archive'] == path.name and item['bytes'] == member.size
            content = stream.extractfile(member)
            assert content is not None
            retain = (member.name.startswith(D0) and '/' not in member.name[len(D0):]) or member.name == CANON + 'sha256sums.txt'
            if retain:
                data = content.read()
                actual = hashlib.sha256(data).hexdigest()
                originals[member.name] = data
            else:
                actual = sha(content)
            assert actual == item['sha256'], member.name
            assert member.name not in observed, member.name
            observed[member.name] = actual
            count += 1
    assert count == archive['files']
assert set(observed) == set(expected)

external = {item['path']: item['sha256'] for item in INVENTORY['externalReferences']}
assert set(external) == {D0 + 'probes/official/igy6lr40.pdf', D0 + 'probes/official/igy6lr40.txt'}
manifest_counts = {}
for name, prefix, expected_count in [(D0 + 'SHA256SUMS', D0, 1081), (CANON + 'sha256sums.txt', '', 4787)]:
    entries = originals[name].decode().splitlines()
    assert len(entries) == expected_count
    for line in entries:
        expected_sha, relative = line.split('  ', 1)
        key = prefix + relative
        assert observed.get(key, external.get(key)) == expected_sha, key
    manifest_counts[name] = len(entries)

reports = 0
links = re.compile(r'\]\(([^)]+)\)')
for name, original in originals.items():
    if not name.startswith(D0) or not name.endswith('.md'):
        continue
    path = ROOT / name[len(D0):]
    text = path.read_text()
    assert links.sub('](LINK)', text) == links.sub('](LINK)', original.decode()), name
    for link in links.findall(text):
        if link.startswith(('http:', 'https:', '#')):
            continue
        assert not link.startswith('/'), (name, link)
        assert (path.parent / link.split('#')[0]).exists(), (name, link)
    reports += 1
assert reports == 20

formatting = {item['path']: item for item in INVENTORY.get('readableFormattingChanges', [])}
readable_copies = 0
formatted_copies = 0
for name, item in expected.items():
    candidates = [ROOT / 'references' / name]
    if name.startswith(D0):
        relative = name[len(D0):]
        if '/' not in relative and relative.endswith('.md'):
            continue
        candidates.append(ROOT / relative)
    elif name.startswith(CANON) and '/' not in name[len(CANON):]:
        candidates.append(ROOT / 'canonical-summary' / name[len(CANON):])
    for candidate in candidates:
        if candidate.is_file():
            adjustment = formatting.get(candidate.relative_to(ROOT).as_posix())
            if adjustment:
                assert item['sha256'] == adjustment['originalSha256'], candidate
                assert file_sha(candidate) == adjustment['publishedSha256'], candidate
                formatted_copies += 1
            else:
                assert file_sha(candidate) == item['sha256'], candidate
                readable_copies += 1
print(json.dumps({'status': 'PASS', 'archives': len(INVENTORY['archives']),
    'archivedFilesVerified': len(observed), 'originalD0ManifestEntriesAccountedFor': 1081,
    'canonicalManifestEntriesVerified': 4787, 'externalNormativeReferenceFiles': len(external),
    'reportsContentPreserved': reports, 'primaryReportLinks': 'PASS',
    'readableCopiesByteIdentical': readable_copies,
    'readableFormattingChangesVerified': formatted_copies,
    'productTests': 'NOT_RUN_DOCUMENTARY_PUBLICATION'}, indent=2))
