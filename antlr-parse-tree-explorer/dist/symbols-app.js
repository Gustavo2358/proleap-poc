(() => {
  "use strict";

  const data = window.SYMBOL_TABLE_DATA;
  if (!data) throw new Error("symbol-data.js não foi carregado");

  const scopes = data.scopes;
  const symbols = data.symbols;
  const scopeChildren = Array.from({ length: scopes.length }, () => []);
  const symbolsByScope = Array.from({ length: scopes.length }, () => []);
  for (const scope of scopes) if (scope.p >= 0) scopeChildren[scope.p].push(scope.id);
  for (const symbol of symbols) symbolsByScope[symbol.s].push(symbol.id);

  const expanded = new Set(scopes.filter((scope) => depth(scope.id) < 3).map((scope) => scope.id));
  let selectedScope = 0;
  let selectedSymbol = requestedSymbol();
  let activeNamespace = "ALL";
  let includeDescendants = true;

  const $ = (selector) => document.querySelector(selector);
  const $$ = (selector) => [...document.querySelectorAll(selector)];
  const format = (value) => Number(value).toLocaleString("pt-BR");
  const escapeHtml = (value) => String(value ?? "")
    .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;").replaceAll("'", "&#039;");

  function initialize() {
    renderHeader();
    renderNamespaceFilters();
    if (selectedSymbol != null && symbols[selectedSymbol]) {
      selectedScope = symbols[selectedSymbol].s;
      expandAncestors(selectedScope);
    } else selectedSymbol = symbols[0]?.id ?? null;
    renderScopes();
    renderSymbols();
    if (selectedSymbol != null) inspectSymbol(selectedSymbol, false);
    bindEvents();
  }

  function renderHeader() {
    const metrics = [
      [format(data.meta.symbols), "declarações"],
      [format(data.meta.scopes), "escopos"],
      [format(data.meta.dataSymbols), "símbolos de dados"],
      [format(data.meta.procedureSymbols), "símbolos de procedure"]
    ];
    $("#symbols-metrics").innerHTML = metrics.map(([value, label]) =>
      `<div class="metric"><strong>${value}</strong><span>${label}</span></div>`).join("");
    $("#symbols-status").innerHTML = `<span></span>Tabela construída · ${format(data.meta.symbols)} símbolos · ${data.meta.diagnostics} avisos`;
    $("#scope-count").textContent = `${format(scopes.length)} total`;
  }

  function bindEvents() {
    $("#symbol-search").addEventListener("input", renderSymbols);
    $("#symbol-search").addEventListener("keydown", (event) => {
      if (event.key === "Escape") { event.target.value = ""; renderSymbols(); }
    });
    document.addEventListener("keydown", (event) => {
      if (event.key === "/" && document.activeElement !== $("#symbol-search")) {
        event.preventDefault(); $("#symbol-search").focus();
      }
    });
    $("#include-descendants").addEventListener("click", () => {
      includeDescendants = !includeDescendants;
      $("#include-descendants").classList.toggle("active", includeDescendants);
      $("#include-descendants").setAttribute("aria-pressed", String(includeDescendants));
      renderSymbols();
    });
  }

  function renderNamespaceFilters() {
    const namespaces = ["ALL", "DATA", "PROCEDURE", "FILE", "PROGRAM"];
    $("#namespace-filters").innerHTML = namespaces.map((namespace) => {
      const count = namespace === "ALL" ? symbols.length : symbols.filter((symbol) => symbol.ns === namespace).length;
      return `<button class="namespace-filter ${namespace === activeNamespace ? "active" : ""}" data-ns="${namespace}">${namespace === "ALL" ? "Todos" : namespace}<span>${format(count)}</span></button>`;
    }).join("");
    $$("#namespace-filters button").forEach((button) => button.addEventListener("click", () => {
      activeNamespace = button.dataset.ns; renderNamespaceFilters(); renderSymbols();
    }));
  }

  function renderScopes() {
    const rows = [];
    const visit = (id) => {
      const scope = scopes[id];
      const children = scopeChildren[id];
      const open = expanded.has(id);
      const owned = scope.o >= 0;
      rows.push(`<div class="scope-row ${id === selectedScope ? "selected" : ""}" data-id="${id}" style="padding-left:${10 + depth(id) * 15}px" role="treeitem">
        <button class="branch-toggle ${children.length ? (open ? "open" : "") : "leaf"}">›</button>
        <i class="dot ${owned ? "declaration" : "scope"}"></i>
        <span><b>${escapeHtml(scope.n)}</b><small>${scope.k.replaceAll("_", " ")}</small></span>
        <em>${symbolsByScope[id].length}</em>
      </div>`);
      if (open) for (const child of children) visit(child);
    };
    visit(0);
    $("#scope-tree").innerHTML = rows.join("");
    $$("#scope-tree .scope-row").forEach((row) => row.addEventListener("click", (event) => {
      const id = Number(row.dataset.id);
      if (event.target.closest(".branch-toggle") && scopeChildren[id].length) {
        if (expanded.has(id)) expanded.delete(id); else expanded.add(id);
        renderScopes(); return;
      }
      selectedScope = id; selectedSymbol = null; renderScopes(); renderSymbols(); inspectScope(id);
    }));
  }

  function renderSymbols() {
    const query = $("#symbol-search").value.trim().toUpperCase();
    const allowedScopes = new Set(includeDescendants ? descendantScopes(selectedScope) : [selectedScope]);
    const filtered = symbols.filter((symbol) => allowedScopes.has(symbol.s))
      .filter((symbol) => activeNamespace === "ALL" || symbol.ns === activeNamespace)
      .filter((symbol) => !query || `${symbol.n} ${symbol.c} ${symbol.k} ${symbol.ns}`.includes(query));
    const scope = scopes[selectedScope];
    $("#selected-scope-note").innerHTML = `<span>Escopo</span><b>${escapeHtml(scope.n)}</b><small>${includeDescendants ? "incluindo descendentes" : "somente declarações locais"}</small>`;
    $("#result-count").textContent = `${format(filtered.length)} resultado${filtered.length === 1 ? "" : "s"}`;
    $("#symbol-list").innerHTML = filtered.length ? filtered.map((symbol) =>
      `<button class="symbol-card ${symbol.id === selectedSymbol ? "selected" : ""}" data-id="${symbol.id}">
        <i class="symbol-glyph ${symbol.ns.toLowerCase()}">${glyph(symbol)}</i>
        <span class="symbol-card-name"><b>${escapeHtml(symbol.n)}</b><small>${escapeHtml(symbol.k.replaceAll("_", " "))}</small></span>
        <span class="namespace-badge ${symbol.ns.toLowerCase()}">${symbol.ns}</span>
        <span class="flow-line">L${symbol.l}</span>
      </button>`).join("") : `<div class="empty-state"><b>Nenhuma declaração neste recorte.</b><span>Tente incluir subescopos, trocar o namespace ou limpar a busca.</span></div>`;
    $$("#symbol-list .symbol-card").forEach((card) => card.addEventListener("click", () => inspectSymbol(Number(card.dataset.id), true)));
  }

  function inspectSymbol(id, updateScope) {
    const symbol = symbols[id];
    selectedSymbol = id;
    if (updateScope && !descendantScopes(selectedScope).includes(symbol.s)) {
      selectedScope = symbol.s; expandAncestors(selectedScope); renderScopes();
    }
    history.replaceState(null, "", `#symbol=${id}`);
    renderSymbols();
    $("#symbol-id").textContent = `símbolo #${format(id)}`;
    $("#symbol-kind").textContent = symbol.k.replaceAll("_", " ");
    $("#symbol-name").textContent = symbol.n;
    $("#symbol-canonical").textContent = `forma canônica · ${symbol.c}`;
    renderBreadcrumb(symbol.s);
    $("#symbol-facts").innerHTML = [
      ["Namespace", symbol.ns], ["Escopo declarador", `#${symbol.s}`],
      ["AST declaradora", `nó #${symbol.a}`], ["Fonte", symbol.l === symbol.e ? `linha ${symbol.l}` : `linhas ${symbol.l}–${symbol.e}`]
    ].map(([term, value]) => `<div><dt>${term}</dt><dd>${escapeHtml(value)}</dd></div>`).join("");
    const attributes = Object.entries(symbol.x || {});
    $("#symbol-attributes").innerHTML = attributes.length
      ? attributes.map(([name, value]) => `<div class="attribute-row"><b>${escapeHtml(name)}</b><span>${escapeHtml(shorten(value, 210))}</span></div>`).join("")
      : `<span class="empty">Nenhum atributo adicional é necessário.</span>`;
    renderSource(symbol.l, symbol.e);
    $("#symbol-origin-copy").innerHTML = `A identidade <b>${escapeHtml(symbol.c)}</b> foi criada a partir de um único nó declarador da AST. Usos com esse texto ainda não apontam para ela.`;
    $("#open-ast-declaration").href = `ast.html#node=${symbol.a}`;
  }

  function inspectScope(id) {
    const scope = scopes[id];
    $("#symbol-id").textContent = `escopo #${format(id)}`;
    $("#symbol-kind").textContent = scope.k.replaceAll("_", " ");
    $("#symbol-name").textContent = scope.n;
    $("#symbol-canonical").textContent = "contexto lexical — não é uma referência";
    renderBreadcrumb(id);
    $("#symbol-facts").innerHTML = [
      ["Declarações locais", symbolsByScope[id].length], ["Subescopos", scopeChildren[id].length],
      ["Pai", scope.p < 0 ? "—" : `#${scope.p}`], ["Símbolo proprietário", scope.o < 0 ? "sintético" : `#${scope.o}`]
    ].map(([term, value]) => `<div><dt>${term}</dt><dd>${escapeHtml(value)}</dd></div>`).join("");
    $("#symbol-attributes").innerHTML = `<span class="empty">Escopos organizam símbolos; não são declarações por si só.</span>`;
    renderSource(0, 0);
    $("#symbol-origin-copy").textContent = "Este escopo foi derivado da estrutura da AST para delimitar futuras buscas de nomes.";
    $("#open-ast-declaration").href = scope.a >= 0 ? `ast.html#node=${scope.a}` : "ast.html";
  }

  function renderBreadcrumb(scopeId) {
    const path = [];
    let cursor = scopes[scopeId];
    while (cursor) { path.unshift(cursor); cursor = cursor.p >= 0 ? scopes[cursor.p] : null; }
    const concise = path.length > 7 ? [path[0], path[1], null, ...path.slice(-4)] : path;
    $("#symbol-breadcrumb").innerHTML = concise.map((scope) => scope
      ? `<button class="crumb" data-id="${scope.id}">${escapeHtml(scope.n)}</button>`
      : `<span class="crumb">…</span>`).join("");
    $$("#symbol-breadcrumb button").forEach((button) => button.addEventListener("click", () => {
      selectedScope = Number(button.dataset.id); selectedSymbol = null; renderScopes(); renderSymbols(); inspectScope(selectedScope);
    }));
  }

  function renderSource(start, end) {
    if (!start) { $("#symbol-source").innerHTML = `<span class="empty">Escopo sintético sem linha própria.</span>`; return; }
    const from = Math.max(1, start - 1);
    const to = Math.min(data.sourceLines.length, Math.max(end, start) + 1);
    const rows = [];
    for (let line = from; line <= to; line++) rows.push(`<div class="source-preview-line ${line >= start && line <= end ? "selected" : ""}"><span>${line}</span><code>${escapeHtml(data.sourceLines[line - 1] || "")}</code></div>`);
    $("#symbol-source").innerHTML = rows.join("");
  }

  function descendantScopes(root) {
    const result = [];
    const visit = (id) => { result.push(id); for (const child of scopeChildren[id]) visit(child); };
    visit(root); return result;
  }

  function expandAncestors(id) {
    let cursor = scopes[id];
    while (cursor) { expanded.add(cursor.id); cursor = cursor.p >= 0 ? scopes[cursor.p] : null; }
  }

  function depth(id) {
    let value = 0, cursor = scopes[id];
    while (cursor?.p >= 0) { value++; cursor = scopes[cursor.p]; }
    return value;
  }

  function glyph(symbol) {
    if (symbol.k === "PROGRAM") return "P";
    if (symbol.k === "PARAGRAPH" || symbol.k === "PROCEDURE_SECTION") return "¶";
    if (symbol.ns === "FILE") return "F";
    if (symbol.k === "CONDITION_NAME") return "88";
    return symbol.x?.level || "D";
  }

  function requestedSymbol() {
    const match = location.hash.match(/symbol=(\d+)/); return match ? Number(match[1]) : null;
  }

  function shorten(value, max) {
    const text = String(value ?? ""); return text.length > max ? `${text.slice(0, max - 1)}…` : text;
  }

  initialize();
})();
