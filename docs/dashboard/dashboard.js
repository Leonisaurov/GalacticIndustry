(function () {
  "use strict";

  var STATUS = {
    PENDIENTE: { label: "Pendiente", cls: "s-PENDIENTE" },
    LISTO: { label: "Listo", cls: "s-LISTO" },
    EN_PROGRESO: { label: "En progreso", cls: "s-EN_PROGRESO" },
    COMPLETADO: { label: "Completado", cls: "s-COMPLETADO" },
    BLOQUEADO: { label: "Bloqueado", cls: "s-BLOQUEADO" },
    POSPUESTO: { label: "Pospuesto", cls: "s-POSPUESTO" },
    REQUIERE_DECISION: { label: "Requiere decisión", cls: "s-REQUIERE_DECISION" }
  };

  var STATUS_ORDER = ["COMPLETADO", "EN_PROGRESO", "LISTO", "PENDIENTE", "BLOQUEADO", "REQUIERE_DECISION", "POSPUESTO"];
  var STATUS_SLUG = {
    PENDIENTE: "pendiente",
    LISTO: "listo",
    EN_PROGRESO: "progreso",
    COMPLETADO: "completado",
    BLOQUEADO: "bloqueado",
    POSPUESTO: "pospuesto",
    REQUIERE_DECISION: "decision"
  };

  var PRIORITY_LABEL = { P0: "P0 · crítico", P1: "P1 · alto", P2: "P2 · medio", P3: "P3 · bajo" };
  var PRIORITY_ORDER = { P0: 0, P1: 1, P2: 2, P3: 3 };

  var DECISION_LABEL = {
    PENDIENTE: "Pendiente",
    APROBADA: "Aprobada",
    RECHAZADA: "Rechazada",
    APLAZADA: "Aplazada",
    TECNICA_RESUELTA: "Técnica resuelta"
  };
  var DECISION_CLASS = {
    PENDIENTE: "s-REQUIERE_DECISION",
    APROBADA: "s-COMPLETADO",
    RECHAZADA: "s-BLOQUEADO",
    APLAZADA: "s-POSPUESTO",
    TECNICA_RESUELTA: "s-LISTO"
  };
  var DECISION_ORDER = { PENDIENTE: 0, APLAZADA: 1, RECHAZADA: 2, TECNICA_RESUELTA: 3, APROBADA: 4 };

  var DATA = null;
  try {
    DATA = parseEmbedded();
  } catch (e) {
    document.getElementById("project-description").textContent = "No se pudo leer el bloque JSON integrado: " + e.message;
    return;
  }

  var meta;
  var stages;
  var tasks;
  var decisions;
  var sessions;
  var issues;
  var taskById;
  var stageById;
  var decisionById;
  var statusDefs;

  var state = {
    search: "",
    status: "ALL",
    stage: "ALL",
    priority: "ALL",
    sort: "stage",
    groupByStage: true,
    historyAll: false,
    hideResolved: false,
    open: {}
  };

  function parseEmbedded() {
    return JSON.parse(document.getElementById("tracker-data").textContent);
  }

  function applyTrackerData(next) {
    DATA = next || {};
    meta = DATA.meta || {};
    stages = DATA.stages || [];
    tasks = DATA.tasks || [];
    decisions = DATA.decisions || [];
    sessions = DATA.sessions || [];
    issues = DATA.issues || [];
    statusDefs = DATA.statusDefinitions || {};
    taskById = {};
    stageById = {};
    decisionById = {};
    tasks.forEach(function (t) { taskById[t.id] = t; });
    stages.forEach(function (s) { stageById[s.id] = s; });
    decisions.forEach(function (d) { decisionById[d.id] = d; });
  }

  applyTrackerData(DATA);

  function pendingDecisionIds(task) {
    return (task.decisionIds || []).filter(function (id) {
      var d = decisionById[id];
      return d && d.status === "PENDIENTE";
    });
  }

  function esc(value) {
    if (value === null || value === undefined) return "";
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function statusInfo(status) {
    return STATUS[status] || { label: status || "—", cls: "" };
  }

  function statusBadge(status) {
    var s = statusInfo(status);
    var def = statusDefs[status];
    var title = def && def.description ? ' title="' + esc(def.description) + '"' : "";
    return '<span class="badge ' + s.cls + '"' + title + ">" + esc(s.label) + "</span>";
  }

  function priorityChip(priority) {
    if (!priority) return '<span class="chip">—</span>';
    return '<span class="chip p-' + esc(priority) + '">' + esc(PRIORITY_LABEL[priority] || priority) + "</span>";
  }

  function decisionBadge(status) {
    var label = DECISION_LABEL[status] || status || "—";
    var cls = DECISION_CLASS[status] || "s-LISTO";
    return '<span class="badge ' + cls + '">' + esc(label) + "</span>";
  }

  function outcomeBadge(value) {
    var s = String(value || "").toUpperCase();
    var cls = s.indexOf("COMPLET") === 0 ? "s-COMPLETADO"
      : s.indexOf("BLOQUE") === 0 ? "s-BLOQUEADO"
      : s.indexOf("PROGRES") === 0 ? "s-EN_PROGRESO"
      : "s-LISTO";
    return '<span class="badge ' + cls + '">' + esc(value || "—") + "</span>";
  }

  function issueStatusBadge(status) {
    var s = String(status || "").toUpperCase();
    var cls = s === "ABIERTO" ? "s-BLOQUEADO" : s === "CONOCIDO" ? "s-LISTO" : "s-COMPLETADO";
    return '<span class="badge ' + cls + '">' + esc(status || "—") + "</span>";
  }

  function issueSeverityChip(severity) {
    var s = String(severity || "").toUpperCase();
    var cls = s === "ALTA" ? "p-P0" : s === "BAJA" ? "p-P2" : "p-P1";
    return '<span class="chip ' + cls + '">Severidad ' + esc(severity || "—") + "</span>";
  }

  function fmtDate(value) {
    if (!value) return "—";
    var d = new Date(value);
    if (isNaN(d.getTime())) return String(value);
    return d.toLocaleString("es-ES", {
      year: "numeric", month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit"
    });
  }

  function fmtDateShort(value) {
    if (!value) return "";
    var d = new Date(value);
    if (isNaN(d.getTime())) return String(value);
    return d.toLocaleDateString("es-ES", { year: "numeric", month: "2-digit", day: "2-digit" });
  }

  function natCompare(a, b) {
    return String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: "base" });
  }

  function stageOrderMap() {
    var map = {};
    stages.forEach(function (s) { map[s.id] = s.order || 0; });
    return map;
  }

  function taskProgress() {
    var impl = tasks.filter(function (t) {
      return t.category === "implementation" && t.countsForProgress === true;
    });
    var done = impl.filter(function (t) { return t.status === "COMPLETADO"; });
    return {
      done: done.length,
      total: impl.length,
      pct: impl.length ? Math.round(done.length * 100 / impl.length) : 0
    };
  }

  function prepProgress() {
    var prep = tasks.filter(function (t) { return t.category === "preparation"; });
    var done = prep.filter(function (t) { return t.status === "COMPLETADO"; });
    return {
      done: done.length,
      total: prep.length,
      pct: prep.length ? Math.round(done.length * 100 / prep.length) : 0
    };
  }

  function stageProgress(stage) {
    var ts = tasks.filter(function (t) { return t.stageId === stage.id; });
    var impl = ts.filter(function (t) {
      return t.category === "implementation" && t.countsForProgress === true;
    });
    var prep = ts.filter(function (t) { return t.category === "preparation"; });
    var done = impl.filter(function (t) { return t.status === "COMPLETADO"; });
    var prepDone = prep.filter(function (t) { return t.status === "COMPLETADO"; });
    var status = stage.status;
    if (impl.length > 0) {
      if (done.length === impl.length) status = "COMPLETADO";
      else if (impl.some(function (t) { return t.status === "EN_PROGRESO"; })) status = "EN_PROGRESO";
      else if (impl.some(function (t) { return t.status === "REQUIERE_DECISION"; })) status = "REQUIERE_DECISION";
      else if (impl.some(function (t) { return t.status === "BLOQUEADO"; })) status = "BLOQUEADO";
      else if (impl.some(function (t) { return t.status === "LISTO"; })) status = "LISTO";
      else status = "PENDIENTE";
    }
    return {
      total: ts.length,
      implTotal: impl.length,
      done: done.length,
      prepTotal: prep.length,
      prepDone: prepDone.length,
      pct: impl.length ? Math.round(done.length * 100 / impl.length) : (status === "COMPLETADO" ? 100 : 0),
      status: status
    };
  }

  function countsByStatus() {
    var c = {};
    tasks.forEach(function (t) { c[t.status] = (c[t.status] || 0) + 1; });
    return c;
  }

  function renderSummary() {
    var p = taskProgress();
    var prep = prepProgress();
    var c = countsByStatus();
    var stageDone = stages.filter(function (s) { return stageProgress(s).status === "COMPLETADO"; }).length;
    var pending = (c.PENDIENTE || 0) + (c.LISTO || 0);
    var openDecisions = decisions.filter(function (d) { return d.status === "PENDIENTE"; }).length;

    var cards = [
      { label: "Progreso de implementación", value: p.pct + "%", sub: p.done + " de " + p.total + " tareas reales", cls: "accent", pct: p.pct },
      { label: "Preparación", value: prep.pct + "%", sub: prep.done + " de " + prep.total + " tareas", cls: "good", pct: prep.pct },
      { label: "Etapas completadas", value: stageDone + " / " + stages.length, sub: "del plan maestro", cls: stageDone === stages.length ? "good" : "", pct: stages.length ? Math.round(stageDone * 100 / stages.length) : 0 },
      { label: "Tareas completadas", value: String(c.COMPLETADO || 0), sub: "de " + tasks.length + " registradas", cls: "good", pct: tasks.length ? Math.round((c.COMPLETADO || 0) * 100 / tasks.length) : 0 },
      { label: "En progreso", value: String(c.EN_PROGRESO || 0), sub: c.EN_PROGRESO ? "recuperar antes de otra tarea" : "ninguna tarea activa", cls: c.EN_PROGRESO ? "warn" : "", pct: tasks.length ? Math.round((c.EN_PROGRESO || 0) * 100 / tasks.length) : 0 },
      { label: "Pendientes", value: String(pending), sub: "pendientes + listas", cls: "warn", pct: tasks.length ? Math.round(pending * 100 / tasks.length) : 0 },
      { label: "Bloqueadas", value: String(c.BLOQUEADO || 0), sub: "requieren intervención", cls: (c.BLOQUEADO || 0) > 0 ? "bad" : "good", pct: tasks.length ? Math.round((c.BLOQUEADO || 0) * 100 / tasks.length) : 0 },
      { label: "Decisiones abiertas", value: String(openDecisions), sub: "de " + decisions.length + " registradas", cls: openDecisions > 0 ? "decision" : "good", pct: decisions.length ? Math.round(openDecisions * 100 / decisions.length) : 0 }
    ];

    document.getElementById("summary-cards").innerHTML = cards.map(function (card) {
      return '<div class="card stat ' + card.cls + '">' +
        '<div class="label">' + esc(card.label) + "</div>" +
        '<div class="value">' + esc(card.value) + "</div>" +
        '<div class="sub">' + esc(card.sub) + "</div>" +
        '<div class="progress"><span style="width:' + Math.max(0, Math.min(100, card.pct)) + '%"></span></div>' +
        "</div>";
    }).join("");
  }

  function renderDistribution() {
    var el = document.getElementById("status-distribution");
    var c = countsByStatus();
    var total = tasks.length;
    if (!el) return;
    if (!total) {
      el.innerHTML = '<div class="empty">No hay tareas registradas.</div>';
      return;
    }

    var segments = STATUS_ORDER.filter(function (k) { return (c[k] || 0) > 0; }).map(function (k) {
      var n = c[k] || 0;
      var pct = n * 100 / total;
      var slug = STATUS_SLUG[k] || "pendiente";
      return '<button type="button" class="dist-seg" data-status-filter="' + esc(k) + '"' +
        ' title="' + esc(STATUS[k].label + ": " + n + " tareas") + '"' +
        ' style="width:' + pct + "%;background:var(--st-" + slug + "-line);color:var(--st-" + slug + '-ink)">' +
        (pct >= 7 ? esc(n) : "") + "</button>";
    }).join("");

    var legend = STATUS_ORDER.map(function (k) {
      var n = c[k] || 0;
      var slug = STATUS_SLUG[k] || "pendiente";
      return '<button type="button" class="legend-btn" data-status-filter="' + esc(k) + '">' +
        '<span class="legend-dot" style="background:var(--st-' + slug + '-line)"></span>' +
        esc(STATUS[k].label) + " <strong>" + n + "</strong></button>";
    }).join("");

    el.innerHTML =
      '<div class="dist-head"><h3>Distribución por estado</h3>' +
      '<span class="hint">' + total + " tareas · haz clic en un segmento para filtrar</span></div>" +
      '<div class="dist-bar" role="img" aria-label="Distribución de tareas por estado">' + segments + "</div>" +
      '<div class="dist-legend">' + legend + "</div>";
  }

  function renderCurrent() {
    var currentStage = stageById[meta.currentStageId] || null;
    var currentTask = meta.currentTaskId ? taskById[meta.currentTaskId] : null;
    var detected = false;
    if (!currentTask) {
      var inProgress = tasks.filter(function (t) { return t.status === "EN_PROGRESO"; });
      if (inProgress.length) { currentTask = inProgress[0]; detected = true; }
    }
    var nextTask = meta.nextTaskId ? taskById[meta.nextTaskId] : null;
    var currentEl = document.getElementById("current-main");
    var nextEl = document.getElementById("next-main");

    var stageHtml = currentStage
      ? '<div class="stage-line"><span class="stage-id">' + esc(currentStage.id) + " · " + esc(currentStage.name) + "</span>" +
        statusBadge(currentStage.status) + "</div>"
      : '<div class="stage-id">Sin etapa actual</div>';

    var progressHtml = "";
    if (currentStage) {
      var sp = stageProgress(currentStage);
      progressHtml = '<div class="progress" title="' + sp.pct + '% de la etapa"><span style="width:' + sp.pct + '%"></span></div>' +
        '<div class="stage-foot"><span>' + (currentStage.type === "preparation"
          ? sp.prepDone + "/" + sp.prepTotal + " tareas de preparación"
          : sp.done + "/" + sp.implTotal + " tareas de implementación") + "</span><span>" + sp.pct + "%</span></div>";
    }

    var taskHtml = currentTask
      ? '<div class="task-id">' + esc(currentTask.id) + "</div>" +
        "<h3>" + esc(currentTask.title) + "</h3>" +
        "<p>" + esc(currentTask.description) + "</p>" +
        '<div class="chips">' + statusBadge(currentTask.status) + priorityChip(currentTask.priority) +
        '<span class="chip mono">' + esc(currentTask.stageId) + "</span></div>" +
        (detected ? '<p style="color:var(--amber);font-size:12.5px">Detectada en el tracker como EN_PROGRESO aunque meta.currentTaskId no apunta a ella. Recupérala antes de empezar otra.</p>' : "") +
        '<div style="margin-top:10px"><button type="button" class="btn ghost small" data-focus-task="' + esc(currentTask.id) + '">Abrir en la lista</button></div>'
      : '<h3>Ninguna tarea en progreso</h3><p>La siguiente tarea disponible está lista para ejecutarse. Una sesión debe empezar por ella y detenerse al terminarla.</p>';

    currentEl.innerHTML = stageHtml + taskHtml + progressHtml +
      (currentStage ? '<div class="next-box"><h4>Objetivo de la etapa</h4><p>' + esc(currentStage.objective) + "</p></div>" : "") +
      (currentStage && currentStage.gate ? '<div class="next-box"><h4>Gate de la etapa</h4><p>' + esc(currentStage.gate) + "</p></div>" : "");

    if (!nextTask) {
      nextEl.innerHTML = '<h3 style="margin:0 0 6px">Próxima tarea</h3><div class="empty">No hay tarea siguiente registrada.</div>';
      return;
    }

    var deps = nextTask.dependencies || [];
    var depItems = deps.map(function (id) {
      var d = taskById[id];
      if (!d) return '<li><span class="no">✕</span> ' + esc(id) + " (no existe)</li>";
      var ok = d.status === "COMPLETADO";
      return '<li><span class="' + (ok ? "ok" : "no") + '">' + (ok ? "✓" : "✕") + "</span> " +
        '<span class="mono">' + esc(d.id) + "</span> · " + esc(d.title) + " · " + esc(statusInfo(d.status).label) + "</li>";
    }).join("");
    if (!depItems) depItems = '<li><span class="ok">✓</span> Sin dependencias.</li>';

    var pending = pendingDecisionIds(nextTask);

    nextEl.innerHTML =
      '<h3 style="margin:0 0 6px">Próxima tarea disponible</h3>' +
      '<div class="task-id">' + esc(nextTask.id) + "</div>" +
      '<h3 style="margin:4px 0 6px">' + esc(nextTask.title) + "</h3>" +
      '<p style="color:var(--muted);margin:0 0 10px">' + esc(nextTask.description) + "</p>" +
      '<div class="chips" style="margin-bottom:10px">' + statusBadge(nextTask.status) + priorityChip(nextTask.priority) +
      '<span class="chip mono">' + esc(nextTask.stageId) + "</span>" +
      '<span class="chip">' + (nextTask.acceptanceCriteria || []).length + " criterios</span>" +
      '<span class="chip">' + (nextTask.tests || []).length + " pruebas</span></div>" +
      '<div style="margin-bottom:10px"><button type="button" class="btn ghost small" data-focus-task="' + esc(nextTask.id) + '">Abrir en la lista</button></div>' +
      (pending.length
        ? '<div class="next-box alert"><h4>Decisión pendiente</h4><p>Esta tarea referencia ' +
          esc(pending.join(", ")) + ". Consulta DECISIONS.md y pide aprobación antes de implementarla.</p></div>"
        : "") +
      '<div class="next-box"><h4>Dependencias</h4><ul class="dep-list">' + depItems + "</ul></div>";
  }

  function renderVerification() {
    var el = document.getElementById("verification");
    if (!el) return;
    var v = meta.lastVerification;
    if (!v) {
      el.innerHTML = '<div class="verification-top"><h3>Última verificación</h3>' + statusBadge("PENDIENTE") + "</div>" +
        '<p class="hint" style="margin:8px 0 0">No hay verificación registrada en el tracker.</p>';
      return;
    }
    var ok = String(v.result || "").toUpperCase() === "OK";
    var checks = Array.isArray(v.checks) ? v.checks : [];
    el.innerHTML =
      '<div class="verification-top"><h3>Última verificación · ' + esc(fmtDate(v.date)) + "</h3>" +
      '<span class="badge ' + (ok ? "s-COMPLETADO" : "s-BLOQUEADO") + '">' + esc(v.result || "—") + "</span></div>" +
      (checks.length
        ? '<div class="check-list">' + checks.map(function (c) { return '<span class="chip ok">✓ ' + esc(c) + "</span>"; }).join("") + "</div>"
        : '<p class="hint" style="margin:8px 0 0">Sin comprobaciones detalladas.</p>') +
      '<p class="hint" style="margin:9px 0 0">Tracker actualizado: ' + esc(fmtDate(meta.lastUpdated)) +
      " · Última sesión: <strong style=\"color:var(--ink)\">" + esc(meta.lastSessionId || "—") + "</strong></p>";
  }

  function renderStages() {
    var el = document.getElementById("stage-grid");
    el.innerHTML = stages.slice().sort(function (a, b) { return (a.order || 0) - (b.order || 0); }).map(function (stage) {
      var sp = stageProgress(stage);
      var isCurrent = stage.id === meta.currentStageId;
      return '<div class="card stage-card' + (isCurrent ? " current" : "") + '" data-stage-jump="' + esc(stage.id) + '" role="button" tabindex="0" aria-label="Filtrar tareas de ' + esc(stage.id + " " + stage.name) + '">' +
        '<div class="stage-top"><span class="stage-id">' + esc(stage.id) + "</span>" +
        '<span style="display:flex;gap:6px;align-items:center;flex-wrap:wrap">' +
        (isCurrent ? '<span class="badge s-LISTO stage-current-tag">Actual</span>' : "") +
        statusBadge(sp.status) + "</span></div>" +
        "<h3>" + esc(stage.name) + "</h3>" +
        '<div class="obj">' + esc(stage.objective) + "</div>" +
        '<div class="progress"><span style="width:' + sp.pct + '%"></span></div>' +
        '<div class="stage-foot"><span>' +
          (stage.type === "preparation"
            ? sp.prepDone + "/" + sp.prepTotal + " tareas de preparación"
            : sp.done + "/" + sp.implTotal + " tareas de implementación") +
          '</span><span>' + sp.pct + "%</span></div>" +
        (stage.gate ? '<div class="stage-gate" title="' + esc(stage.gate) + '">Gate: ' + esc(stage.gate) + "</div>" : "") +
        "</div>";
    }).join("");
  }

  function setSelectValue(el, value, fallback) {
    if (!el) return;
    el.value = value;
    if (el.value !== value) el.value = fallback;
  }

  function populateFilters() {
    var c = countsByStatus();
    var statusSel = document.getElementById("status-filter");
    statusSel.innerHTML = '<option value="ALL">Todos los estados (' + tasks.length + ")</option>" +
      STATUS_ORDER.map(function (k) {
        return '<option value="' + k + '">' + esc(STATUS[k].label) + " (" + (c[k] || 0) + ")</option>";
      }).join("");

    var stageSel = document.getElementById("stage-filter");
    var stageCounts = {};
    tasks.forEach(function (t) { stageCounts[t.stageId] = (stageCounts[t.stageId] || 0) + 1; });
    stageSel.innerHTML = '<option value="ALL">Todas las etapas (' + tasks.length + ")</option>" +
      stages.slice().sort(function (a, b) { return (a.order || 0) - (b.order || 0); }).map(function (s) {
        return '<option value="' + esc(s.id) + '">' + esc(s.id) + " · " + esc(s.name) + " (" + (stageCounts[s.id] || 0) + ")</option>";
      }).join("");

    var prioSel = document.getElementById("priority-filter");
    var priorityCounts = {};
    tasks.forEach(function (t) { if (t.priority) priorityCounts[t.priority] = (priorityCounts[t.priority] || 0) + 1; });
    var priorities = Object.keys(priorityCounts).sort(function (a, b) {
      return (PRIORITY_ORDER[a] === undefined ? 99 : PRIORITY_ORDER[a]) - (PRIORITY_ORDER[b] === undefined ? 99 : PRIORITY_ORDER[b]);
    });
    prioSel.innerHTML = '<option value="ALL">Todas las prioridades</option>' +
      priorities.map(function (p) {
        return '<option value="' + esc(p) + '">' + esc(PRIORITY_LABEL[p] || p) + " (" + priorityCounts[p] + ")</option>";
      }).join("");
  }

  function restoreFilters() {
    document.getElementById("search").value = state.search || "";
    setSelectValue(document.getElementById("status-filter"), state.status || "ALL", "ALL");
    setSelectValue(document.getElementById("stage-filter"), state.stage || "ALL", "ALL");
    setSelectValue(document.getElementById("priority-filter"), state.priority || "ALL", "ALL");
    setSelectValue(document.getElementById("sort-filter"), state.sort || "stage", "stage");
    var group = document.getElementById("group-toggle");
    if (group) group.checked = state.groupByStage !== false;
  }

  function filterTasks() {
    var q = (state.search || "").trim().toLowerCase();
    var tokens = q ? q.split(/\s+/).filter(Boolean) : [];
    return tasks.filter(function (t) {
      if (state.status !== "ALL" && t.status !== state.status) return false;
      if (state.stage !== "ALL" && t.stageId !== state.stage) return false;
      if (state.priority !== "ALL" && t.priority !== state.priority) return false;
      if (!tokens.length) return true;
      var stage = stageById[t.stageId] || {};
      var hay = [
        t.id, t.title, t.description, t.stageId, stage.name, t.status, t.priority,
        t.category, t.createdAt, t.updatedAt,
        (t.files || []).join(" "),
        (t.acceptanceCriteria || []).join(" "),
        (t.tests || []).join(" "),
        (t.technicalNotes || ""),
        (t.dependencies || []).map(function (id) {
          var d = taskById[id];
          return id + " " + (d ? d.title : "");
        }).join(" "),
        (t.decisionIds || []).map(function (id) {
          var d = decisionById[id];
          return id + " " + (d ? d.title : "");
        }).join(" ")
      ].join(" ").toLowerCase();
      return tokens.every(function (token) { return hay.indexOf(token) !== -1; });
    });
  }

  function sortTasks(list) {
    var stageOrder = stageOrderMap();
    list.sort(function (a, b) {
      if (state.sort === "id") return natCompare(a.id, b.id);
      if (state.sort === "priority") {
        var pa = PRIORITY_ORDER[a.priority] === undefined ? 99 : PRIORITY_ORDER[a.priority];
        var pb = PRIORITY_ORDER[b.priority] === undefined ? 99 : PRIORITY_ORDER[b.priority];
        return pa !== pb ? pa - pb : natCompare(a.id, b.id);
      }
      if (state.sort === "status") {
        var sa = STATUS_ORDER.indexOf(a.status);
        var sb = STATUS_ORDER.indexOf(b.status);
        if (sa < 0) sa = 99;
        if (sb < 0) sb = 99;
        return sa !== sb ? sa - sb : natCompare(a.id, b.id);
      }
      if (state.sort === "updated") {
        var ua = String(a.updatedAt || "");
        var ub = String(b.updatedAt || "");
        if (ua !== ub) return ub.localeCompare(ua);
        return natCompare(a.id, b.id);
      }
      var oa = stageOrder[a.stageId] === undefined ? 99 : stageOrder[a.stageId];
      var ob = stageOrder[b.stageId] === undefined ? 99 : stageOrder[b.stageId];
      return oa !== ob ? oa - ob : natCompare(a.id, b.id);
    });
    return list;
  }

  function taskHtml(t) {
    var stage = stageById[t.stageId] || { name: t.stageId };
    var open = state.open[t.id] ? " open" : "";
    var deps = t.dependencies || [];
    var depsDone = deps.filter(function (id) {
      var d = taskById[id];
      return d && d.status === "COMPLETADO";
    }).length;
    var depsText = deps.length ? depsDone + "/" + deps.length + " deps" : "sin deps";
    var evidenceCount = (t.evidence || []).length;
    var pending = pendingDecisionIds(t);
    var isNext = meta.nextTaskId === t.id;

    var depChips = deps.map(function (id) {
      var d = taskById[id];
      var ok = d && d.status === "COMPLETADO";
      return '<span class="chip ' + (ok ? "ok" : "no") + '"><span class="mono">' + esc(id) + "</span>" +
        (d ? " · " + esc(d.title) : " · no existe") + "</span>";
    }).join("");
    if (!depChips) depChips = '<span class="chip ok">Sin dependencias</span>';

    var decisionChips = (t.decisionIds || []).map(function (id) {
      var d = decisionById[id];
      return '<span class="chip">' + esc(id) + (d ? " · " + esc(DECISION_LABEL[d.status] || d.status) : " · no existe") + "</span>";
    }).join("");
    if (!decisionChips) decisionChips = '<span class="chip">Ninguna</span>';

    var acceptance = (t.acceptanceCriteria || []).map(function (x) { return "<li>" + esc(x) + "</li>"; }).join("");
    var tests = (t.tests || []).map(function (x) { return "<li>" + esc(x) + "</li>"; }).join("");
    var files = (t.files || []).map(function (x) { return '<span class="chip mono">' + esc(x) + "</span>"; }).join("");
    var evidence = (t.evidence || []).map(function (x) { return '<span class="chip">' + esc(x) + "</span>"; }).join("");
    var logs = (t.executionLog || []).map(function (l) {
      var st = l.status && STATUS[l.status] ? statusBadge(l.status) + " " : "";
      return '<li class="log-item"><span class="mono">' + esc(l.sessionId || "—") + "</span> · " +
        esc(fmtDate(l.date)) + " · " + esc(l.agent || "") + " · " + st + esc(l.summary || "") + "</li>";
    }).join("");

    return '<details class="task"' + open + ' id="task-' + esc(t.id) + '" data-task="' + esc(t.id) + '">' +
      "<summary>" +
        '<span class="task-id">' + esc(t.id) + "</span>" +
        '<span class="task-title"><strong>' + esc(t.title) + "</strong><small>" +
          esc(t.stageId) + " · " + esc(stage.name) + " · " + esc(depsText) +
          (evidenceCount ? " · " + evidenceCount + " evidencia" + (evidenceCount === 1 ? "" : "s") : "") +
        "</small></span>" +
        '<span class="task-right">' +
          (isNext ? '<span class="badge s-LISTO">Siguiente</span>' : "") +
          statusBadge(t.status) +
          (pending.length ? '<span class="badge s-REQUIERE_DECISION">Decisión</span>' : "") +
          priorityChip(t.priority) +
          '<a class="task-link" href="#task-' + esc(t.id) + '" data-task-link="' + esc(t.id) + '" title="Enlace directo a ' + esc(t.id) + '">#</a>' +
        "</span>" +
      "</summary>" +
      '<div class="task-body">' +
        '<p class="desc">' + esc(t.description) + "</p>" +
        '<div class="meta-grid">' +
          '<div class="meta-block"><h4>Dependencias</h4><div class="chips">' + depChips + "</div></div>" +
          '<div class="meta-block"><h4>Decisiones</h4><div class="chips">' + decisionChips + "</div></div>" +
          '<div class="meta-block"><h4>Criterios de aceptación</h4><ul>' + (acceptance || "<li>Sin criterios registrados.</li>") + "</ul></div>" +
          '<div class="meta-block"><h4>Pruebas necesarias</h4><ul>' + (tests || "<li>Sin pruebas registradas.</li>") + "</ul></div>" +
          '<div class="meta-block" style="grid-column:1/-1"><h4>Archivos / sistemas</h4><div class="chips">' + (files || '<span class="chip">Sin archivos registrados</span>') + "</div></div>" +
          '<div class="meta-block" style="grid-column:1/-1"><h4>Observaciones técnicas</h4><p style="margin:0;color:var(--muted);font-size:13px">' + esc(t.technicalNotes || "—") + "</p></div>" +
          '<div class="meta-block" style="grid-column:1/-1"><h4>Evidencias</h4><div class="chips">' + (evidence || '<span class="chip">Sin evidencias todavía</span>') + "</div></div>" +
          '<div class="meta-block" style="grid-column:1/-1"><h4>Registro de ejecución</h4><ul class="log-list">' + (logs || "<li>Sin ejecuciones registradas.</li>") + "</ul></div>" +
          '<div class="meta-block"><h4>Creada</h4><p style="margin:0;color:var(--muted);font-size:12.5px">' + esc(fmtDateShort(t.createdAt)) + "</p></div>" +
          '<div class="meta-block"><h4>Actualizada</h4><p style="margin:0;color:var(--muted);font-size:12.5px">' + esc(fmtDateShort(t.updatedAt)) + "</p></div>" +
        "</div>" +
      "</div>" +
    "</details>";
  }

  function renderTasks() {
    var list = sortTasks(filterTasks());
    var el = document.getElementById("task-list");
    document.getElementById("task-count").textContent = list.length + " de " + tasks.length + " tareas";
    if (!list.length) {
      el.innerHTML = '<div class="empty">No hay tareas que coincidan con los filtros.<br>' +
        '<button type="button" class="btn ghost" data-clear-filters>Limpiar filtros</button></div>';
      return;
    }

    if (state.groupByStage === false) {
      el.innerHTML = list.map(taskHtml).join("");
    } else {
      var stageOrder = stageOrderMap();
      var groups = [];
      var byStage = {};
      list.forEach(function (t) {
        if (!byStage[t.stageId]) {
          byStage[t.stageId] = [];
          groups.push(t.stageId);
        }
        byStage[t.stageId].push(t);
      });
      groups.sort(function (a, b) {
        var oa = stageOrder[a] === undefined ? 99 : stageOrder[a];
        var ob = stageOrder[b] === undefined ? 99 : stageOrder[b];
        return oa !== ob ? oa - ob : natCompare(a, b);
      });

      el.innerHTML = groups.map(function (sid) {
        var stage = stageById[sid] || { id: sid, name: sid };
        var sp = stageProgress(stage);
        var items = byStage[sid];
        var isCurrent = sid === meta.currentStageId;
        var countsLabel = stage.type === "preparation"
          ? sp.prepDone + "/" + sp.prepTotal + " prep"
          : sp.done + "/" + sp.implTotal;
        return '<div class="stage-group">' +
          '<div class="stage-group-head" data-stage-jump="' + esc(sid) + '" role="button" tabindex="0" aria-label="Filtrar solo la etapa ' + esc(sid) + '">' +
            '<h3>' + esc(sid) + " · " + esc(stage.name) + (isCurrent ? ' <span class="badge s-LISTO">Actual</span>' : "") + "</h3>" +
            '<div class="group-right"><span class="group-meta">' + items.length + " visibles · " + countsLabel + " · " + sp.pct + "%</span>" +
            statusBadge(sp.status) + "</div>" +
          "</div>" +
          items.map(taskHtml).join("") +
          "</div>";
      }).join("");
    }

    Array.prototype.forEach.call(el.querySelectorAll("details.task"), function (d) {
      d.addEventListener("toggle", function () {
        if (d.open) state.open[d.getAttribute("data-task")] = true;
        else delete state.open[d.getAttribute("data-task")];
        saveUiState();
      });
    });
  }

  function renderHistory() {
    var el = document.getElementById("history-list");
    var moreEl = document.getElementById("history-more");
    var sorted = sessions.slice().sort(function (a, b) {
      return String(b.date || "").localeCompare(String(a.date || ""));
    });
    if (!sorted.length) {
      el.innerHTML = '<div class="empty">No hay sesiones registradas.</div>';
      moreEl.innerHTML = "";
      return;
    }
    var limit = 8;
    var shown = state.historyAll ? sorted : sorted.slice(0, limit);
    el.innerHTML = shown.map(function (s) {
      var isCurrent = s.id === meta.lastSessionId;
      var files = (s.files || []).map(function (f) { return "<li class=\"mono\">" + esc(f) + "</li>"; }).join("");
      var tests = (s.tests || []).map(function (x) { return "<li>" + esc(x) + "</li>"; }).join("");
      return '<div class="session' + (isCurrent ? " current" : "") + '">' +
        '<div class="session-top"><h3>' + esc(s.id) + " · " + esc(s.title || "Sesión") + "</h3>" +
        '<span class="date">' + esc(fmtDate(s.date)) + "</span></div>" +
        '<div class="chips" style="margin:7px 0">' + outcomeBadge(s.outcome) +
        (isCurrent ? '<span class="badge s-COMPLETADO">Última sesión</span>' : "") +
        (s.tasks || []).map(function (id) {
          return '<button type="button" class="chip mono" data-focus-task="' + esc(id) + '" title="Abrir ' + esc(id) + '">' + esc(id) + "</button>";
        }).join("") + "</div>" +
        "<p>" + esc(s.summary || "") + "</p>" +
        '<div class="files">Agente: ' + esc(s.agent || "—") + " · Resultado: " + esc(s.outcome || "—") + "</div>" +
        ((files || tests || s.notes)
          ? '<details><summary>Archivos, pruebas y notas</summary>' +
            (files ? "<h4 style=\"margin:8px 0 0;font-size:12px;color:var(--muted)\">Archivos</h4><ul>" + files + "</ul>" : "") +
            (tests ? "<h4 style=\"margin:8px 0 0;font-size:12px;color:var(--muted)\">Pruebas</h4><ul>" + tests + "</ul>" : "") +
            (s.notes ? '<p style="color:var(--muted)">' + esc(s.notes) + "</p>" : "") +
            "</details>"
          : "") +
        "</div>";
    }).join("");

    if (sorted.length > limit) {
      moreEl.innerHTML = '<button type="button" class="btn ghost" data-history-toggle>' +
        (state.historyAll ? "Mostrar solo las últimas " + limit : "Mostrar todas (" + sorted.length + ")") + "</button>";
    } else {
      moreEl.innerHTML = "";
    }
  }

  function renderDecisions() {
    var listEl = document.getElementById("decision-list");
    var summaryEl = document.getElementById("decision-summary");
    var toggle = document.getElementById("decisions-toggle");

    var counts = {};
    decisions.forEach(function (d) { counts[d.status] = (counts[d.status] || 0) + 1; });
    summaryEl.innerHTML = Object.keys(DECISION_LABEL).map(function (k) {
      var n = counts[k] || 0;
      return '<span class="chip">' + esc(DECISION_LABEL[k]) + " <strong style=\"color:var(--ink)\">" + n + "</strong></span>";
    }).join("");

    var resolved = { APROBADA: true, RECHAZADA: true, TECNICA_RESUELTA: true };
    var visible = decisions.filter(function (d) {
      return !state.hideResolved || !resolved[d.status];
    }).slice().sort(function (a, b) {
      var oa = DECISION_ORDER[a.status] === undefined ? 99 : DECISION_ORDER[a.status];
      var ob = DECISION_ORDER[b.status] === undefined ? 99 : DECISION_ORDER[b.status];
      return oa !== ob ? oa - ob : natCompare(a.id, b.id);
    });

    if (toggle) toggle.textContent = state.hideResolved ? "Mostrar resueltas" : "Ocultar resueltas";

    if (!visible.length) {
      listEl.innerHTML = '<div class="empty">No hay decisiones que mostrar con el filtro actual.</div>';
    } else {
      listEl.innerHTML = visible.map(function (d) {
        var affected = (d.affectedTasks || []).map(function (id) {
          return '<button type="button" class="chip mono" data-focus-task="' + esc(id) + '" title="Abrir ' + esc(id) + '">' + esc(id) + "</button>";
        }).join("") || '<span class="chip">Ninguna</span>';
        var options = (d.options || []).map(function (o) { return "<li>" + esc(o) + "</li>"; }).join("");
        return '<div class="decision">' +
          '<div class="decision-top"><h3>' + esc(d.id) + " · " + esc(d.title) + "</h3>" + decisionBadge(d.status) + "</div>" +
          (d.decision
            ? '<p><strong>Decisión:</strong> ' + esc(d.decision) + "</p>" +
              '<p class="rec"><strong>Recomendación inicial:</strong> ' + esc(d.recommendation || "—") + "</p>"
            : '<p class="rec"><strong>Recomendación:</strong> ' + esc(d.recommendation || "—") + "</p>") +
          '<div class="chips" style="margin-top:7px">' + affected + "</div>" +
          '<details><summary>Contexto, opciones e impacto</summary>' +
            (d.context ? '<p class="rec"><strong>Contexto:</strong> ' + esc(d.context) + "</p>" : "") +
            (options ? '<ul class="options">' + options + "</ul>" : "") +
            (d.impact ? '<p class="rec"><strong>Impacto:</strong> ' + esc(d.impact) + "</p>" : "") +
            '<p class="rec">Decidido por: ' + esc(d.decidedBy || "—") + " · Actualizado: " + esc(fmtDateShort(d.updatedAt)) + "</p>" +
          "</details>" +
          "</div>";
      }).join("");
    }

    var issueEl = document.getElementById("issue-list");
    if (!issues.length) {
      issueEl.innerHTML = '<div class="empty">Sin problemas registrados.</div>';
    } else {
      var issueOrder = { ABIERTO: 0, CONOCIDO: 1, RESUELTO: 2, CERRADO: 3 };
      issueEl.innerHTML = issues.slice().sort(function (a, b) {
        var oa = issueOrder[a.status] === undefined ? 99 : issueOrder[a.status];
        var ob = issueOrder[b.status] === undefined ? 99 : issueOrder[b.status];
        return oa !== ob ? oa - ob : natCompare(a.id, b.id);
      }).map(function (i) {
        var affected = (i.affectedTasks || []).map(function (id) {
          return '<button type="button" class="chip mono" data-focus-task="' + esc(id) + '" title="Abrir ' + esc(id) + '">' + esc(id) + "</button>";
        }).join("");
        return '<div class="issue sev-' + esc(i.severity || "") + '">' +
          '<div class="issue-top"><h4>' + esc(i.id) + " · " + esc(i.title) + "</h4>" +
          '<span style="display:flex;gap:5px;flex-wrap:wrap">' + issueStatusBadge(i.status) + issueSeverityChip(i.severity) + "</span></div>" +
          "<p>" + esc(i.description) + "</p>" +
          (affected ? '<div class="chips" style="margin-top:6px">' + affected + "</div>" : "") +
          '<div class="issue-meta">Creado: ' + esc(fmtDateShort(i.createdAt)) + "</div>" +
          "</div>";
      }).join("");
    }

    var blocked = tasks.filter(function (t) {
      return t.status === "BLOQUEADO" || t.status === "REQUIERE_DECISION" || t.status === "POSPUESTO" || pendingDecisionIds(t).length > 0;
    });
    var blockedEl = document.getElementById("blocked-list");
    if (!blocked.length) {
      blockedEl.innerHTML = '<div class="empty">No hay tareas bloqueadas, con decisión pendiente ni pospuestas.</div>';
    } else {
      blockedEl.innerHTML = blocked.map(function (t) {
        var extra = pendingDecisionIds(t).length ? " · Decisión: " + pendingDecisionIds(t).join(", ") : "";
        return '<div class="blocked-row">' +
          '<div class="title"><strong>' + esc(t.id) + " · " + esc(t.title) + "</strong><small>" + esc(t.technicalNotes || "Sin observaciones") + esc(extra) + "</small></div>" +
          '<div class="row-right">' + statusBadge(t.status) +
          '<button type="button" class="btn ghost small" data-focus-task="' + esc(t.id) + '">Abrir</button></div>' +
          "</div>";
      }).join("");
    }
  }

  function renderRules() {
    var el = document.getElementById("rules-body");
    if (!el) return;
    var rules = Array.isArray(meta.rules) ? meta.rules : [];
    var defs = Object.keys(statusDefs);
    el.innerHTML =
      (meta.progressDefinition ? '<p><strong>Definición de progreso:</strong> ' + esc(meta.progressDefinition) + "</p>" : "") +
      (rules.length ? "<h4 style=\"margin:12px 0 4px;font-size:13px\">Reglas de continuidad</h4><ul>" +
        rules.map(function (r) { return "<li>" + esc(r) + "</li>"; }).join("") + "</ul>" : "") +
      (defs.length ? '<h4 style="margin:14px 0 0;font-size:13px">Estados</h4><div class="rules-grid">' +
        defs.map(function (k) {
          var d = statusDefs[k] || {};
          return '<div class="rule-def">' + statusBadge(k) +
            "<p>" + esc(d.description || "Sin descripción.") + "</p></div>";
        }).join("") + "</div>" : "") +
      '<p class="hint" style="margin-top:12px">' + esc(meta.statusSource || "") +
      (meta.oneTaskPerSession ? " · Una tarea por sesión." : "") + "</p>";
  }

  function renderAll() {
    document.getElementById("project-name").textContent = meta.displayName || meta.project || "GalacticIndustry";
    document.getElementById("project-description").textContent = meta.description || "";
    document.getElementById("last-updated").textContent = fmtDate(meta.lastUpdated);
    document.getElementById("last-session").textContent = meta.lastSessionId || "—";
    var currentStage = stageById[meta.currentStageId];
    document.getElementById("top-stage").textContent = currentStage ? currentStage.id + " · " + currentStage.name : "—";
    var p = taskProgress();
    document.getElementById("top-progress").textContent = p.pct + "% (" + p.done + "/" + p.total + ")";
    document.title = "Panel de continuidad — " + (meta.displayName || "GalacticIndustry") + " · " + p.pct + "%";

    renderSummary();
    renderDistribution();
    renderCurrent();
    renderVerification();
    renderStages();
    renderTasks();
    renderHistory();
    renderDecisions();
    renderRules();
    document.getElementById("view-time").textContent = fmtDate(new Date().toISOString());
  }

  function toast(msg) {
    var el = document.getElementById("toast");
    el.textContent = msg;
    el.classList.add("show");
    clearTimeout(toast.timer);
    toast.timer = setTimeout(function () { el.classList.remove("show"); }, 2400);
  }

  function setDataSource(label, kind) {
    var el = document.getElementById("data-source");
    if (el) el.textContent = label;
    var dot = document.getElementById("source-dot");
    if (dot) {
      dot.className = "dot" + (kind === "warn" ? " warn" : kind === "err" ? " err" : "");
    }
  }

  function motionMode() {
    return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth";
  }

  function scrollToElement(el, block) {
    if (!el) return;
    try {
      el.scrollIntoView({ behavior: motionMode(), block: block || "start" });
    } catch (e) {
      el.scrollIntoView();
    }
  }

  function scrollToTasks() {
    scrollToElement(document.getElementById("tareas"), "start");
  }

  function focusTask(id, opts) {
    opts = opts || {};
    if (!taskById[id]) return;
    state.search = "";
    state.status = "ALL";
    state.stage = "ALL";
    state.priority = "ALL";
    state.open = {};
    state.open[id] = true;
    restoreFilters();
    renderTasks();
    if (opts.updateHash !== false && window.location.hash !== "#task-" + id) {
      try {
        history.pushState(null, "", "#task-" + id);
      } catch (e) {
        window.location.hash = "task-" + id;
      }
    }
    setTimeout(function () {
      var el = document.getElementById("task-" + id);
      if (!el) return;
      el.open = true;
      el.classList.remove("flash");
      void el.offsetWidth;
      el.classList.add("flash");
      setTimeout(function () { el.classList.remove("flash"); }, 1400);
      scrollToElement(el, "center");
    }, 30);
    saveUiState();
    if (!opts.silent) toast("Tarea " + id + " abierta");
  }

  function handleHash() {
    var m = /^#task-(.+)$/.exec(window.location.hash || "");
    if (!m) return;
    var id = decodeURIComponent(m[1]);
    if (taskById[id]) focusTask(id, { updateHash: false, silent: true });
  }

  function saveUiState() {
    try {
      sessionStorage.setItem("gi-panel-state", JSON.stringify(state));
    } catch (e) { /* file:// puede restringir sessionStorage; no es crítico */ }
    try {
      sessionStorage.setItem("gi-panel-scroll", String(window.scrollY || 0));
    } catch (e) { /* ignorar */ }
  }

  function loadUiState() {
    try {
      var raw = sessionStorage.getItem("gi-panel-state");
      if (raw) {
        var parsed = JSON.parse(raw);
        if (parsed && typeof parsed === "object") {
          state.search = parsed.search || "";
          state.status = parsed.status || "ALL";
          state.stage = parsed.stage || "ALL";
          state.priority = parsed.priority || "ALL";
          state.sort = parsed.sort || "stage";
          state.groupByStage = parsed.groupByStage !== false;
          state.historyAll = parsed.historyAll === true;
          state.hideResolved = parsed.hideResolved === true;
          state.open = parsed.open || {};
        }
      }
      var scroll = sessionStorage.getItem("gi-panel-scroll");
      if (scroll) {
        setTimeout(function () { window.scrollTo(0, parseInt(scroll, 10) || 0); }, 30);
      }
    } catch (e) { /* ignorar */ }
  }

  var TRACKER_URL = "../IMPLEMENTATION_TRACKER.json";
  var refreshing = false;

  function refreshData(showToast) {
    if (refreshing) return;
    if (window.location.protocol === "file:") {
      setDataSource("embebidos (file://)", "ok");
      if (showToast) {
        toast("Con file:// no se puede releer el JSON. Abre el panel por HTTP para actualizar sin recargar.");
      }
      return;
    }
    var btn = document.getElementById("reload-btn");
    refreshing = true;
    if (btn) btn.disabled = true;
    var url = TRACKER_URL + "?t=" + Date.now();
    fetch(url, { cache: "no-store" })
      .then(function (response) {
        if (!response.ok) throw new Error("HTTP " + response.status);
        return response.text();
      })
      .then(function (text) {
        var next = JSON.parse(text);
        var scrollY = window.scrollY;
        applyTrackerData(next);
        populateFilters();
        restoreFilters();
        renderAll();
        if (window.scrollTo) window.scrollTo(0, scrollY);
        setDataSource("JSON en vivo", "ok");
        if (showToast) toast("Datos actualizados sin recargar");
      })
      .catch(function (error) {
        setDataSource("embebidos (sin fetch)", "warn");
        if (showToast) toast("No se pudo leer el JSON: " + error.message);
      })
      .then(function () {
        refreshing = false;
        if (btn) btn.disabled = false;
      });
  }

  var AUTO_KEY = "gi-auto-reload";
  var autoTimer = null;

  function autoEnabled() {
    try { return localStorage.getItem(AUTO_KEY) === "1"; } catch (e) { return false; }
  }

  function setAuto(on) {
    try { localStorage.setItem(AUTO_KEY, on ? "1" : "0"); } catch (e) { /* ignorar */ }
    if (autoTimer) {
      clearInterval(autoTimer);
      autoTimer = null;
    }
    var btn = document.getElementById("auto-btn");
    btn.classList.toggle("on", on);
    btn.setAttribute("aria-pressed", on ? "true" : "false");
    btn.textContent = on ? "Auto: 10 s" : "Auto: off";
    if (on) {
      autoTimer = setInterval(function () { refreshData(false); }, 10000);
    }
  }

  var THEME_KEY = "gi-theme";

  function setTheme(theme) {
    var t = theme === "light" ? "light" : "dark";
    document.documentElement.setAttribute("data-theme", t);
    try { localStorage.setItem(THEME_KEY, t); } catch (e) { /* ignorar */ }
    var btn = document.getElementById("theme-btn");
    if (btn) {
      btn.textContent = t === "light" ? "☾ Oscuro" : "☀ Claro";
      btn.setAttribute("aria-pressed", t === "light" ? "true" : "false");
      btn.setAttribute("aria-label", t === "light" ? "Cambiar a tema oscuro" : "Cambiar a tema claro");
    }
    var metaTheme = document.querySelector('meta[name="theme-color"]');
    if (metaTheme) metaTheme.setAttribute("content", t === "light" ? "#f4f7fb" : "#05070d");
  }

  function initTheme() {
    var saved = null;
    try { saved = localStorage.getItem(THEME_KEY); } catch (e) { /* ignorar */ }
    if (!saved && window.matchMedia) {
      saved = window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
    }
    setTheme(saved || "dark");
  }

  function closest(el, selector) {
    while (el && el.nodeType === 1) {
      if (el.matches && el.matches(selector)) return el;
      el = el.parentElement;
    }
    return null;
  }

  function updateScrollUi() {
    var links = Array.prototype.slice.call(document.querySelectorAll("#nav-links a"));
    var y = (window.scrollY || window.pageYOffset || 0) + 100;
    var active = links[0] || null;
    links.forEach(function (a) {
      var target = document.querySelector(a.getAttribute("href"));
      if (target && target.offsetTop <= y) active = a;
    });
    links.forEach(function (a) {
      var on = a === active;
      a.classList.toggle("active", on);
      if (on) a.setAttribute("aria-current", "page");
      else a.removeAttribute("aria-current");
    });
    var toTop = document.getElementById("to-top");
    if (toTop) toTop.classList.toggle("show", (window.scrollY || 0) > 700);
  }

  function clearFilters() {
    state.search = "";
    state.status = "ALL";
    state.stage = "ALL";
    state.priority = "ALL";
    restoreFilters();
    renderTasks();
    saveUiState();
  }

  function initEvents() {
    var search = document.getElementById("search");
    var status = document.getElementById("status-filter");
    var stage = document.getElementById("stage-filter");
    var priority = document.getElementById("priority-filter");
    var sort = document.getElementById("sort-filter");
    var group = document.getElementById("group-toggle");

    search.addEventListener("input", function () { state.search = this.value; renderTasks(); saveUiState(); });
    status.addEventListener("change", function () { state.status = this.value; renderTasks(); saveUiState(); });
    stage.addEventListener("change", function () { state.stage = this.value; renderTasks(); saveUiState(); });
    priority.addEventListener("change", function () { state.priority = this.value; renderTasks(); saveUiState(); });
    sort.addEventListener("change", function () { state.sort = this.value; renderTasks(); saveUiState(); });
    group.addEventListener("change", function () { state.groupByStage = this.checked; renderTasks(); saveUiState(); });

    document.getElementById("clear-btn").addEventListener("click", clearFilters);

    document.getElementById("expand-btn").addEventListener("click", function () {
      Array.prototype.forEach.call(document.querySelectorAll("details.task"), function (d) { d.open = true; });
      toast("Tareas visibles expandidas");
    });
    document.getElementById("collapse-btn").addEventListener("click", function () {
      Array.prototype.forEach.call(document.querySelectorAll("details.task"), function (d) { d.open = false; });
      toast("Tareas contraídas");
    });

    document.getElementById("reload-btn").addEventListener("click", function () {
      refreshData(true);
    });

    document.getElementById("auto-btn").addEventListener("click", function () {
      setAuto(!autoEnabled());
      toast(autoEnabled() ? "Actualización automática cada 10 s" : "Actualización automática desactivada");
    });

    document.getElementById("theme-btn").addEventListener("click", function () {
      var next = document.documentElement.getAttribute("data-theme") === "light" ? "dark" : "light";
      setTheme(next);
      toast(next === "light" ? "Tema claro activado" : "Tema oscuro activado");
    });

    document.getElementById("decisions-toggle").addEventListener("click", function () {
      state.hideResolved = !state.hideResolved;
      renderDecisions();
      saveUiState();
    });

    document.getElementById("to-top").addEventListener("click", function () {
      try { window.scrollTo({ top: 0, behavior: motionMode() }); } catch (e) { window.scrollTo(0, 0); }
    });

    document.addEventListener("click", function (ev) {
      var el = closest(ev.target, "[data-focus-task]");
      if (el) {
        ev.preventDefault();
        focusTask(el.getAttribute("data-focus-task"), { updateHash: true });
        return;
      }
      el = closest(ev.target, "[data-task-link]");
      if (el) {
        ev.preventDefault();
        focusTask(el.getAttribute("data-task-link"), { updateHash: true });
        return;
      }
      el = closest(ev.target, "[data-stage-jump]");
      if (el) {
        ev.preventDefault();
        state.stage = el.getAttribute("data-stage-jump");
        restoreFilters();
        renderTasks();
        saveUiState();
        scrollToTasks();
        return;
      }
      el = closest(ev.target, "[data-status-filter]");
      if (el) {
        ev.preventDefault();
        state.status = el.getAttribute("data-status-filter");
        restoreFilters();
        renderTasks();
        saveUiState();
        scrollToTasks();
        toast("Filtro: " + statusInfo(state.status).label);
        return;
      }
      el = closest(ev.target, "[data-history-toggle]");
      if (el) {
        state.historyAll = !state.historyAll;
        renderHistory();
        saveUiState();
        return;
      }
      el = closest(ev.target, "[data-clear-filters]");
      if (el) {
        clearFilters();
      }
    });

    document.addEventListener("keydown", function (ev) {
      if (ev.key === "Enter" || ev.key === " ") {
        var el = closest(ev.target, "[data-stage-jump]");
        if (el) {
          ev.preventDefault();
          state.stage = el.getAttribute("data-stage-jump");
          restoreFilters();
          renderTasks();
          saveUiState();
          scrollToTasks();
        }
      }
      var tag = (ev.target && ev.target.tagName ? ev.target.tagName : "").toLowerCase();
      var typing = tag === "input" || tag === "select" || tag === "textarea";
      if (ev.key === "/" && !typing && !ev.metaKey && !ev.ctrlKey && !ev.altKey) {
        ev.preventDefault();
        search.focus();
        search.select();
      }
      if (ev.key === "Escape" && ev.target === search) {
        if (search.value) {
          search.value = "";
          state.search = "";
          renderTasks();
          saveUiState();
        } else {
          search.blur();
        }
      }
    });

    window.addEventListener("hashchange", handleHash);
    window.addEventListener("beforeunload", saveUiState);

    var ticking = false;
    window.addEventListener("scroll", function () {
      if (ticking) return;
      ticking = true;
      window.requestAnimationFrame(function () {
        updateScrollUi();
        ticking = false;
      });
    }, { passive: true });
  }

  loadUiState();
  initTheme();
  populateFilters();
  restoreFilters();
  renderAll();
  initEvents();
  setAuto(autoEnabled());
  setDataSource(window.location.protocol === "file:" ? "embebidos (file://)" : "cargando…", window.location.protocol === "file:" ? "ok" : "warn");
  if (window.location.protocol !== "file:") refreshData(false);
  handleHash();
  updateScrollUi();
})();
