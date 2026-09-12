#!/usr/bin/env python3
"""W2A semantic mutations, immutable receipts, byte-exact restoration and second GREEN."""
import gzip
import hashlib
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'src/main/java/io/github/gustavo2358/cobolexplorer'
OUT = ROOT / os.getenv('W2A_CHALLENGE_OUT', 'target/cp6-w2a/challenges')
TESTS = 'IfCheckpointW2ATest,IfCanonicalProofTest,IfSemanticsScaleTest'
BUILDER = 'AstBuilder.java'
ENGINE = 'IfSemantics.java'
WRITER = 'semanticproduct/transport/SemanticProductJsonWriter.java'
PROJECTOR = 'semanticproduct/projection/CobolSemanticProductProjector.java'
CHALLENGES = [
    ('01-inner-move-completion-erased', [(BUILDER,
        'current instanceof Ast.MoveStatement || current instanceof Ast.IfStatement',
        'region.topLevel() && current instanceof Ast.MoveStatement || current instanceof Ast.IfStatement')]),
    ('02-then-else-swapped', [(WRITER, 'arm(branch.thenArm()), arm(branch.elseArm()), branch.profile()',
        'arm(branch.elseArm()), arm(branch.thenArm()), branch.profile()')]),
    ('03-absent-else-made-present', [(BUILDER,
        'context.ifElse() == null ? Ast.BranchPresence.ABSENT : Ast.BranchPresence.PRESENT',
        'context.ifElse() == null ? Ast.BranchPresence.PRESENT : Ast.BranchPresence.PRESENT')]),
    ('04-known-flag-read-erased', [(WRITER,
        'p.knownReads().stream().map(SemanticProductJsonWriter::operandHandle).toList(), p.readsCompleteness()',
        'List.of(), p.readsCompleteness()')]),
    ('05-incomplete-reads-falsely-complete', [(WRITER,
        'p.knownReads().stream().map(SemanticProductJsonWriter::operandHandle).toList(), p.readsCompleteness()',
        'List.of(), CobolSemanticProduct.ReadsCompleteness.COMPLETE')]),
    ('06-whole-item-read-erased', [(WRITER,
        '.map(access -> new WholeItemDocument(dataHandle(access.data()))).orElse(null)',
        '.filter(access -> reference.role() != CobolSemanticProduct.OperandRole.READ).map(access -> new WholeItemDocument(dataHandle(access.data()))).orElse(null)')]),
    ('07-predicate-outside-slice-promoted', [(ENGINE,
        'relation.operatorKind() == Ast.RelationOperator.EQUAL',
        'relation.operatorKind() != Ast.RelationOperator.UNAVAILABLE')]),
    ('08-distinct-identities-used-as-proof', [(ENGINE,
        'independent &= eligible;', 'independent &= entity != null;')]),
    ('09-redefines-ignored', [('ScalarMoveSemantics.java',
        'boolean overlay = hasOverlay(section, counts);', 'boolean overlay = false;'),
        (ENGINE, 'independent &= eligible;', 'independent &= entity != null;')]),
    ('10-nested-ownership-flattened', [(PROJECTOR,
        'collectStatementGroup(conditional.thenBranch(), conditional,',
        'collectStatementGroup(conditional.thenBranch(), parent == null ? conditional : parent,'),
        (PROJECTOR, 'collectStatementGroup(conditional.elseBranch(), conditional,',
        'collectStatementGroup(conditional.elseBranch(), parent == null ? conditional : parent,')]),
    ('11-control-derived-from-node-id', [(BUILDER,
        'result.put(current.meta().id(), next.meta().id());',
        'result.put(current.meta().id(), current.meta().id() + 1);')]),
    ('12-control-derived-from-program-point', [(PROJECTOR,
        '"canonical MOVE continuation is not a statement");\n                return Objects.requireNonNull(statementIds.get((Ast.Statement) node), "continuation must be published");',
        '"canonical MOVE continuation is not a statement");\n                return statementIds.get(inputs.statementPositions().get(plan.position().ordinal() + 1).statement());')]),
    ('13-predicate-provenance-lost', [(WRITER,
        'provenance(p.provenance()), p.gapCodes());', 'null, p.gapCodes());')]),
]


def digest(data):
    return hashlib.sha256(data).hexdigest()


def source_hashes():
    return {str(p.relative_to(ROOT)): digest(p.read_bytes()) for p in sorted((ROOT / 'src').rglob('*')) if p.is_file()}


def run(label):
    command = [os.getenv('MAVEN_BIN', 'mvn'), '-q', '-Dtest=' + TESTS, 'test']
    log = OUT / (label + '.log')
    with log.open('xb') as stream:
        result = subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT)
    data = log.read_bytes()
    text = data.decode(errors='replace')
    return {'label': label, 'command': command, 'exitCode': result.returncode,
            'logSha256': digest(data), 'sourceFiles': source_hashes(),
            'semanticRed': result.returncode != 0 and 'COMPILATION ERROR' not in text
            and ('Failures:' in text or 'Errors:' in text)}


def main():
    OUT.mkdir(parents=True, exist_ok=False)
    results = []
    baseline = {p: p.read_bytes() for p in BASE.rglob('*.java')}
    results.append(run('green-before'))
    if results[-1]['exitCode'] != 0:
        (OUT / 'results.json').write_text(json.dumps(results, indent=2) + '\n')
        raise RuntimeError('Initial GREEN failed')
    for label, changes in CHALLENGES:
        result = None
        try:
            for name, before, after in changes:
                path = BASE / name
                text = path.read_text()
                if text.count(before) != 1:
                    raise RuntimeError(label + ': expected exactly one mutation site')
                path.write_text(text.replace(before, after))
            result = run(label)
            results.append(result)
            if not result['semanticRed']:
                raise RuntimeError(label + ': survived or nonsemantic failure')
        finally:
            for path in {BASE / name for name, _, _ in changes}:
                path.write_bytes(baseline[path])
            restored = all(path.read_bytes() == original for path, original in baseline.items())
            if result is not None:
                result['restoredByteExact'] = restored
            (OUT / 'results.json').write_text(json.dumps(results, indent=2) + '\n')
            if not restored:
                raise RuntimeError('Production source changed beyond the controlled mutation')
        print(label + ': semantic RED; restoration byte-exact', flush=True)
    results.append(run('green-restored'))
    (OUT / 'results.json').write_text(json.dumps(results, indent=2) + '\n')
    if results[-1]['exitCode'] != 0:
        raise RuntimeError('Restored GREEN failed')
    print('W2A: 13 semantic mutants rejected, exact restoration, second GREEN.', flush=True)


if __name__ == '__main__':
    main()
