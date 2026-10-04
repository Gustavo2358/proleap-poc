/* Counts and details come directly from Semantic Product.gaps. */
(function () {
  'use strict';
  const data = window.SEMANTIC_GAPS;
  const el = id => document.getElementById(id);
  const node = (tag, text, className) => {
    const n = document.createElement(tag);
    if (text !== undefined) n.textContent = text;
    if (className) n.className = className;
    return n;
  };
  if (!data) { el('result-count').textContent = 'O arquivo de gaps não foi carregado.'; return; }
  const fmt = n => n.toLocaleString('pt-BR');
  let page = 0;
  const pageSize = 50;
  for (const [i, unit] of data.units.entries()) {
    const option = node('option', `${unit.unit.canonicalProgramName} · unidade ${i + 1}`);
    option.value = String(i);
    el('unit-filter').append(option);
  }
  function selectedUnits() {
    const value = el('unit-filter').value;
    return value === '' ? data.units : [data.units[Number(value)]];
  }
  function refreshSummary() {
    const gaps = selectedUnits().flatMap(u => u.gaps);
    el('counts').replaceChildren();
    const card = node('div', undefined, 'count');
    card.append(node('strong', fmt(gaps.length)), node('span', 'Gaps ativos'));
    el('counts').append(card);
    const counts = new Map();
    for (const gap of gaps) counts.set(gap.code, (counts.get(gap.code) || 0) + 1);
    const ranking = [...counts].map(([code, count]) => ({code, count})).sort((a, b) => b.count - a.count || a.code.localeCompare(b.code));
    el('ranking').replaceChildren();
    for (const item of ranking) {
      const row = node('tr');
      const first = node('td');
      const button = node('button', item.code);
      button.type = 'button';
      button.addEventListener('click', () => { el('search').value = item.code; page = 0; render(); el('details-title').scrollIntoView(); });
      first.append(button); row.append(first);
      row.append(node('td', fmt(item.count)));
      el('ranking').append(row);
    }
  }
  function render() {
    const query = el('search').value.trim().toLowerCase();
    const rows = selectedUnits().flatMap(unit => unit.gaps.map((gap, index) => ({ unit, gap, index }))).filter(({ unit, gap }) => {
      return !query || [unit.unit.canonicalProgramName, gap.code, gap.statement, gap.detail,
        gap.provenance.original.file, gap.provenance.original.startLine].join(' ').toLowerCase().includes(query);
    });
    const pages = Math.max(1, Math.ceil(rows.length / pageSize));
    page = Math.min(page, pages - 1);
    el('result-count').textContent = `${fmt(rows.length)} registros correspondem aos filtros.`;
    el('page-number').textContent = `${page + 1} / ${pages}`;
    el('previous').disabled = page === 0; el('next').disabled = page + 1 === pages;
    const fragment = document.createDocumentFragment();
    for (const { unit, gap, index } of rows.slice(page * pageSize, (page + 1) * pageSize)) {
      const detail = node('details'); const summary = node('summary');
      summary.append(node('b', gap.code));
      const origin = gap.provenance.original;
      summary.append(node('small', `${unit.unit.canonicalProgramName} · ${origin.file}:${origin.startLine} · ${gap.statement} · gaps[${index}]`));
      const list = node('dl');
      const field = (title, value) => { list.append(node('dt', title), node('dd', value)); };
      field('Diagnóstico', gap.detail);
      field('Escopo', gap.scope);
      field('Proveniência completa', JSON.stringify(gap.provenance, null, 2));
      detail.append(summary, list); fragment.append(detail);
    }
    el('occurrences').replaceChildren(fragment);
  }
  el('unit-filter').addEventListener('change', () => { page = 0; refreshSummary(); render(); });
  el('search').addEventListener('input', () => { page = 0; render(); });
  el('previous').addEventListener('click', () => { page--; render(); });
  el('next').addEventListener('click', () => { page++; render(); });
  refreshSummary(); render();
}());
