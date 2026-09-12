#!/usr/bin/env python3
"""Controlled CP4A falsifications. Every mutation restores exact source bytes.
Run only on an idle working tree; logs/hashes remain under target/checkpoint-4a.
Compilation failures do not count as killed semantic mutants.
"""
import hashlib
import json
import os
from pathlib import Path
import subprocess

from lean import require_local
require_local()

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'src/main/java/io/github/gustavo2358/cobolexplorer'
OUT = ROOT / 'target/checkpoint-4a/challenges'
OUT.mkdir(parents=True, exist_ok=True)
PROJECTOR = 'semanticproduct/projection/CobolSemanticProductProjector.java'
MODEL = 'semanticproduct/CobolSemanticProduct.java'
WRITER = 'semanticproduct/transport/SemanticProductJsonWriter.java'
ENGINE = 'ScalarMoveSemantics.java'
TESTS = 'ScalarMoveCheckpoint4ATest,SemanticProductEntryGobackTest'
# Each tuple is a concrete, compiling semantic alteration, not a weakened test.
CHALLENGES = [
 ('01-any-string-literal', [(PROJECTOR,
  'literal.logicalText().isPresent() ? LiteralKind.ALPHANUMERIC : LiteralKind.UNKNOWN',
  'LiteralKind.ALPHANUMERIC')], TESTS),
 ('02-ignore-length', [(ENGINE,
  'literal.logicalText().get().extent() == shape.extent()', 'true')], TESTS),
 ('03a-ignore-occurs', [(ENGINE,
  'else return Optional.empty();', 'else if (!(clause instanceof Ast.OccursClause)) return Optional.empty();')], TESTS),
 ('03b-ignore-overlay', [(ENGINE, 'boolean overlay = hasOverlay(section, counts);', 'boolean overlay = false;')], TESTS),
 ('03c-ignore-refmod', [(ENGINE, '\n                    && target.subscriptGroups().isEmpty() && target.referenceModification() == null', '\n                    && target.subscriptGroups().isEmpty()')], TESTS),
 ('04-wrong-selected', [(PROJECTOR,
  'CobolSemanticProduct.DataItemId selectedId = dataIds.get(selected.entityId());',
  'CobolSemanticProduct.DataItemId selectedId = new DataItemId(dataIds.get(selected.entityId()).unit(), 999);')], TESTS),
 ('05-nominal-implies-whole', [(ENGINE,
  '\n                    && target.subscriptGroups().isEmpty() && target.referenceModification() == null', '')], TESTS),
 ('06-array-order-continuation', [(PROJECTOR,
  'CopySemantics copy = CopySemantics.valueOf(semantic.copy().name());\n            Optional<StatementId> next = semantic.nextStatement().map(nodeId -> {',
  '''CopySemantics copy = CopySemantics.valueOf(semantic.copy().name());
            Optional<StatementId> next = Optional.ofNullable(inputs.statementPositions().indexOf(plan.position()) + 1
                    < inputs.statementPositions().size() ? inputs.statementPositions().get(
                    inputs.statementPositions().indexOf(plan.position()) + 1).statement().meta().id() : null).map(nodeId -> {''')], TESTS),
 ('07-remove-continuation', [(PROJECTOR,
  'copy, continuation, semantic.adjustment()', 'copy, NormalContinuation.unavailable(statementProvenance), semantic.adjustment()')], TESTS),
 ('08-goback-fallthrough', [(MODEL,
  'public enum LocalContinuation { NONE }', 'public enum LocalContinuation { NONE, FALLTHROUGH }'),
  (MODEL, 'public LocalContinuation localContinuation() { return LocalContinuation.NONE; }',
   'public LocalContinuation localContinuation() { return LocalContinuation.FALLTHROUGH; }')], TESTS),
 ('09a-remove-literal-origin', [(WRITER,
  'provenance(source.provenance()), source.logicalValue()', 'null, source.logicalValue()')], TESTS),
 ('09b-remove-target-origin', [(WRITER,
  'binding(reference.binding()), provenance(reference.provenance()), reference.wholeItemAccess()',
  'binding(reference.binding()), null, reference.wholeItemAccess()')], TESTS),
 ('10-duplicate-declaration', [(PROJECTOR,
  'return new DeclarationProjection(Collections.unmodifiableMap(ids), List.copyOf(facts));',
  'if (!facts.isEmpty()) facts.add(facts.get(0));\n        return new DeclarationProjection(Collections.unmodifiableMap(ids), List.copyOf(facts));')],
  'ScalarMoveScaleTest#tenThousandTargetsShareOneDataDeclaration'),
]


def run(label, tests):
    log = OUT / (label + '.log')
    with log.open('wb') as output:
        result = subprocess.run([os.getenv('MAVEN_BIN', 'mvn'), '-q', '-Dtest=' + tests, 'test'],
                                cwd=ROOT, stdout=output, stderr=subprocess.STDOUT)
    text = log.read_text()
    return {'label': label, 'exitCode': result.returncode,
            'logSha256': hashlib.sha256(log.read_bytes()).hexdigest(),
            'semanticRed': result.returncode != 0 and 'COMPILATION ERROR' not in text
                           and ('Failures:' in text or 'Errors:' in text)}


def main():
    results = []
    green = run('green-before', TESTS)
    results.append(green)
    if green['exitCode'] != 0: raise RuntimeError('Initial GREEN failed')
    for label, edits, tests in CHALLENGES:
        originals = {name: (BASE / name).read_bytes() for name, _, _ in edits}
        try:
            for name, before, after in edits:
                path = BASE / name
                source = path.read_text()
                if source.count(before) != 1: raise RuntimeError(f'{label}: expected one mutation site')
                path.write_text(source.replace(before, after))
            result = run(label, tests)
            results.append(result)
            print(json.dumps(result), flush=True)
            if not result['semanticRed']: raise RuntimeError(f'{label}: challenge survived or failed compilation')
        finally:
            for name, original in originals.items():
                (BASE / name).write_bytes(original)
            assert all((BASE / name).read_bytes() == original for name, original in originals.items())
            (OUT / 'results.json').write_text(json.dumps(results, indent=2) + '\n')
    results.append(run('green-restored', TESTS + ',ScalarMoveScaleTest'))
    (OUT / 'results.json').write_text(json.dumps(results, indent=2) + '\n')
    if results[-1]['exitCode'] != 0: raise RuntimeError('Restored second GREEN failed')
    print('CP4A: all semantic challenges RED; exact restore and second GREEN passed.', flush=True)

if __name__ == '__main__':
    main()
