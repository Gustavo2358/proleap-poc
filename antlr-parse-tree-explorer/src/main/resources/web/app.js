(() => {
  "use strict";

  const data = window.PARSE_TREE_DATA;
  if (!data) throw new Error("tree-data.js não foi carregado");

  const nodes = data.nodes;
  const children = Array.from({ length: nodes.length }, () => []);
  const roots = [];
  for (const node of nodes) {
    if (node.p < 0) roots.push(node.id);
    else children[node.p].push(node.id);
  }

  const ROW_HEIGHT = 34;
  const expanded = new Set();
  let visible = [];
  let selectedId = requestedNode() ?? roots[0] ?? 0;
  let currentView = "tree";
  let flowFilter = "all";

  const $ = (selector) => document.querySelector(selector);
  const $$ = (selector) => [...document.querySelectorAll(selector)];
  const format = (value) => Number(value).toLocaleString("pt-BR");
  const escapeHtml = (value) => String(value ?? "")
    .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;").replaceAll("'", "&#039;");

  function initialize() {
    for (const node of nodes) if (node.q && node.d < 3) expanded.add(node.id);
    const procedure = nodes.find((node) => node.n === "procedureDivision");
    if (procedure) expandAncestors(procedure.id);
    renderHeader();
    renderRuleBars();
    renderFlowFilters();
    renderFlows();
    rebuildVisible();
    if (!nodes[selectedId]) selectedId = roots[0] ?? 0;
    selectNode(selectedId, requestedNode() != null);
    bindEvents();
  }

  function renderHeader() {
    const gotoCount = data.ruleCounts.goToStatement || 0;
    const metrics = [
      [format(data.meta.nodes), "nós na parse tree"],
      [format(data.meta.tokens), "tokens consumidos"],
      [format(data.meta.maxDepth), "níveis de profundidade"],
      [format(gotoCount), "nós GO TO"]
    ];
    $("#metrics").innerHTML = metrics.map(([value, label]) =>
      `<div class="metric"><strong>${value}</strong><span>${label}</span></div>`).join("");
    const clean = data.meta.lexerErrors === 0 && data.meta.parserErrors === 0;
    const status = $("#parse-status");
    status.classList.toggle("ok", clean);
    status.innerHTML = `<span></span>${clean ? "parse sem erros" : "parse com erros"} · ${data.meta.unresolvedCopies} COPYs ausentes`;
    $("#source-file").textContent = `${data.meta.source} · pré-processado`;
    $("#generated-source").textContent = data.meta.source;
  }

  function bindEvents() {
    $("#tree-list").addEventListener("scroll", renderVirtualRows);
    $("#collapse-all").addEventListener("click", () => {
      expanded.clear();
      roots.forEach((id) => expanded.add(id));
      rebuildVisible();
    });
    $$(".tab").forEach((button) => button.addEventListener("click", () => setView(button.dataset.view)));
    $("#tree-search").addEventListener("input", (event) => renderSearch(event.target.value));
    $("#tree-search").addEventListener("keydown", (event) => {
      if (event.key === "Escape") clearSearch();
      if (event.key === "Enter") $("#search-summary .search-result")?.click();
    });
    document.addEventListener("keydown", (event) => {
      if (event.key === "/" && document.activeElement !== $("#tree-search")) {
        event.preventDefault();
        setView("tree");
        $("#tree-search").focus();
      }
    });
    document.addEventListener("click", (event) => {
      if (!event.target.closest(".search-box") && !event.target.closest("#search-summary")) {
        $("#search-summary").classList.add("hidden");
      }
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
      const value = node.x ? `<span class="tree-value">${escapeHtml(shorten(node.x, 34))}</span>` : "";
      rows.push(`<div class="tree-row ${node.id === selectedId ? "selected" : ""}" role="treeitem"
          aria-level="${node.d + 1}" aria-expanded="${hasChildren ? open : "false"}"
          data-id="${node.id}" style="top:${index * ROW_HEIGHT}px;padding-left:${8 + node.d * 17}px">
        <button class="branch-toggle ${hasChildren ? (open ? "open" : "") : "leaf"}" aria-label="${open ? "Recolher" : "Expandir"}">›</button>
        <i class="dot ${node.k === "rule" ? "rule" : node.k === "terminal" ? "token" : "error"}"></i>
        <span class="tree-name ${node.k === "rule" ? "rule-name" : "token-name"}">${escapeHtml(node.n)}</span>
        ${value}<span class="tree-location">L${node.l || "—"}</span>
      </div>`);
    }
    $("#tree-rows").innerHTML = rows.join("");
    $$("#tree-rows .tree-row").forEach((row) => {
      row.addEventListener("click", (event) => {
        const id = Number(row.dataset.id);
        if (event.target.closest(".branch-toggle")) toggleNode(id);
        else selectNode(id, false);
      });
      row.addEventListener("dblclick", () => toggleNode(Number(row.dataset.id)));
    });
  }

  function toggleNode(id) {
    if (!children[id].length) return;
    if (expanded.has(id)) expanded.delete(id); else expanded.add(id);
    rebuildVisible();
  }

  function selectNode(id, reveal = true) {
    selectedId = id;
    if (reveal) {
      setView("tree");
      expandAncestors(id);
      rebuildVisible();
      const index = visible.indexOf(id);
      if (index >= 0) $("#tree-list").scrollTop = Math.max(0, index * ROW_HEIGHT - $("#tree-list").clientHeight / 2);
    } else {
      renderVirtualRows();
    }
    renderInspector(nodes[id]);
    renderSource(nodes[id]);
    history.replaceState(null, "", `#node=${id}`);
  }

  function expandAncestors(id) {
    let cursor = nodes[id];
    while (cursor && cursor.p >= 0) {
      expanded.add(cursor.p);
      cursor = nodes[cursor.p];
    }
  }

  function renderInspector(node) {
    $("#node-id").textContent = `nó #${format(node.id)}`;
    $("#node-kind").textContent = node.k === "rule" ? "REGRA DA GRAMÁTICA" : node.k === "terminal" ? "TOKEN TERMINAL" : "NÓ DE ERRO";
    $("#node-name").textContent = node.n;
    $("#node-runtime").textContent = node.k === "rule" ? "ParserRuleContext" : node.k === "terminal" ? "TerminalNodeImpl" : "ErrorNodeImpl";
    const span = node.l === node.e || !node.e ? `linha ${format(node.l)}` : `linhas ${format(node.l)}–${format(node.e)}`;
    $("#node-facts").innerHTML = [
      ["Profundidade", node.d],
      ["Filhos diretos", node.q],
      ["Origem", span],
      ["Coluna", node.c],
      ["Token inicial", node.a],
      ["Token final", node.b]
    ].map(([term, value]) => `<div><dt>${term}</dt><dd>${typeof value === "number" ? format(value) : escapeHtml(value)}</dd></div>`).join("");

    const path = [];
    let cursor = node;
    while (cursor) {
      path.unshift(cursor);
      cursor = cursor.p >= 0 ? nodes[cursor.p] : null;
    }
    const concisePath = path.length > 6 ? [path[0], null, ...path.slice(-4)] : path;
    $("#breadcrumb").innerHTML = concisePath.map((item) => item
      ? `<button class="crumb" data-id="${item.id}">${escapeHtml(item.n)}</button>`
      : `<span class="crumb">…</span>`).join("");
    $$("#breadcrumb button").forEach((button) => button.addEventListener("click", () => selectNode(Number(button.dataset.id))));

    const immediate = children[node.id];
    $("#child-list").innerHTML = immediate.length
      ? immediate.slice(0, 18).map((id) => {
          const child = nodes[id];
          return `<button class="child-chip" data-id="${id}">${escapeHtml(child.x || child.n)}</button>`;
        }).join("") + (immediate.length > 18 ? `<span class="empty">+${immediate.length - 18} filhos</span>` : "")
      : `<span class="empty">Folha: não possui filhos.</span>`;
    $$("#child-list button").forEach((button) => button.addEventListener("click", () => selectNode(Number(button.dataset.id))));
    $("#node-insight").textContent = insightFor(node);
    renderAstLink(node);
  }

  function renderAstLink(node) {
    const ast = window.AST_DATA;
    const action = $("#open-ast-node");
    const copy = $("#ast-link-copy");
    if (!ast) { action.href = "ast.html"; return; }
    let cursor = node;
    let matches = null;
    while (cursor) {
      matches = ast.parseToAst[String(cursor.id)];
      if (matches?.length) break;
      cursor = cursor.p >= 0 ? nodes[cursor.p] : null;
    }
    if (matches?.length) {
      const astNode = ast.nodes[matches[0]];
      action.href = `ast.html#node=${astNode.id}`;
      action.textContent = `Abrir ${astNode.t} na AST →`;
      copy.textContent = cursor.id === node.id
        ? `Este nó origina ${matches.length === 1 ? "um nó" : `${matches.length} nós`} da AST.`
        : `A região semântica mais próxima é ${astNode.t}, originada por ${cursor.n}.`;
    } else {
      action.href = "ast.html";
      action.textContent = "Abrir a AST →";
      copy.textContent = "Este token isolado foi absorvido pela estrutura semântica ao redor.";
    }
  }

  function insightFor(node) {
    if (node.k === "terminal") return `É uma folha consumida pelo parser. “${shorten(node.x, 48)}” é texto do token; acima dela estão as regras que deram significado sintático a esse texto.`;
    if (node.k === "error") return "Este nó foi inserido durante a estratégia de recuperação de erro do ANTLR. Ele sinaliza que a árvore existe, mas aquela região não foi reconhecida de forma limpa.";
    if (node.n === "startRule") return "É a raiz retornada pela chamada ao parser. Todo o programa, inclusive EOF, fica alcançável a partir deste contexto.";
    if (node.n === "dataDescriptionEntryFormat1") return "O ANTLR preserva level, nome e clauses como filhos sintáticos. A hierarquia 01/05/10 de dados ainda precisa ser reconstruída por um AstBuilder.";
    if (node.n === "goToStatement" || node.n.startsWith("goTo")) return "A árvore reconhece a forma do GO TO e seu nome de destino. Ela não cria uma aresta para o parágrafo-alvo; essa resolução pertence ao CFG ou à análise semântica.";
    if (node.n === "performStatement" || node.n.startsWith("perform")) return "Os filhos distinguem PERFORM inline de chamada de procedure e expõem THRU quando presente. Isso é matéria-prima para construir fluxo, não o fluxo pronto.";
    if (node.n === "evaluateStatement" || node.n.startsWith("evaluate")) return "Os ramos WHEN/OTHER aparecem como subárvores. Um visitor pode convertê-los em branches de uma AST ou CFG mais enxutos.";
    if (node.n === "ifStatement" || node.n === "ifThen" || node.n === "ifElse") return "A estrutura separa condição, THEN e ELSE. Pontuação e palavras-chave continuam presentes porque esta é uma parse tree concreta.";
    if (node.n.endsWith("Division") || node.n.endsWith("Section") || node.n === "paragraph") return "Este nó funciona como contêiner sintático COBOL. Seus descendentes preservam a ordem exata em que o parser reconheceu regras e tokens.";
    return "Cada regra invocada com sucesso vira um contexto. Regras intermediárias podem parecer verbosas, mas guardam alternativas, ordem, posição e limites de tokens úteis para visitors.";
  }

  function renderSource(node) {
    const line = Math.max(1, node.l || 1);
    const stop = Math.max(line, node.e || line);
    const radius = 10;
    const wideSpan = stop - line > radius * 2;
    const start = Math.max(1, line - radius);
    const end = Math.min(data.sourceLines.length, wideSpan ? line + radius : Math.max(stop + 4, line + radius));
    const output = [];
    for (let number = start; number <= end; number++) {
      const selected = wideSpan ? number === line : number >= line && number <= stop;
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
    if (!query) {
      panel.classList.add("hidden");
      panel.innerHTML = "";
      return;
    }
    const matches = [];
    for (const node of nodes) {
      if (node.n.toLowerCase().includes(query) || (node.x && node.x.toLowerCase().includes(query))) matches.push(node.id);
      if (matches.length >= 250) break;
    }
    panel.innerHTML = `<div class="search-count">${matches.length === 250 ? "250+" : format(matches.length)} resultados · mostrando ${Math.min(30, matches.length)}</div>` +
      matches.slice(0, 30).map((id) => {
        const node = nodes[id];
        return `<button class="search-result" data-id="${id}"><i class="dot ${node.k === "rule" ? "rule" : "token"}"></i><b>${escapeHtml(node.n)}</b>${node.x ? escapeHtml(shorten(node.x, 24)) : ""}<span>L${node.l}</span></button>`;
      }).join("");
    panel.classList.remove("hidden");
    $$("#search-summary .search-result").forEach((button) => button.addEventListener("click", () => {
      selectNode(Number(button.dataset.id));
      clearSearch();
    }));
  }

  function clearSearch() {
    $("#tree-search").value = "";
    $("#search-summary").classList.add("hidden");
  }

  function renderRuleBars() {
    const entries = Object.entries(data.ruleCounts).sort((a, b) => b[1] - a[1]).slice(0, 36);
    const max = entries[0]?.[1] || 1;
    $("#rule-bars").innerHTML = entries.map(([name, count]) =>
      `<div class="rule-bar" data-rule="${escapeHtml(name)}"><span class="rule-label">${escapeHtml(name)}</span><span class="bar-track"><span class="bar-fill" style="width:${count / max * 100}%"></span></span><span class="rule-count">${format(count)}</span></div>`
    ).join("");
    $$(".rule-bar").forEach((bar) => bar.addEventListener("click", () => {
      const found = nodes.find((node) => node.n === bar.dataset.rule);
      if (found) selectNode(found.id);
    }));
  }

  const flowTypes = ["goToStatement", "performStatement", "evaluateStatement", "ifStatement"];

  function renderFlowFilters() {
    const labels = { all: "Todos", goToStatement: "GO TO", performStatement: "PERFORM", evaluateStatement: "EVALUATE", ifStatement: "IF" };
    const options = ["all", ...flowTypes];
    $("#flow-filters").innerHTML = options.map((type) => {
      const count = type === "all" ? flowTypes.reduce((sum, key) => sum + (data.ruleCounts[key] || 0), 0) : data.ruleCounts[type] || 0;
      return `<button class="flow-filter ${type === flowFilter ? "active" : ""}" data-type="${type}">${labels[type]} · ${format(count)}</button>`;
    }).join("");
    $$(".flow-filter").forEach((button) => button.addEventListener("click", () => {
      flowFilter = button.dataset.type;
      renderFlowFilters();
      renderFlows();
    }));
  }

  function renderFlows() {
    const accepted = flowFilter === "all" ? new Set(flowTypes) : new Set([flowFilter]);
    const matches = nodes.filter((node) => accepted.has(node.n));
    const labels = { goToStatement: "GO TO", performStatement: "PERFORM", evaluateStatement: "EVALUATE", ifStatement: "IF" };
    $("#flow-list").innerHTML = matches.map((node) =>
      `<div class="flow-card" data-id="${node.id}"><span class="flow-type">${labels[node.n]}</span><span class="flow-snippet">${escapeHtml(subtreeText(node.id))}</span><span class="flow-line">L${node.l}</span></div>`
    ).join("");
    $$(".flow-card").forEach((card) => card.addEventListener("click", () => selectNode(Number(card.dataset.id))));
  }

  function subtreeText(id) {
    const root = nodes[id];
    const parts = [];
    for (let index = id + 1; index < nodes.length && nodes[index].d > root.d && parts.length < 28; index++) {
      const node = nodes[index];
      if (node.k === "terminal" && node.n !== "EOF" && node.x) parts.push(node.x);
    }
    return shorten(parts.join(" ").replace(/\s+([.,;)])/g, "$1").replace(/([(])\s+/g, "$1"), 130);
  }

  function shorten(value, length) {
    const clean = String(value ?? "").replace(/\s+/g, " ").trim();
    return clean.length > length ? `${clean.slice(0, length - 1)}…` : clean;
  }

  function requestedNode() {
    const match = location.hash.match(/node=(\d+)/);
    return match ? Number(match[1]) : null;
  }

  initialize();
})();
