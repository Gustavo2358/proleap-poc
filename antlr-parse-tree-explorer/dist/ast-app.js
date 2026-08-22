(() => {
  "use strict";

  const data = window.AST_DATA;
  if (!data) throw new Error("ast-data.js não foi carregado");

  const nodes = data.nodes;
  const children = Array.from({ length: nodes.length }, () => []);
  const roots = [];
  for (const node of nodes) {
    if (node.p < 0) roots.push(node.id); else children[node.p].push(node.id);
  }

  const ROW_HEIGHT = 34;
  const expanded = new Set();
  let visible = [];
  let selectedId = requestedNode() ?? roots[0] ?? 0;
  let currentView = "tree";

  const $ = (selector) => document.querySelector(selector);
  const $$ = (selector) => [...document.querySelectorAll(selector)];
  const format = (value) => Number(value).toLocaleString("pt-BR");
  const escapeHtml = (value) => String(value ?? "")
    .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;").replaceAll("'", "&#039;");

  function initialize() {
    for (const node of nodes) if (node.q && node.d < 2) expanded.add(node.id);
    renderHeader();
    renderTypeBars();
    renderCalls();
    rebuildVisible();
    selectNode(nodes[selectedId] ? selectedId : 0, requestedNode() != null);
    bindEvents();
  }

  function renderHeader() {
    const reduction = Math.round((1 - data.meta.nodes / data.meta.parseTreeNodes) * 100);
    const metrics = [
      [format(data.meta.nodes), "nós semânticos"],
      [`${reduction}%`, "menos nós"],
      [format(data.meta.maxDepth), "níveis de profundidade"],
      [format(data.meta.staticCalls), "CALL estático"]
    ];
    $("#ast-metrics").innerHTML = metrics.map(([value, label]) =>
      `<div class="metric"><strong>${value}</strong><span>${label}</span></div>`).join("");
    $("#model-parse-nodes").textContent = `${format(data.meta.parseTreeNodes)} nós`;
    $("#model-ast-nodes").textContent = `${format(data.meta.nodes)} nós`;
    $("#ast-status").innerHTML = `<span></span>AST construída · ${format(data.meta.nodes)} nós · ${data.meta.dynamicCalls} CALLs dinâmicos`;
    $("#source-file").textContent = `${data.meta.source} · pré-processado`;
  }

  function bindEvents() {
    $("#tree-list").addEventListener("scroll", renderVirtualRows);
    $("#collapse-all").addEventListener("click", () => {
      expanded.clear(); roots.forEach((id) => expanded.add(id)); rebuildVisible();
    });
    $$(".tab").forEach((button) => button.addEventListener("click", () => setView(button.dataset.view)));
    $("#tree-search").addEventListener("input", (event) => renderSearch(event.target.value));
    $("#tree-search").addEventListener("keydown", (event) => {
      if (event.key === "Escape") clearSearch();
      if (event.key === "Enter") $("#search-summary .search-result")?.click();
    });
    document.addEventListener("keydown", (event) => {
      if (event.key === "/" && document.activeElement !== $("#tree-search")) {
        event.preventDefault(); setView("tree"); $("#tree-search").focus();
      }
    });
    document.addEventListener("click", (event) => {
      if (!event.target.closest(".search-box") && !event.target.closest("#search-summary"))
        $("#search-summary").classList.add("hidden");
    });
    window.addEventListener("resize", renderVirtualRows);
  }

  function setView(view) {
    currentView = view;
    $$(".tab").forEach((button) => button.classList.toggle("active", button.dataset.view === view));
    $$(".view").forEach((panel) => panel.classList.toggle("active", panel.id === `${view}-view`));
    $("#tree-toolbar").style.display = view === "tree" ? "flex" : "none";
    if (view === "tree") requestAnimationFrame(renderVirtualRows);
  }

  function rebuildVisible() {
    visible = [];
    const visit = (id) => {
      visible.push(id);
      if (expanded.has(id)) for (const child of children[id]) visit(child);
    };
    roots.forEach(visit);
    $("#tree-spacer").style.height = `${visible.length * ROW_HEIGHT}px`;
    $("#visible-count").textContent = `${format(visible.length)} de ${format(nodes.length)} visíveis`;
    renderVirtualRows();
  }

  function renderVirtualRows() {
    if (currentView !== "tree") return;
    const viewport = $("#tree-list");
    const start = Math.max(0, Math.floor(viewport.scrollTop / ROW_HEIGHT) - 5);
    const count = Math.ceil(viewport.clientHeight / ROW_HEIGHT) + 10;
    const end = Math.min(visible.length, start + count);
    const rows = [];
    for (let index = start; index < end; index++) {
      const node = nodes[visible[index]];
      const hasChildren = node.q > 0;
      const open = expanded.has(node.id);
      rows.push(`<div class="tree-row ${node.id === selectedId ? "selected" : ""}" role="treeitem"
          aria-level="${node.d + 1}" aria-expanded="${hasChildren ? open : "false"}"
          data-id="${node.id}" style="top:${index * ROW_HEIGHT}px;padding-left:${8 + node.d * 17}px">
        <button class="branch-toggle ${hasChildren ? (open ? "open" : "") : "leaf"}" aria-label="${open ? "Recolher" : "Expandir"}">›</button>
        <i class="dot ${dotClass(node)}"></i>
        <span class="tree-name rule-name">${escapeHtml(node.t)}</span>
        <span class="ast-row-type">${escapeHtml(shorten(node.n, 38))}</span>
        <span class="tree-location">L${node.l || "—"}</span>
      </div>`);
    }
    $("#tree-rows").innerHTML = rows.join("");
    $$("#tree-rows .tree-row").forEach((row) => {
      row.addEventListener("click", (event) => {
        const id = Number(row.dataset.id);
        if (event.target.closest(".branch-toggle")) toggleNode(id); else selectNode(id, false);
      });
      row.addEventListener("dblclick", () => toggleNode(Number(row.dataset.id)));
    });
  }

  function dotClass(node) {
    if (node.t === "EmbeddedLanguageStatement") return "semantic";
    return ["program", "structure", "statement", "expression", "declaration"].includes(node.k) ? node.k : "semantic";
  }

  function toggleNode(id) {
    if (!children[id].length) return;
    if (expanded.has(id)) expanded.delete(id); else expanded.add(id);
    rebuildVisible();
  }

  function selectNode(id, reveal = true) {
    selectedId = id;
    if (reveal) {
      setView("tree"); expandAncestors(id); rebuildVisible();
      const index = visible.indexOf(id);
      if (index >= 0) $("#tree-list").scrollTop = Math.max(0, index * ROW_HEIGHT - $("#tree-list").clientHeight / 2);
    } else renderVirtualRows();
    history.replaceState(null, "", `#node=${id}`);
    renderInspector(nodes[id]);
    renderSource(nodes[id]);
  }

  function expandAncestors(id) {
    let cursor = nodes[id];
    while (cursor && cursor.p >= 0) { expanded.add(cursor.p); cursor = nodes[cursor.p]; }
  }

  function renderInspector(node) {
    $("#node-id").textContent = `nó #${format(node.id)}`;
    $("#node-kind").textContent = categoryLabel(node.k);
    $("#node-name").textContent = node.t;
    $("#node-label").textContent = node.n;
    const sourceSpan = node.l === node.e || !node.e ? `linha ${format(node.l)}` : `linhas ${format(node.l)}–${format(node.e)}`;
    $("#node-facts").innerHTML = [
      ["Profundidade", node.d], ["Filhos semânticos", node.q], ["Origem", sourceSpan],
      ["Regra de origem", node.g], ["Parse tree root", node.r < 0 ? "sintético" : `#${format(node.r)}`],
      ["Nós na região", node.z]
    ].map(([term, value]) => `<div><dt>${term}</dt><dd>${typeof value === "number" ? format(value) : escapeHtml(value)}</dd></div>`).join("");

    const path = [];
    let cursor = node;
    while (cursor) { path.unshift(cursor); cursor = cursor.p >= 0 ? nodes[cursor.p] : null; }
    const concise = path.length > 6 ? [path[0], null, ...path.slice(-4)] : path;
    $("#breadcrumb").innerHTML = concise.map((item) => item
      ? `<button class="crumb" data-id="${item.id}">${escapeHtml(item.t)}</button>`
      : `<span class="crumb">…</span>`).join("");
    $$("#breadcrumb button").forEach((button) => button.addEventListener("click", () => selectNode(Number(button.dataset.id))));

    const attributes = Object.entries(node.a || {});
    $("#attribute-list").innerHTML = attributes.length
      ? attributes.map(([name, value]) => `<div class="attribute-row"><b>${escapeHtml(name)}</b><span>${escapeHtml(shorten(value, 240))}</span></div>`).join("")
      : `<span class="empty">Este nó não precisa de atributos adicionais.</span>`;

    const immediate = children[node.id];
    $("#child-list").innerHTML = immediate.length
      ? immediate.slice(0, 18).map((id) => `<button class="child-chip" data-id="${id}">${escapeHtml(nodes[id].t)} · ${escapeHtml(shorten(nodes[id].n, 24))}</button>`).join("") +
        (immediate.length > 18 ? `<span class="empty">+${immediate.length - 18} filhos</span>` : "")
      : `<span class="empty">Nó sem filhos.</span>`;
    $$("#child-list button").forEach((button) => button.addEventListener("click", () => selectNode(Number(button.dataset.id))));

    const grammar = node.g ? `<b>${escapeHtml(node.g)}</b>` : "uma região sintática";
    $("#compression-copy").innerHTML = node.r >= 0
      ? `A região enraizada em ${grammar} contém <span class="compression-number">${format(node.z)}</span> nós da parse tree. Aqui ela aparece como um conceito tipado com ${format(node.q)} filhos semânticos.`
      : "Este é um nó sintético criado para tornar explícita uma estrutura que não existia como nó único na parse tree.";
    const originLink = $("#open-parse-node");
    originLink.href = node.r >= 0 ? `index.html#node=${node.r}` : "index.html";
    originLink.textContent = node.r >= 0 ? "Ver origem na parse tree ←" : "Abrir parse tree ←";
    $("#node-insight").textContent = insightFor(node);
  }

  function categoryLabel(category) {
    return ({program:"PROGRAMA", structure:"ESTRUTURA", statement:"STATEMENT",
      expression:"EXPRESSION", declaration:"DECLARAÇÃO", semantic:"NÓ SEMÂNTICO"})[category] || category.toUpperCase();
  }

  function insightFor(node) {
    if (node.t === "Program") return "É a raiz do modelo da aplicação. Ela já ignora EOF e regras de compilação que não acrescentam significado ao domínio.";
    if (node.t === "Division") return "A divisão COBOL permanece explícita porque organiza declarações e lógica com papéis semânticos diferentes.";
    if (node.t === "Sentence") return "O ponto não virou um nó. Seu efeito foi promovido ao atributo terminator=PERIOD, preservando a fronteira necessária para NEXT SENTENCE e CFG.";
    if (node.t === "CallStatement") return node.a.targetKind === "STATIC_LITERAL"
      ? "O alvo literal já é um fato direto sobre dependência de subprograma. Nenhuma tabela de símbolos ou análise de fluxo é necessária para descobri-lo."
      : "O alvo é uma expression dinâmica. A AST o preserva; reaching definitions e resolução de constantes serão responsáveis pelos valores possíveis.";
    if (node.t === "IfStatement" || node.t === "EvaluateStatement") return "Os branches são filhos semânticos. No CFG eles se tornarão caminhos separados, cujo estado poderá ser unido no ponto de merge.";
    if (node.t === "GoToStatement") return "A AST guarda os nomes dos destinos, mas ainda não resolve os parágrafos nem cria arestas. Isso pertence à resolução de nomes e ao CFG.";
    if (node.t === "PerformStatement") return "A AST distingue PERFORM inline de procedure e preserva THRU e controle, evitando que o CFG precise reinterpretar tokens.";
    if (node.t === "MoveStatement") return "Source e targets já estão separados. Esta forma será convertida em definições e usos pela IR de análise.";
    if (node.t === "EmbeddedLanguageStatement") return "O payload e sua origem foram preservados sem interpretar a DSL. Um plugin SQL/CICS poderá preencher uma AST embutida no próximo MVP sem alterar o núcleo COBOL.";
    if (node.t === "UnsupportedStatement") return "O statement ainda não recebeu um modelo próprio, mas não foi perdido: regra, texto, posição e statements aninhados continuam disponíveis.";
    if (node.k === "expression") return "Expressions são valores analisáveis. Literais já são tipados como alvos estáticos; referências permanecem pelo nome escrito até a resolução de símbolos.";
    return "Este nó existe porque representa um conceito útil para as próximas etapas, e não apenas porque a gramática precisou de uma regra intermediária.";
  }

  function renderSource(node) {
    const line = Math.max(1, node.l || 1);
    const stop = Math.max(line, node.e || line);
    const radius = 10;
    const wide = stop - line > radius * 2;
    const start = Math.max(1, line - radius);
    const end = Math.min(data.sourceLines.length, wide ? line + radius : Math.max(stop + 4, line + radius));
    const output = [];
    for (let number = start; number <= end; number++) {
      const selected = wide ? number === line : number >= line && number <= stop;
      output.push(`<div class="source-line ${selected ? "selected" : ""}"><span class="ln">${number}</span><span>${highlightCobol(data.sourceLines[number - 1] || "")}</span></div>`);
    }
    $("#source-code").innerHTML = output.join("");
    $("#source-position").textContent = `L${format(line)}${stop !== line ? `–${format(stop)}` : ""}`;
  }

  function highlightCobol(line) {
    const safe = escapeHtml(line);
    if (line.trimStart().startsWith("*>")) return `<span class="comment">${safe}</span>`;
    return safe.replace(/\b(DIVISION|SECTION|PROCEDURE|WORKING-STORAGE|IF|ELSE|END-IF|EVALUATE|WHEN|END-EVALUATE|PERFORM|THRU|GO|TO|MOVE|CALL|EXEC|END-EXEC|PIC|VALUE|REDEFINES|OCCURS)\b/gi,
      '<span class="keyword">$1</span>');
  }

  function renderSearch(rawQuery) {
    const query = rawQuery.trim().toLowerCase();
    const panel = $("#search-summary");
    if (!query) { panel.classList.add("hidden"); panel.innerHTML = ""; return; }
    const matches = [];
    for (const node of nodes) {
      const attributes = Object.values(node.a || {}).join(" ").toLowerCase();
      if (node.t.toLowerCase().includes(query) || node.n.toLowerCase().includes(query) || attributes.includes(query)) matches.push(node.id);
      if (matches.length >= 250) break;
    }
    panel.innerHTML = `<div class="search-count">${matches.length === 250 ? "250+" : format(matches.length)} resultados · mostrando ${Math.min(30, matches.length)}</div>` +
      matches.slice(0, 30).map((id) => {
        const node = nodes[id];
        return `<button class="search-result" data-id="${id}"><i class="dot ${dotClass(node)}"></i><b>${escapeHtml(node.t)}</b>${escapeHtml(shorten(node.n, 25))}<span>L${node.l}</span></button>`;
      }).join("");
    panel.classList.remove("hidden");
    $$("#search-summary .search-result").forEach((button) => button.addEventListener("click", () => {
      selectNode(Number(button.dataset.id)); clearSearch();
    }));
  }

  function clearSearch() { $("#tree-search").value = ""; $("#search-summary").classList.add("hidden"); }

  function renderTypeBars() {
    const entries = Object.entries(data.typeCounts).sort((a, b) => b[1] - a[1]);
    const max = entries[0]?.[1] || 1;
    $("#type-bars").innerHTML = entries.map(([name, count]) =>
      `<div class="rule-bar" data-type="${escapeHtml(name)}"><span class="rule-label">${escapeHtml(name)}</span><span class="bar-track"><span class="bar-fill" style="width:${count / max * 100}%"></span></span><span class="rule-count">${format(count)}</span></div>`
    ).join("");
    $$("#type-bars .rule-bar").forEach((bar) => bar.addEventListener("click", () => {
      const found = nodes.find((node) => node.t === bar.dataset.type); if (found) selectNode(found.id);
    }));
  }

  function renderCalls() {
    const calls = nodes.filter((node) => node.t === "CallStatement");
    $("#call-summary").innerHTML = `<span><b>${format(data.meta.staticCalls)}</b>estático</span><span><b>${format(data.meta.dynamicCalls)}</b>dinâmico</span>`;
    $("#call-list").innerHTML = calls.length ? calls.map((node) =>
      `<div class="flow-card" data-id="${node.id}"><span class="flow-type">${node.a.targetKind === "STATIC_LITERAL" ? "LITERAL" : "DINÂMICO"}</span><span class="flow-snippet">CALL ${escapeHtml(node.n)}</span><span class="flow-line">L${node.l}</span></div>`
    ).join("") : `<span class="empty">Nenhum CALL encontrado.</span>`;
    $$("#call-list .flow-card").forEach((card) => card.addEventListener("click", () => selectNode(Number(card.dataset.id))));
  }

  function requestedNode() {
    const match = location.hash.match(/node=(\d+)/);
    return match ? Number(match[1]) : null;
  }

  function shorten(value, length) {
    const clean = String(value ?? "").replace(/\s+/g, " ").trim();
    return clean.length > length ? `${clean.slice(0, length - 1)}…` : clean;
  }

  initialize();
})();
