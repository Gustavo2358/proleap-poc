(() => {
  "use strict";
  const data = window.RESOLUTION_DATA;
  const $ = (selector) => document.querySelector(selector);
  const $$ = (selector) => [...document.querySelectorAll(selector)];
  const format = (value) => new Intl.NumberFormat("pt-BR").format(value || 0);
  const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"
  })[character]);

  if (!data) {
    $("#resolution-status").className = "parse-status error";
    $("#resolution-status").innerHTML = "<span></span>resolution-data.js ausente";
    return;
  }

  const units = new Map(data.units.map((unit) => [unit.id, unit]));
  const classifications = data.classifications || [];
  const classificationsByRootOccurrence = new Map(classifications.map((classification) =>
    [`${classification.unitId}#${classification.rootOccurrenceId}`, classification]));
  let selectedId = entryFromHash() ?? data.entries[0]?.id ?? null;

  renderHeader();
  configureFilters();
  bindEvents();
  renderList();
  renderGaps();
  if (selectedId !== null) inspect(selectedId, false);

  function renderHeader() {
    const statusCounts = data.counts.status;
    $("#resolution-metrics").innerHTML = [
      [statusCounts.RESOLVED, "resolved"], [statusCounts.AMBIGUOUS, "ambiguous"],
      [statusCounts.UNRESOLVED, "unresolved"], [statusCounts.UNSUPPORTED, "unsupported"],
      [statusCounts.EXTERNAL_OBSERVED, "external-observed"],
      [classifications.length, "external-inferred"]
    ].map(([value, label]) => `<div class="metric ${label}"><b>${format(value)}</b><span>${label}</span></div>`).join("");
    const ready = data.meta.dependencyAnalysisReady;
    $("#resolution-status").className = `parse-status ${ready ? "ok" : "warn"}`;
    $("#resolution-status").innerHTML = `<span></span>${escapeHtml(data.meta.source)} · ${format(data.meta.references)} referências`;
    $("#completeness-banner").className = `coverage-banner ${ready ? "complete" : "incomplete"}`;
    $("#completeness-banner").innerHTML = ready
      ? `<b>Análise de dependências pronta neste estágio</b><span>Nenhuma lacuna bloqueante foi observada no frontend, coleta ou binding nominal.</span>`
      : `<b>Análise de dependências incompleta</b><span>${format(data.meta.gaps)} lacunas bloqueantes. Não interprete o resultado como inventário completo de dependências.</span>`;
    $("#policy-strip").innerHTML = [
      ["Política", `${data.meta.policyId} @ ${data.meta.policyVersion}`],
      ["QUALIFY", data.meta.qualifyMode], ["Escopo", "artefato atual"],
      ["Classificações externas", `${format(classifications.length)} inferidas`],
      ["Custo", `${format(data.metrics.nominalLookups)} lookups · ${format(data.metrics.candidateInspections)} inspeções`]
    ].map(([label, value]) => `<div><span>${label}</span><b>${escapeHtml(value)}</b></div>`).join("");
  }

  function configureFilters() {
    fillSelect("#unit-filter", data.units.map((unit) => [unit.id, `${unit.path} · ${unit.name}`]));
    fillSelect("#kind-filter", distinct(data.entries.map((entry) => entry.kind)));
    fillSelect("#role-filter", distinct(data.entries.map((entry) => entry.role)));
    fillSelect("#status-filter", Object.keys(data.counts.status).map((value) => [value, `${value} (${format(data.counts.status[value])})`]));
    fillSelect("#reason-filter", distinct(data.entries.map((entry) => entry.reason)));
  }

  function fillSelect(selector, values) {
    const normalized = values.map((value) => Array.isArray(value) ? value : [value, value]);
    $(selector).innerHTML = `<option value="">Todos</option>` + normalized
      .map(([value, label]) => `<option value="${escapeHtml(value)}">${escapeHtml(label)}</option>`).join("");
  }

  function distinct(values) {
    return [...new Set(values)].sort().map((value) => [value, value]);
  }

  function bindEvents() {
    ["#unit-filter", "#kind-filter", "#role-filter", "#status-filter", "#reason-filter"]
      .forEach((selector) => $(selector).addEventListener("change", renderList));
    $("#resolution-search").addEventListener("input", renderList);
    document.addEventListener("keydown", (event) => {
      if (event.key === "/" && document.activeElement !== $("#resolution-search")) {
        event.preventDefault(); $("#resolution-search").focus();
      }
    });
  }

  function filteredEntries() {
    const query = $("#resolution-search").value.trim().toUpperCase();
    const predicates = [
      ["#unit-filter", "unitId"], ["#kind-filter", "kind"], ["#role-filter", "role"],
      ["#status-filter", "status"], ["#reason-filter", "reason"]
    ];
    return data.entries.filter((entry) => predicates.every(([selector, field]) =>
      !$(selector).value || entry[field] === $(selector).value)).filter((entry) => !query ||
        [entry.writtenText, entry.grammarRule, entry.reason, entry.status, entry.role,
          units.get(entry.unitId)?.name].some((value) => String(value || "").toUpperCase().includes(query)));
  }

  function renderList() {
    const filtered = filteredEntries();
    const visible = filtered.slice(0, 500);
    $("#resolution-count").textContent = filtered.length > visible.length
      ? `${format(visible.length)} de ${format(filtered.length)}` : `${format(filtered.length)} resultados`;
    $("#resolution-list").innerHTML = visible.length ? visible.map((entry) => {
      const unit = units.get(entry.unitId);
      return `<button class="resolution-card ${entry.status.toLowerCase()} ${entry.id === selectedId ? "selected" : ""}" data-id="${entry.id}">
        <i class="resolution-state" aria-hidden="true"></i>
        <span class="resolution-card-name"><b>${escapeHtml(entry.writtenText || "<texto preservado>")}</b><small>${escapeHtml(entry.kind)} · ${escapeHtml(entry.role)}</small></span>
        <span class="resolution-card-decision"><b>${escapeHtml(entry.status)}</b><small>${escapeHtml(entry.reason)}</small></span>
        <span class="resolution-card-location">${escapeHtml(unit?.name || "unit")}<small>linha ${format(entry.span.startLine)}</small></span>
      </button>`;
    }).join("") : `<div class="empty-state"><b>Nenhuma referência</b><span>Ajuste os filtros ou a busca.</span></div>`;
    $$("#resolution-list .resolution-card").forEach((card) => card.addEventListener("click", () => inspect(Number(card.dataset.id), true)));
  }

  function inspect(id, updateHash) {
    const entry = data.entries.find((candidate) => candidate.id === id);
    if (!entry) return;
    selectedId = id;
    if (updateHash) history.replaceState(null, "", `#entry=${id}`);
    renderList();
    $("#resolution-id").textContent = `binding #${format(id)}`;
    $("#resolution-kind").textContent = `${entry.kind} · ${entry.role}`.replaceAll("_", " ");
    $("#resolution-name").textContent = entry.writtenText || "<texto preservado>";
    $("#resolution-decision").innerHTML = `<span class="status-badge ${entry.status.toLowerCase()}">${entry.status}</span> ${escapeHtml(entry.reason)}`;
    const unit = units.get(entry.unitId);
    $("#resolution-facts").innerHTML = facts([
      ["Unidade", unit ? `${unit.path} · ${unit.name}` : entry.unitId],
      ["Regra", entry.grammarRule], ["Escopo", `#${entry.scopeId}`],
      ["Preservação", entry.preservation], ["AST", `#${entry.astNodeId}`],
      ["Parse tree", `#${entry.parseNodeId}`], ["Candidatos", entry.candidates.length],
      ["Fonte original", `${entry.provenance.original.file}:${entry.provenance.original.startLine}`]
    ]);
    renderCandidates(entry);
    renderSource(entry);
    const primaryUnit = data.units[0]?.id;
    const astLink = $("#open-reference-ast");
    astLink.href = entry.unitId === primaryUnit ? `ast.html#node=${entry.astNodeId}` : "resolution.html";
    astLink.classList.toggle("disabled", entry.unitId !== primaryUnit);
    astLink.setAttribute("aria-disabled", entry.unitId !== primaryUnit ? "true" : "false");
    astLink.title = entry.unitId === primaryUnit ? "Abrir o uso na AST"
      : "A página AST legada exibe somente a unidade primária; a identidade namespaced foi preservada aqui.";
    $("#open-reference-parse").href = `index.html#node=${entry.parseNodeId}`;
    const dynamicCall = entry.role === "CALL_TARGET" && entry.kind === "DATA";
    $("#resolution-insight").textContent = dynamicCall
      ? "Este binding identifica a declaração da variável usada pelo CALL. Ele não afirma qual programa será chamado: os valores possíveis dependem de CFG, reaching definitions e merge de caminhos."
      : entry.status === "EXTERNAL_OBSERVED"
        ? "O alvo literal é uma dependência externa observada. Esta análise termina no artefato atual e não procura nem inventa um programa externo."
      : entry.status === "AMBIGUOUS"
        ? "Todos os candidatos semanticamente válidos foram preservados. Nenhuma escolha arbitrária foi feita."
        : entry.status === "UNRESOLVED" || entry.status === "UNSUPPORTED"
          ? "Esta lacuna permanece desconhecida e bloqueia uma afirmação de cobertura completa de dependências."
          : "O nome possui uma identidade única sob a política explícita desta execução.";
  }

  function renderCandidates(entry) {
    $("#candidate-inspector").innerHTML = entry.candidates.length ? entry.candidates.map((candidate) => {
      const owner = units.get(candidate.unitId);
      const primaryUnit = data.units[0]?.id;
      const symbols = candidate.symbolIds.map((id) => candidate.unitId === primaryUnit
        ? `<a href="symbols.html#symbol=${id}">símbolo #${id}</a>`
        : `<span>símbolo namespaced #${id}</span>`).join("");
      const attributes = Object.entries(candidate.attributes).map(([key, value]) =>
        `<span><b>${escapeHtml(key)}</b>${escapeHtml(value)}</span>`).join("");
      return `<article class="candidate-card"><header><span>${escapeHtml(candidate.domain)}</span><b>${escapeHtml(candidate.writtenName)}</b></header>
        <p>${escapeHtml(owner ? `${owner.path} · ${owner.name}` : candidate.unitId)} · entity #${candidate.localId}</p>
        ${symbols ? `<nav>${symbols}</nav>` : ""}${attributes ? `<div>${attributes}</div>` : ""}</article>`;
    }).join("") : `<span class="empty">Nenhum candidato. O motivo acima distingue ausência, entrada incompleta e forma sem suporte.</span>`;
  }

  function renderSource(entry) {
    const start = Math.max(1, entry.span.startLine - 2);
    const end = Math.min(data.sourceLines.length, entry.span.endLine + 2);
    const rows = [];
    for (let line = start; line <= end; line++) rows.push(`<div class="source-preview-line ${line >= entry.span.startLine && line <= entry.span.endLine ? "selected" : ""}"><span>${line}</span><code>${escapeHtml(data.sourceLines[line - 1] || "")}</code></div>`);
    $("#resolution-source").innerHTML = rows.join("");
  }

  function renderGaps() {
    $("#gap-count").textContent = `${format(data.gaps.length)} lacunas`;
    $("#gap-list").innerHTML = data.gaps.length ? data.gaps.slice(0, 500).map((gap) => {
      const classification = classificationsByRootOccurrence.get(`${gap.unitId}#${gap.occurrenceId}`);
      const detail = classification
        ? `${classification.technology} · ${classification.kind} · ${classification.certainty} · ${format(classification.coveredOccurrenceIds.length)} occurrences cobertas`
        : `${gap.grammarRule || "frontend"} · linha ${format(gap.line)}`;
      const message = classification
        ? `${classification.constructWrittenText} · ${classification.reason}` : gap.message;
      return `<button class="gap-card" data-occurrence="${gap.occurrenceId}" data-unit="${escapeHtml(gap.unitId || "")}"><span>${escapeHtml(gap.category)}</span><b>${escapeHtml(gap.code)}</b><p>${escapeHtml(message)}</p><small>${escapeHtml(detail)}</small></button>`;
    }).join("") : `<div class="empty-state"><b>Sem lacunas bloqueantes</b><span>O binding nominal está completo para a entrada observada.</span></div>`;
    $$("#gap-list .gap-card").forEach((card) => card.addEventListener("click", () => {
      const occurrence = Number(card.dataset.occurrence);
      if (occurrence < 0) return;
      const entry = data.entries.find((value) => value.unitId === card.dataset.unit && value.occurrenceId === occurrence);
      if (entry) inspect(entry.id, true);
    }));
  }

  function facts(values) {
    return values.map(([term, value]) => `<div><dt>${escapeHtml(term)}</dt><dd>${escapeHtml(value)}</dd></div>`).join("");
  }

  function entryFromHash() {
    const match = location.hash.match(/entry=(\d+)/);
    return match ? Number(match[1]) : null;
  }
})();
