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

  var UPDATE_STATUS = {
    PROPUESTA: { label: "Propuesta", cls: "u-PROPUESTA" },
    EN_REVISION: { label: "En revisión", cls: "u-EN_REVISION" },
    PENDIENTE_APROBACION: { label: "Pendiente de aprobación", cls: "u-PENDIENTE_APROBACION" },
    APROBADA: { label: "Aprobada", cls: "u-APROBADA" },
    EN_PROGRESO: { label: "En progreso", cls: "u-EN_PROGRESO" },
    BLOQUEADA: { label: "Bloqueada", cls: "u-BLOQUEADA" },
    COMPLETADA: { label: "Completada", cls: "u-COMPLETADA" },
    RECHAZADA: { label: "Rechazada", cls: "u-RECHAZADA" },
    ARCHIVADA: { label: "Archivada", cls: "u-ARCHIVADA" }
  };

  var UPDATE_ORDER = {
    EN_PROGRESO: 0,
    PENDIENTE_APROBACION: 1,
    EN_REVISION: 2,
    PROPUESTA: 3,
    APROBADA: 4,
    BLOQUEADA: 5,
    COMPLETADA: 6,
    RECHAZADA: 7,
    ARCHIVADA: 8
  };

  var UPDATE_TYPE_LABEL = {
    CORRECCION: "Corrección",
    MEJORA: "Mejora",
    DISENO: "Cambio de diseño",
    CARACTERISTICA: "Nueva característica",
    REEQUILIBRIO: "Reequilibrio",
    REFACTORIZACION: "Refactorización",
    TECNICA: "Técnica",
    INFRAESTRUCTURA: "Infraestructura"
  };

  var MISSING = "No documentado en el tracker.";

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
  var updates;
  var updateStatusDefs;
  var updateById;
  var reviewCatalog;
  var systemById;
  var featureById;
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
    versionHistoryAll: false,
    versionId: null,
    versionMode: "compact",
    hideResolved: false,
    open: {},
    review: { stages: {}, tasks: {}, updateTasks: {}, features: {}, systems: {}, updates: {} },
    reviewQuery: ""
  };

  var VERSION_MODE_KEY = "gi-version-mode";

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
    updates = DATA.updates || [];
    updateStatusDefs = DATA.updateStatusDefinitions || {};
    reviewCatalog = DATA.reviewCatalog || {};
    statusDefs = DATA.statusDefinitions || {};
    taskById = {};
    stageById = {};
    decisionById = {};
    updateById = {};
    systemById = {};
    featureById = {};
    tasks.forEach(function (t) { taskById[t.id] = t; });
    stages.forEach(function (s) { stageById[s.id] = s; });
    decisions.forEach(function (d) { decisionById[d.id] = d; });
    updates.forEach(function (u) {
      updateById[u.id] = u;
      (u.tasks || []).forEach(function (t) { taskById[t.id] = t; });
    });
    (reviewCatalog.systems || []).forEach(function (s) { systemById[s.id] = s; });
    (reviewCatalog.features || []).forEach(function (f) { featureById[f.id] = f; });
  }

  applyTrackerData(DATA);

  /* ---------------------------------------------------------------------
     Modelo de versiones de presentación
     --------------------------------------------------------------------- */

  function versionParts(value) {
    var parts = String(value || "").split(".");
    var major = parseInt(parts[0], 10);
    var minor = parseInt(parts[1], 10);
    return [isNaN(major) ? -1 : major, isNaN(minor) ? -1 : minor];
  }

  function versionCompare(a, b) {
    var pa = versionParts(a.displayVersion);
    var pb = versionParts(b.displayVersion);
    if (pa[0] !== pb[0]) return pa[0] - pb[0];
    if (pa[1] !== pb[1]) return pa[1] - pb[1];
    return natCompare(a.id, b.id);
  }

  function loadVersionMode() {
    try {
      return localStorage.getItem(VERSION_MODE_KEY) === "descriptive" ? "descriptive" : "compact";
    } catch (e) {
      return "compact";
    }
  }

  function saveVersionMode(mode) {
    try {
      localStorage.setItem(VERSION_MODE_KEY, mode === "descriptive" ? "descriptive" : "compact");
    } catch (e) { /* localStorage puede estar restringido en file:// */ }
  }

  function baseVersionStatus() {
    var impl = tasks.filter(function (t) {
      return t.category === "implementation" && t.countsForProgress === true;
    });
    if (impl.some(function (t) { return t.status === "EN_PROGRESO"; })) return "EN_PROGRESO";
    if (impl.some(function (t) { return t.status === "BLOQUEADO"; })) return "BLOQUEADO";
    if (impl.length && impl.every(function (t) { return t.status === "COMPLETADO"; })) return "COMPLETADO";
    if (impl.some(function (t) { return t.status === "LISTO"; })) return "LISTO";
    if (impl.some(function (t) { return t.status === "REQUIERE_DECISION"; })) return "REQUIERE_DECISION";
    if (impl.some(function (t) { return t.status === "PENDIENTE"; })) return "PENDIENTE";
    return stages.length && stages.every(function (s) { return s.status === "COMPLETADO"; })
      ? "COMPLETADO" : "PENDIENTE";
  }

  function initialVersion() {
    var iv = meta.initialVersion || {};
    return {
      id: iv.id || "BASE",
      kind: "base",
      internalId: "BASE",
      displayVersion: iv.displayVersion || "0.1",
      name: iv.name || "Implementación inicial",
      description: iv.description || "Plan maestro original y primera versión jugable del proyecto.",
      objective: iv.objective || "",
      date: iv.date || null,
      status: baseVersionStatus(),
      update: null
    };
  }

  function fallbackDisplayVersion(u) {
    var sorted = updates.slice().sort(function (a, b) {
      return String(a.createdAt || "").localeCompare(String(b.createdAt || "")) || natCompare(a.id, b.id);
    });
    var index = 0;
    for (var i = 0; i < sorted.length; i += 1) {
      if (sorted[i].id === u.id) { index = i; break; }
    }
    return (0.2 + index * 0.1).toFixed(1);
  }

  function updateVersion(u) {
    return {
      id: u.id,
      kind: "update",
      internalId: u.id,
      displayVersion: u.displayVersion || fallbackDisplayVersion(u),
      name: u.name || u.id,
      description: u.description || MISSING,
      objective: u.objective || u.motivation || u.description || MISSING,
      date: u.createdAt || null,
      status: u.status,
      update: u
    };
  }

  function versionList() {
    var list = [initialVersion()];
    updates.forEach(function (u) { list.push(updateVersion(u)); });
    return list.sort(versionCompare);
  }

  function versionById(id) {
    if (!id) return null;
    if (id === "BASE" || id === "PLAN-BASE" || id === "update-plan-base") return initialVersion();
    var list = versionList();
    for (var i = 0; i < list.length; i += 1) {
      if (list[i].id === id || list[i].internalId === id) return list[i];
    }
    return null;
  }

  function defaultVersionId() {
    if (meta.currentUpdateId && updateById[meta.currentUpdateId]) {
      return meta.currentUpdateId;
    }
    var development = updates.filter(function (u) {
      return u.status === "EN_PROGRESO" || u.status === "APROBADA" || u.status === "BLOQUEADA";
    });
    if (development.length) {
      development.sort(function (a, b) {
        return versionCompare(updateVersion(a), updateVersion(b));
      });
      return development[development.length - 1].id;
    }
    var list = versionList();
    return list.length ? list[list.length - 1].id : "BASE";
  }

  function ensureVersionSelection() {
    var current = state.versionId ? versionById(state.versionId) : null;
    if (!current) state.versionId = defaultVersionId();
    return versionById(state.versionId) || initialVersion();
  }

  function selectedVersion() {
    return ensureVersionSelection();
  }

  function versionTaskList(v) {
    if (!v) return [];
    if (v.kind === "base") return tasks;
    return (v.update && v.update.tasks) || [];
  }

  function versionProgress(v) {
    var list = versionTaskList(v);
    var impl = list.filter(function (t) {
      return t.category === "implementation" && t.countsForProgress === true;
    });
    var done = impl.filter(function (t) { return t.status === "COMPLETADO"; });
    var counts = {};
    list.forEach(function (t) { counts[t.status] = (counts[t.status] || 0) + 1; });
    var closed = v.kind === "update" && (v.status === "RECHAZADA" || v.status === "ARCHIVADA");
    return {
      list: list,
      impl: impl,
      done: done.length,
      total: impl.length,
      pct: impl.length ? Math.round(done.length * 100 / impl.length) : null,
      counts: counts,
      closed: closed,
      hasImpl: impl.length > 0
    };
  }

  function versionStatusInfo(v) {
    return v.kind === "base" ? statusInfo(v.status) : updateStatusInfo(v.status);
  }

  function versionStatusBadge(v) {
    return v.kind === "base" ? statusBadge(v.status) : updateStatusBadge(v.status);
  }

  function versionStatusSlug(v) {
    var map = {
      PENDIENTE: "pendiente",
      LISTO: "listo",
      EN_PROGRESO: "progreso",
      COMPLETADO: "completado",
      BLOQUEADO: "bloqueado",
      POSPUESTO: "pospuesto",
      REQUIERE_DECISION: "decision",
      PROPUESTA: "pendiente",
      EN_REVISION: "listo",
      PENDIENTE_APROBACION: "decision",
      APROBADA: "completado",
      BLOQUEADA: "bloqueado",
      COMPLETADA: "completado",
      RECHAZADA: "bloqueado",
      ARCHIVADA: "pospuesto"
    };
    return map[v.status] || "pendiente";
  }

  function allProjectTasks() {
    return tasks.concat(updateTasks());
  }

  function taskVersionId(task) {
    return task.updateId || "BASE";
  }

  function groupProgressFromTasks(list, fallbackStatus) {
    var impl = list.filter(function (t) {
      return t.category === "implementation" && t.countsForProgress === true;
    });
    var done = impl.filter(function (t) { return t.status === "COMPLETADO"; });
    var status = fallbackStatus || "PENDIENTE";
    if (impl.length) {
      if (done.length === impl.length) status = "COMPLETADO";
      else if (impl.some(function (t) { return t.status === "EN_PROGRESO"; })) status = "EN_PROGRESO";
      else if (impl.some(function (t) { return t.status === "REQUIERE_DECISION"; })) status = "REQUIERE_DECISION";
      else if (impl.some(function (t) { return t.status === "BLOQUEADO"; })) status = "BLOQUEADO";
      else if (impl.some(function (t) { return t.status === "LISTO"; })) status = "LISTO";
      else status = "PENDIENTE";
    }
    return {
      done: done.length,
      total: impl.length,
      pct: impl.length ? Math.round(done.length * 100 / impl.length) : (status === "COMPLETADO" ? 100 : 0),
      status: status
    };
  }

  function groupProgressText(group) {
    var gp = (group && group.progress) || {};
    if (group && group.type === "preparation") {
      return (gp.prepDone || 0) + "/" + (gp.prepTotal || 0) + " tareas de preparación · " + (gp.pct || 0) + "%";
    }
    var total = gp.implTotal !== undefined ? gp.implTotal : gp.total;
    if (!total) return statusInfo(gp.status).label;
    return (gp.done || 0) + "/" + total + " tareas de implementación · " + (gp.pct || 0) + "%";
  }

  function versionGroupList(v) {
    if (v.kind === "base") {
      return stages.slice().sort(function (a, b) { return (a.order || 0) - (b.order || 0); }).map(function (stage) {
        var stageTasks = tasks.filter(function (t) { return t.stageId === stage.id; });
        var sp = stageProgress(stage);
        var tests = [];
        stageTasks.forEach(function (t) { tests = tests.concat(t.tests || []); });
        return {
          id: stage.id,
          name: stage.name,
          type: stage.type,
          objective: stage.objective,
          description: stage.gate || "",
          gate: stage.gate || "",
          status: sp.status,
          progress: sp,
          taskIds: stageTasks.map(function (t) { return t.id; }),
          dependencies: stage.dependencies || [],
          tests: uniq(tests)
        };
      });
    }
    var groups = (v.update && v.update.groups) || [];
    return groups.map(function (group) {
      var groupTasks = (group.taskIds || []).map(function (id) { return taskById[id]; }).filter(Boolean);
      var gp = groupProgressFromTasks(groupTasks, group.status);
      return {
        id: group.id || "G",
        name: group.name || group.id || "Grupo",
        type: "implementation",
        objective: group.objective || "",
        description: group.description || "",
        gate: group.gate || "",
        status: gp.status,
        progress: gp,
        taskIds: group.taskIds || [],
        dependencies: group.dependencies || [],
        tests: group.tests || []
      };
    });
  }

  function versionGroupForTask(v, taskId) {
    var groups = versionGroupList(v);
    if (v.kind === "base") {
      var task = taskById[taskId];
      if (!task || !task.stageId) return null;
      for (var b = 0; b < groups.length; b += 1) {
        if (groups[b].id === task.stageId) return groups[b];
      }
      return null;
    }
    for (var i = 0; i < groups.length; i += 1) {
      if (groups[i].taskIds.indexOf(taskId) !== -1) return groups[i];
    }
    return null;
  }

  function taskContextLabel(task, v) {
    if (task.stageId && stageById[task.stageId]) {
      return task.stageId + " · " + stageById[task.stageId].name;
    }
    var group = v ? versionGroupForTask(v, task.id) : null;
    if (group) return group.id + " · " + group.name;
    return v ? "Actualización " + v.internalId : "Sin grupo";
  }

  function versionSessionIds(v) {
    var ids = {};
    versionTaskList(v).forEach(function (t) {
      ids[t.id] = true;
      (t.executionLog || []).forEach(function (l) {
        if (l.sessionId) ids[l.sessionId] = true;
      });
    });
    if (v.kind === "update") {
      ((v.update && v.update.implementationLog) || []).forEach(function (l) {
        if (l.sessionId) ids[l.sessionId] = true;
      });
    }
    return ids;
  }

  function versionSessions(v) {
    var ids = versionSessionIds(v);
    return sessions.filter(function (s) {
      if (ids[s.id]) return true;
      return (s.tasks || []).some(function (id) { return ids[id] === true; });
    });
  }

  function sessionVersionLabels(s) {
    var seen = {};
    var out = [];
    (s.tasks || []).forEach(function (id) {
      var t = taskById[id];
      if (!t) return;
      var vid = taskVersionId(t);
      if (seen[vid]) return;
      var v = versionById(vid);
      if (!v) return;
      seen[vid] = true;
      out.push(v);
    });
    return out.sort(versionCompare);
  }

  function nextDisplayVersion() {
    var major = 0;
    var minor = 0;
    var found = false;
    versionList().forEach(function (v) {
      var p = versionParts(v.displayVersion);
      if (p[0] < 0 || p[1] < 0) return;
      if (!found || p[0] > major || (p[0] === major && p[1] > minor)) {
        major = p[0];
        minor = p[1];
        found = true;
      }
    });
    return found ? major + "." + (minor + 1) : "0.1";
  }

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

  function updateStatusInfo(status) {
    return UPDATE_STATUS[status] || { label: status || "—", cls: "" };
  }

  function updateStatusBadge(status) {
    var s = updateStatusInfo(status);
    var def = updateStatusDefs[status];
    var title = def && def.description ? ' title="' + esc(def.description) + '"' : "";
    return '<span class="badge ' + s.cls + '"' + title + ">" + esc(s.label) + "</span>";
  }

  function updateTypeLabel(type) {
    return UPDATE_TYPE_LABEL[type] || type || "—";
  }

  function isApprovedUpdate(u) {
    return u && (u.status === "APROBADA" || u.status === "EN_PROGRESO" || u.status === "COMPLETADA");
  }

  function updateProgress(u) {
    var list = (u && u.tasks) || [];
    var impl = list.filter(function (t) {
      return t.category === "implementation" && t.countsForProgress === true;
    });
    var done = impl.filter(function (t) { return t.status === "COMPLETADO"; });
    return {
      done: done.length,
      total: impl.length,
      pct: impl.length ? Math.round(done.length * 100 / impl.length) : 0
    };
  }

  function updateTasks() {
    var out = [];
    updates.forEach(function (u) {
      (u.tasks || []).forEach(function (t) { out.push(t); });
    });
    return out;
  }

  function isCurrentUpdate(u) {
    var cu = currentUpdate();
    return !!(cu && u && cu.id === u.id);
  }

  function currentUpdate() {
    if (meta.currentUpdateId && updateById[meta.currentUpdateId]) return updateById[meta.currentUpdateId];
    var active = updates.filter(function (u) {
      return u.status === "EN_PROGRESO" || u.status === "PENDIENTE_APROBACION" || u.status === "PROPUESTA" || u.status === "APROBADA";
    });
    if (active.length) return active[active.length - 1];
    return updates.length ? updates[updates.length - 1] : null;
  }

  function nextUpdateId() {
    var max = 0;
    updates.forEach(function (u) {
      var m = /^UPD-(\d+)$/.exec(u.id || "");
      if (m) max = Math.max(max, parseInt(m[1], 10));
    });
    return "UPD-" + String(max + 1).padStart(3, "0");
  }

  function updateStatusCounts() {
    var c = {};
    updates.forEach(function (u) { c[u.status] = (c[u.status] || 0) + 1; });
    return c;
  }

  function approvedUpdateCount() {
    return updates.filter(isApprovedUpdate).length;
  }

  function pendingApprovalCount() {
    return updates.filter(function (u) {
      return u.status === "PROPUESTA" || u.status === "EN_REVISION" || u.status === "PENDIENTE_APROBACION";
    }).length;
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
    var versions = versionList().length;

    var cards = [
      { label: "Progreso del plan inicial", value: p.pct + "%", sub: p.done + " de " + p.total + " tareas reales", cls: "accent", pct: p.pct },
      { label: "Preparación", value: prep.pct + "%", sub: prep.done + " de " + prep.total + " tareas", cls: "good", pct: prep.pct },
      { label: "Etapas completadas", value: stageDone + " / " + stages.length, sub: "del plan inicial", cls: stageDone === stages.length ? "good" : "", pct: stages.length ? Math.round(stageDone * 100 / stages.length) : 0 },
      { label: "Versiones registradas", value: String(versions), sub: "0.1 + " + updates.length + " actualizaciones", cls: "accent", pct: 0, noBar: true },
      { label: "Actualizaciones", value: approvedUpdateCount() + " / " + updates.length, sub: updates.length ? "aprobadas o completadas" : "ninguna registrada", cls: updates.length ? "accent" : "", pct: updates.length ? Math.round(approvedUpdateCount() * 100 / updates.length) : 0 },
      { label: "Pendientes de aprobación", value: String(pendingApprovalCount()), sub: "propuestas sin autorizar", cls: pendingApprovalCount() ? "decision" : "good", pct: updates.length ? Math.round(pendingApprovalCount() * 100 / updates.length) : 0 },
      { label: "Tareas completadas", value: String(c.COMPLETADO || 0), sub: "de " + tasks.length + " del plan inicial", cls: "good", pct: tasks.length ? Math.round((c.COMPLETADO || 0) * 100 / tasks.length) : 0 },
      { label: "En progreso", value: String(c.EN_PROGRESO || 0), sub: c.EN_PROGRESO ? "recuperar antes de otra tarea" : "ninguna tarea activa", cls: c.EN_PROGRESO ? "warn" : "", pct: tasks.length ? Math.round((c.EN_PROGRESO || 0) * 100 / tasks.length) : 0 },
      { label: "Pendientes", value: String(pending), sub: "pendientes + listas del plan inicial", cls: "warn", pct: tasks.length ? Math.round(pending * 100 / tasks.length) : 0 },
      { label: "Bloqueadas", value: String(c.BLOQUEADO || 0), sub: "requieren intervención", cls: (c.BLOQUEADO || 0) > 0 ? "bad" : "good", pct: tasks.length ? Math.round((c.BLOQUEADO || 0) * 100 / tasks.length) : 0 },
      { label: "Decisiones abiertas", value: String(openDecisions), sub: "de " + decisions.length + " registradas", cls: openDecisions > 0 ? "decision" : "good", pct: decisions.length ? Math.round(openDecisions * 100 / decisions.length) : 0 }
    ];

    document.getElementById("summary-cards").innerHTML = cards.map(function (card) {
      return '<div class="card stat ' + card.cls + '">' +
        '<div class="label">' + esc(card.label) + "</div>" +
        '<div class="value">' + esc(card.value) + "</div>" +
        '<div class="sub">' + esc(card.sub) + "</div>" +
        (card.noBar ? "" : '<div class="progress"><span style="width:' + Math.max(0, Math.min(100, card.pct)) + '%"></span></div>') +
        "</div>";
    }).join("");
  }

  function distributionMarkup(list, title, hint, interactive) {
    var c = {};
    (list || []).forEach(function (t) { c[t.status] = (c[t.status] || 0) + 1; });
    var total = (list || []).length;
    if (!total) return '<div class="empty">No hay tareas registradas.</div>';

    var segments = STATUS_ORDER.filter(function (k) { return (c[k] || 0) > 0; }).map(function (k) {
      var n = c[k] || 0;
      var pct = n * 100 / total;
      var slug = STATUS_SLUG[k] || "pendiente";
      var tag = interactive ? "button" : "span";
      var attrs = interactive
        ? ' type="button" class="dist-seg" data-status-filter="' + esc(k) + '"'
        : ' class="dist-seg" aria-hidden="true"';
      return "<" + tag + attrs +
        ' title="' + esc(STATUS[k].label + ": " + n + " tareas") + '"' +
        ' style="width:' + pct + "%;background:var(--st-" + slug + "-line);color:var(--st-" + slug + '-ink)">' +
        (pct >= 7 ? esc(n) : "") + "</" + tag + ">";
    }).join("");

    var legend = STATUS_ORDER.map(function (k) {
      var n = c[k] || 0;
      var slug = STATUS_SLUG[k] || "pendiente";
      var tag = interactive ? "button" : "span";
      var attrs = interactive
        ? ' type="button" class="legend-btn" data-status-filter="' + esc(k) + '"'
        : ' class="legend-btn"';
      return "<" + tag + attrs + ">" +
        '<span class="legend-dot" style="background:var(--st-' + slug + '-line)"></span>' +
        esc(STATUS[k].label) + " <strong>" + n + "</strong></" + tag + ">";
    }).join("");

    return '<div class="dist-head"><h3>' + esc(title) + "</h3>" +
      '<span class="hint">' + esc(hint || (total + " tareas")) + "</span></div>" +
      '<div class="dist-bar" role="img" aria-label="Distribución de tareas por estado">' + segments + "</div>" +
      '<div class="dist-legend">' + legend + "</div>";
  }

  function renderDistribution() {
    var el = document.getElementById("status-distribution");
    if (!el) return;
    el.classList.add("static");
    el.innerHTML = distributionMarkup(tasks, "Distribución global por estado", tasks.length + " tareas del plan inicial", false);
  }

  function renderCurrent() {
    var currentStage = stageById[meta.currentStageId] || null;
    var currentTask = meta.currentTaskId ? taskById[meta.currentTaskId] : null;
    var detected = false;
    if (!currentTask) {
      var inProgress = allProjectTasks().filter(function (t) { return t.status === "EN_PROGRESO"; });
      if (inProgress.length) { currentTask = inProgress[0]; detected = true; }
    }
    var nextTask = meta.nextTaskId ? taskById[meta.nextTaskId] : null;
    var currentEl = document.getElementById("current-main");
    var nextEl = document.getElementById("next-main");

    var currentTaskVersion = currentTask && currentTask.updateId ? versionById(currentTask.updateId) : null;
    var stageHtml;
    var progressHtml = "";
    if (currentTaskVersion) {
      var vp = versionProgress(currentTaskVersion);
      stageHtml = '<div class="stage-line"><span class="stage-id">v' + esc(currentTaskVersion.displayVersion) + " · " + esc(currentTaskVersion.internalId) + "</span>" +
        versionStatusBadge(currentTaskVersion) + "</div>";
      if (vp.pct !== null) {
        progressHtml = '<div class="progress" title="' + vp.pct + '% de la versión"><span style="width:' + vp.pct + '%"></span></div>' +
          '<div class="stage-foot"><span>' + vp.done + "/" + vp.total + " tareas de implementación</span><span>" + vp.pct + "%</span></div>";
      }
    } else {
      stageHtml = currentStage
        ? '<div class="stage-line"><span class="stage-id">' + esc(currentStage.id) + " · " + esc(currentStage.name) + "</span>" +
          statusBadge(currentStage.status) + "</div>"
        : '<div class="stage-id">Sin etapa actual</div>';
      if (currentStage) {
        var sp = stageProgress(currentStage);
        progressHtml = '<div class="progress" title="' + sp.pct + '% de la etapa"><span style="width:' + sp.pct + '%"></span></div>' +
          '<div class="stage-foot"><span>' + (currentStage.type === "preparation"
            ? sp.prepDone + "/" + sp.prepTotal + " tareas de preparación"
            : sp.done + "/" + sp.implTotal + " tareas de implementación") + "</span><span>" + sp.pct + "%</span></div>";
      }
    }

    var taskHtml = currentTask
      ? '<div class="task-id">' + esc(currentTask.id) + "</div>" +
        "<h3>" + esc(currentTask.title) + "</h3>" +
        "<p>" + esc(currentTask.description) + "</p>" +
        '<div class="chips">' + statusBadge(currentTask.status) + priorityChip(currentTask.priority) +
        '<span class="chip">' + esc(taskContextLabel(currentTask, versionById(taskVersionId(currentTask)))) + "</span>" +
        (currentTask.updateId ? '<span class="chip mono">' + esc(currentTask.updateId) + "</span>" : "") + "</div>" +
        (detected
          ? '<p style="color:var(--amber);font-size:12.5px">' +
            (currentTask.updateId
              ? "Detectada como EN_PROGRESO en la actualización " + esc(currentTask.updateId) + "."
              : "Detectada en el tracker como EN_PROGRESO aunque meta.currentTaskId no apunta a ella. Recupérala antes de empezar otra.") +
            "</p>"
          : "") +
        '<div style="margin-top:10px"><button type="button" class="btn ghost small" data-focus-task="' + esc(currentTask.id) + '">Abrir en la lista</button></div>'
      : '<h3>Ninguna tarea en progreso</h3><p>La siguiente tarea disponible está lista para ejecutarse. Una sesión debe empezar por ella y detenerse al terminarla.</p>';

    currentEl.innerHTML = stageHtml + taskHtml + progressHtml +
      (currentTaskVersion
        ? '<div class="next-box"><h4>Objetivo de la versión</h4><p>' + esc(currentTaskVersion.objective || "No documentado en el tracker.") + "</p></div>"
        : (currentStage ? '<div class="next-box"><h4>Objetivo de la etapa</h4><p>' + esc(currentStage.objective) + "</p></div>" : "")) +
      (!currentTaskVersion && currentStage && currentStage.gate ? '<div class="next-box"><h4>Gate de la etapa</h4><p>' + esc(currentStage.gate) + "</p></div>" : "") +
      (function () {
        var cu = currentUpdate();
        if (!cu) return "";
        var up = updateProgress(cu);
        return '<div class="next-box"><h4>Actualización actual</h4><p><strong>' + esc(cu.id) + " · " + esc(cu.name) + "</strong> · " +
          esc(updateStatusInfo(cu.status).label) + " · " + up.done + "/" + up.total + " tareas completadas.</p>" +
          '<div style="margin-top:8px"><button type="button" class="btn ghost small" data-focus-update="' + esc(cu.id) + '">Abrir actualización</button></div></div>';
      })();

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
      '<span class="chip">' + esc(taskContextLabel(nextTask, initialVersion())) + "</span>" +
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

  function versionGroupHtml(v, group) {
    var gp = group.progress || groupProgressFromTasks([], group.status);
    var taskChips = (group.taskIds || []).map(function (id) {
      var t = taskById[id];
      if (!t) return '<span class="chip no">' + esc(id) + " · no existe</span>";
      return '<button type="button" class="chip mono" data-focus-task="' + esc(id) + '" title="Abrir ' + esc(id) + '">' + esc(id) + "</button>" +
        '<span class="chip">' + esc(t.title) + " · " + esc(statusInfo(t.status).label) + "</span>";
    }).join("") || '<span class="chip">Sin tareas asociadas</span>';
    var deps = (group.dependencies || []).map(function (id) {
      var t = taskById[id];
      return '<span class="chip">' + esc(id) + (t ? " · " + esc(t.title) : " · no existe") + "</span>";
    }).join("") || '<span class="chip ok">Sin dependencias</span>';
    var tests = (group.tests || []).map(function (x) { return "<li>" + esc(x) + "</li>"; }).join("");
    var open = state.open["group-" + group.id] ? " open" : "";
    return '<details class="version-group"' + open + ' id="version-group-' + esc(group.id) + '" data-version-group="' + esc(group.id) + '">' +
      "<summary>" +
        '<span class="group-id">' + esc(group.id) + "</span>" +
        '<span class="group-main"><strong>' + esc(group.name) + "</strong><small>" +
          esc(groupProgressText(group)) +
        "</small></span>" +
        '<span class="group-right">' + statusBadge(group.status) + "</span>" +
      "</summary>" +
      '<div class="group-body">' +
        (group.objective ? '<p class="desc">' + esc(group.objective) + "</p>" : "") +
        '<div class="meta-grid">' +
          '<div class="meta-block"><h4>Descripción / gate</h4><p style="margin:0;color:var(--muted);font-size:13px">' + esc(group.description || group.gate || "—") + "</p></div>" +
          '<div class="meta-block"><h4>Estado y progreso</h4><p style="margin:0;color:var(--muted);font-size:13px">' +
            esc(statusInfo(group.status).label) + " · " + (gp ? gp.pct : 0) + "%" +
          "</p></div>" +
          '<div class="meta-block"><h4>Dependencias</h4><div class="chips">' + deps + "</div></div>" +
          '<div class="meta-block"><h4>Pruebas relevantes</h4><ul>' + (tests || "<li>Sin pruebas registradas.</li>") + "</ul></div>" +
        "</div>" +
        '<div class="meta-block" style="margin-top:12px"><h4>Tareas asociadas</h4><div class="chips">' + taskChips + "</div></div>" +
      "</div>" +
    "</details>";
  }

  function renderVersionGroups(v) {
    var el = document.getElementById("version-groups");
    var titleEl = document.getElementById("version-groups-title");
    var hintEl = document.getElementById("version-groups-hint");
    if (!el) return;
    var groups = versionGroupList(v);
    if (titleEl) titleEl.textContent = v.kind === "base" ? "Etapas del plan inicial" : (groups.length ? "Grupos de trabajo de la versión" : "Grupos de trabajo");
    if (hintEl) {
      hintEl.textContent = v.kind === "base"
        ? "Etapas S0–S11 del plan histórico. Solo se muestran en la versión 0.1."
        : (groups.length ? "Grupos declarados en el tracker para esta actualización." : "Esta versión no declara etapas ni grupos propios.");
    }
    if (!groups.length) {
      el.innerHTML = '<div class="empty">Esta versión no declara etapas ni grupos propios. Sus tareas se muestran en la sección Tareas, sin jerarquía inventada.</div>';
      return;
    }
    el.innerHTML = groups.map(function (group) { return versionGroupHtml(v, group); }).join("");
    Array.prototype.forEach.call(el.querySelectorAll("details.version-group"), function (d) {
      d.addEventListener("toggle", function () {
        if (d.open) state.open["group-" + d.getAttribute("data-version-group")] = true;
        else delete state.open["group-" + d.getAttribute("data-version-group")];
        saveUiState();
      });
    });
  }

  function compactVersionHtml(v, selected) {
    var sel = selected && selected.id === v.id;
    var label = versionStatusInfo(v).label;
    return '<button type="button" class="version-chip' + (sel ? " selected" : "") + '" data-select-version="' + esc(v.id) + '"' +
      ' aria-pressed="' + (sel ? "true" : "false") + '" title="' + esc("v" + v.displayVersion + " · " + v.internalId + " · " + v.name + " · " + label) + '">' +
      '<span class="version-number">v' + esc(v.displayVersion) + "</span>" +
      '<span class="version-id">' + esc(v.internalId) + "</span>" +
      '<span class="version-state"><span class="version-dot" style="background:var(--st-' + esc(versionStatusSlug(v)) + '-line)"></span>' + esc(label) + "</span>" +
    "</button>";
  }

  function descriptiveVersionHtml(v, selected) {
    var sel = selected && selected.id === v.id;
    var p = versionProgress(v);
    var progress = p.pct === null
      ? '<span class="hint">Sin tareas de implementación registradas.</span>'
      : '<span class="progress"><span style="width:' + p.pct + '%"></span></span><span class="version-pct">' + p.pct + "% · " + p.done + "/" + p.total + "</span>";
    return '<button type="button" class="version-card' + (sel ? " selected" : "") + '" data-select-version="' + esc(v.id) + '"' +
      ' aria-pressed="' + (sel ? "true" : "false") + '">' +
      '<span class="version-card-top"><span class="version-number">v' + esc(v.displayVersion) + "</span>" +
        '<span class="version-id">' + esc(v.internalId) + "</span>" + versionStatusBadge(v) + "</span>" +
      '<strong class="version-name">' + esc(v.name) + "</strong>" +
      '<span class="version-desc">' + esc(v.description || MISSING) + "</span>" +
      '<span class="version-objective"><b>Objetivo:</b> ' + esc(v.objective || MISSING) + "</span>" +
      '<span class="version-progress-line">' + progress + "</span>" +
    "</button>";
  }

  function renderVersionSelector() {
    var el = document.getElementById("version-selector");
    var summary = document.getElementById("version-summary");
    if (!el) return;
    var selected = selectedVersion();
    var list = versionList().slice().reverse();
    var compact = state.versionMode !== "descriptive";
    el.className = "version-strip " + (compact ? "compact" : "descriptive");
    el.innerHTML = list.map(function (v) {
      return compact ? compactVersionHtml(v, selected) : descriptiveVersionHtml(v, selected);
    }).join("");
    if (summary) {
      var current = currentUpdate();
      var chips = [
        '<span class="chip"><strong style="color:var(--ink)">' + versionList().length + "</strong> versiones</span>",
        '<span class="chip">Seleccionada <strong style="color:var(--ink)">v' + esc(selected.displayVersion) + "</strong></span>",
        '<span class="chip">Actual <strong style="color:var(--ink)">' + (current ? esc(current.id) : "—") + "</strong></span>",
        '<span class="chip">Pendientes de aprobación <strong style="color:var(--ink)">' + pendingApprovalCount() + "</strong></span>"
      ];
      summary.innerHTML = chips.join("");
    }
    var modeBtn = document.getElementById("version-mode-btn");
    if (modeBtn) {
      modeBtn.textContent = compact ? "Vista descriptiva" : "Vista compacta";
      modeBtn.setAttribute("aria-pressed", compact ? "false" : "true");
      modeBtn.setAttribute("title", compact ? "Expandir la información de las versiones" : "Volver al modo compacto");
    }
  }

  function renderTimeline() {
    var el = document.getElementById("version-timeline");
    if (!el) return;
    var selected = selectedVersion();
    el.innerHTML = versionList().map(function (v) {
      var sel = selected.id === v.id;
      return '<button type="button" class="timeline-point' + (sel ? " selected" : "") + '" data-select-version="' + esc(v.id) + '"' +
        ' aria-pressed="' + (sel ? "true" : "false") + '" title="' + esc("v" + v.displayVersion + " · " + v.internalId + " · " + v.name) + '">' +
        '<span class="timeline-dot" style="background:var(--st-' + esc(versionStatusSlug(v)) + '-line)"></span>' +
        '<span class="timeline-version">v' + esc(v.displayVersion) + "</span>" +
        '<span class="timeline-id">' + esc(v.internalId) + "</span>" +
        '<span class="timeline-name">' + esc(v.name) + "</span>" +
        versionStatusBadge(v) +
      "</button>";
    }).join("");
  }

  function renderVersionHero(v) {
    var el = document.getElementById("version-hero");
    if (!el) return;
    var p = versionProgress(v);
    var current = meta.currentUpdateId === v.id;
    var approval = v.kind === "update" ? (v.update.approval || {}) : null;
    var progress = p.pct === null
      ? '<span class="hint">Sin tareas de implementación registradas.</span>'
      : "<strong>" + p.pct + "%</strong><span>" + p.done + " / " + p.total + " tareas de implementación</span>" +
        '<span class="progress"><span style="width:' + p.pct + '%"></span></span>';
    var docHref = v.kind === "update" && v.update.document ? "../" + v.update.document.replace(/^docs\//, "") : "";
    el.innerHTML = '<div class="card version-hero">' +
      '<div class="version-hero-top">' +
        '<div class="version-title">' +
          '<span class="version-kicker">Versión ' + esc(v.displayVersion) + "</span>" +
          "<h2>" + esc(v.name) + "</h2>" +
          '<div class="version-meta">' +
            '<span class="mono">' + esc(v.internalId) + "</span>" +
            "<span>" + esc(v.date ? fmtDateShort(v.date) : "—") + "</span>" +
            versionStatusBadge(v) +
            (current ? '<span class="badge s-LISTO">Actual</span>' : "") +
          "</div>" +
        "</div>" +
        '<div class="version-hero-progress">' + progress + "</div>" +
      "</div>" +
      '<p class="version-desc">' + esc(v.description || MISSING) + "</p>" +
      '<div class="version-objectives"><h3>Objetivo principal</h3><p>' + esc(v.objective || MISSING) + "</p></div>" +
      (v.kind === "update" && v.update.motivation
        ? '<div class="version-objectives"><h3>Motivación</h3><p>' + esc(v.update.motivation) + "</p></div>"
        : "") +
      (approval
        ? '<div class="version-approval"><h3>Aprobación</h3><p>' +
            (approval.status === "APROBADA"
              ? "Aprobada por " + esc(approval.approvedBy || "—") + " · " + esc(fmtDate(approval.approvedAt)) +
                (approval.basis ? " · " + esc(approval.basis) : "")
              : "Aprobación pendiente. No se implementa hasta que el usuario la autorice.") +
          "</p>" + (docHref ? '<p>Documento: <a href="' + esc(docHref) + '" target="_blank" rel="noopener">' + esc(v.update.document) + "</a></p>" : "") +
          "</div>"
        : '<div class="version-approval"><h3>Plan histórico</h3><p>Plan maestro original. Se conserva como referencia y no se reabre; los cambios posteriores viven en actualizaciones.</p></div>') +
    "</div>";
  }

  function renderVersionProgress(v) {
    var el = document.getElementById("version-progress");
    if (!el) return;
    var p = versionProgress(v);
    var c = p.counts;
    var closed = p.closed;
    var cards = [
      { label: "Progreso de implementación", value: p.pct === null ? "—" : p.pct + "%", sub: p.hasImpl ? p.done + " de " + p.total + " tareas de implementación" : "sin tareas de implementación registradas", cls: "accent", pct: p.pct },
      { label: "Completadas", value: String(c.COMPLETADO || 0), sub: "de " + p.list.length + " tareas de la versión", cls: "good", pct: p.list.length ? Math.round((c.COMPLETADO || 0) * 100 / p.list.length) : 0 },
      { label: "En progreso", value: String(c.EN_PROGRESO || 0), sub: "tareas activas", cls: (c.EN_PROGRESO || 0) > 0 ? "warn" : "", pct: p.list.length ? Math.round((c.EN_PROGRESO || 0) * 100 / p.list.length) : 0 },
      { label: "Listas", value: String(c.LISTO || 0), sub: "listas para empezar", cls: "accent", pct: p.list.length ? Math.round((c.LISTO || 0) * 100 / p.list.length) : 0 },
      { label: "Pendientes", value: closed ? "—" : String(c.PENDIENTE || 0), sub: closed ? "versión cerrada sin trabajo pendiente" : "tareas PENDIENTE", cls: closed ? "" : "warn", pct: closed || !p.list.length ? 0 : Math.round((c.PENDIENTE || 0) * 100 / p.list.length) },
      { label: "Bloqueadas", value: String(c.BLOQUEADO || 0), sub: "requieren intervención", cls: (c.BLOQUEADO || 0) > 0 ? "bad" : "good", pct: p.list.length ? Math.round((c.BLOQUEADO || 0) * 100 / p.list.length) : 0 }
    ];
    var secondary = [
      '<span class="chip">Requieren decisión <strong style="color:var(--ink)">' + (c.REQUIERE_DECISION || 0) + "</strong></span>",
      '<span class="chip">Pospuestas <strong style="color:var(--ink)">' + (c.POSPUESTO || 0) + "</strong></span>",
      '<span class="chip">Total de tareas <strong style="color:var(--ink)">' + p.list.length + "</strong></span>",
      closed ? '<span class="chip">Versión cerrada: no cuenta como pendiente</span>' : ""
    ].join("");
    el.innerHTML = '<div class="grid cards-auto version-stats">' + cards.map(function (card) {
      return '<div class="card stat ' + card.cls + '">' +
        '<div class="label">' + esc(card.label) + "</div>" +
        '<div class="value">' + esc(card.value) + "</div>" +
        '<div class="sub">' + esc(card.sub) + "</div>" +
        (card.pct === null ? "" : '<div class="progress"><span style="width:' + Math.max(0, Math.min(100, card.pct)) + '%"></span></div>') +
        "</div>";
    }).join("") + "</div>" +
    '<div class="chips version-secondary">' + secondary + "</div>" +
    '<div class="card distribution" id="version-distribution">' +
      distributionMarkup(p.list, "Distribución de la versión", "solo tareas de la versión seleccionada", true) +
    "</div>";
  }

  function renderVersionAlert(v) {
    var el = document.getElementById("version-alert");
    if (!el) return;
    var text = "";
    if (v.kind === "update") {
      if (v.status === "PROPUESTA" || v.status === "EN_REVISION" || v.status === "PENDIENTE_APROBACION") {
        text = "Esta versión es una propuesta y no trabajo en desarrollo. No se implementa hasta que el usuario la apruebe.";
      } else if (v.status === "BLOQUEADA") {
        text = "Esta versión está bloqueada. Consulta sus pendientes y dependencias antes de continuar.";
      } else if (v.status === "RECHAZADA" || v.status === "ARCHIVADA") {
        text = "Esta versión está cerrada (" + versionStatusInfo(v).label + "). Se conserva como antecedente y no contabiliza tareas pendientes.";
      }
    }
    el.innerHTML = text ? '<div class="update-note alert">' + esc(text) + "</div>" : "";
  }

  function sessionCardHtml(s, opts) {
    opts = opts || {};
    var isCurrent = s.id === meta.lastSessionId;
    var files = (s.files || []).map(function (f) { return "<li class=\"mono\">" + esc(f) + "</li>"; }).join("");
    var tests = (s.tests || []).map(function (x) { return "<li>" + esc(x) + "</li>"; }).join("");
    var versions = opts.showVersions
      ? sessionVersionLabels(s).map(function (v) {
          return '<button type="button" class="chip mono" data-select-version="' + esc(v.id) + '" title="Abrir versión ' + esc(v.displayVersion) + '">v' + esc(v.displayVersion) + " · " + esc(v.internalId) + "</button>";
        }).join("")
      : "";
    return '<div class="session' + (isCurrent ? " current" : "") + '">' +
      '<div class="session-top"><h3>' + esc(s.id) + " · " + esc(s.title || "Sesión") + "</h3>" +
      '<span class="date">' + esc(fmtDate(s.date)) + "</span></div>" +
      '<div class="chips" style="margin:7px 0">' + outcomeBadge(s.outcome) +
      (isCurrent ? '<span class="badge s-COMPLETADO">Última sesión</span>' : "") +
      versions +
      (s.document ? '<a class="chip" href="../' + esc(String(s.document).replace(/^docs\//, "")) + '" target="_blank" rel="noopener">Registro</a>' : "") +
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
  }

  function renderVersionHistory(v) {
    var el = document.getElementById("version-history");
    if (!el) return;
    var list = versionSessions(v).slice().sort(function (a, b) {
      return String(b.date || "").localeCompare(String(a.date || ""));
    });
    var implLog = v.kind === "update" ? ((v.update && v.update.implementationLog) || []) : [];
    var logHtml = implLog.length
      ? '<div class="card version-log"><h3>Registro de implementación de la actualización</h3><ul class="log-list">' +
        implLog.map(function (l) {
          var st = l.status && STATUS[l.status] ? statusBadge(l.status) + " " : "";
          return '<li class="log-item"><span class="mono">' + esc(l.sessionId || "—") + "</span> · " +
            esc(fmtDate(l.date)) + " · " + esc(l.agent || "") + " · " + st + esc(l.summary || "") +
            ((l.files || []).length ? '<div class="chips" style="margin-top:4px">' + l.files.map(function (f) { return '<span class="chip mono">' + esc(f) + "</span>"; }).join("") + "</div>" : "") +
            "</li>";
        }).join("") + "</ul></div>"
      : "";
    if (!list.length) {
      el.innerHTML = logHtml + '<div class="empty">No hay sesiones registradas directamente para esta versión.</div>';
      return;
    }
    var limit = 8;
    var shown = state.versionHistoryAll ? list : list.slice(0, limit);
    el.innerHTML = logHtml +
      '<div class="history">' + shown.map(function (s) { return sessionCardHtml(s, { showVersions: false }); }).join("") + "</div>" +
      (list.length > limit
        ? '<div class="history-more"><button type="button" class="btn ghost" data-version-history-toggle>' +
          (state.versionHistoryAll ? "Mostrar solo las últimas " + limit : "Mostrar todas (" + list.length + ")") +
          "</button></div>"
        : "");
  }

  function renderVersionPending(v) {
    var el = document.getElementById("version-pending");
    if (!el) return;
    var list = versionTaskList(v);
    var pending = list.filter(function (t) {
      return t.status === "BLOQUEADO" || t.status === "REQUIERE_DECISION" || t.status === "POSPUESTO" || pendingDecisionIds(t).length > 0;
    });
    var pendingDecisions = relatedDecisions(list).filter(function (d) { return d.status === "PENDIENTE"; });
    var rows = pending.map(function (t) {
      var extra = pendingDecisionIds(t).length ? " · Decisión: " + pendingDecisionIds(t).join(", ") : "";
      return '<div class="blocked-row">' +
        '<div class="title"><strong>' + esc(t.id) + " · " + esc(t.title) + "</strong><small>" + esc(t.technicalNotes || "Sin observaciones") + esc(extra) + "</small></div>" +
        '<div class="row-right">' + statusBadge(t.status) +
        '<button type="button" class="btn ghost small" data-focus-task="' + esc(t.id) + '">Abrir</button></div>' +
        "</div>";
    }).join("");
    var decisions = pendingDecisions.map(function (d) {
      return '<div class="blocked-row"><div class="title"><strong>' + esc(d.id) + " · " + esc(d.title) + "</strong><small>" + esc(d.recommendation || "") + "</small></div>" +
        '<div class="row-right">' + decisionBadge(d.status) + "</div></div>";
    }).join("");
    el.innerHTML = (rows || decisions)
      ? rows + decisions
      : '<div class="empty">No hay pendientes propios de esta versión.</div>';
  }

  function renderVersionDetails(v) {
    var el = document.getElementById("version-details");
    if (!el) return;
    if (v.kind === "base") {
      el.innerHTML = '<details class="card version-details">' +
        "<summary>Ficha del plan inicial</summary>" +
        '<div class="version-details-body">' +
          '<p class="hint">Plan histórico. No se reabre; las correcciones y mejoras posteriores se registran como actualizaciones.</p>' +
          '<div class="meta-grid">' +
            '<div class="meta-block"><h4>Etapas</h4><p style="margin:0;color:var(--muted);font-size:13px">' + stages.length + " etapas (S0–S11)</p></div>" +
            '<div class="meta-block"><h4>Tareas</h4><p style="margin:0;color:var(--muted);font-size:13px">' + tasks.length + " tareas registradas</p></div>" +
            '<div class="meta-block" style="grid-column:1/-1"><h4>Definición de progreso</h4><p style="margin:0;color:var(--muted);font-size:13px">' + esc(meta.progressDefinition || MISSING) + "</p></div>" +
          "</div>" +
        "</div>" +
      "</details>";
      return;
    }
    var u = v.update;
    var approval = u.approval || {};
    var systems = (u.affectedSystems || []).map(function (id) {
      var s = systemById[id];
      return '<span class="chip">' + esc(id) + (s ? " · " + esc(s.name) : " · no existe") + "</span>";
    }).join("") || '<span class="chip">Sin sistemas registrados</span>';
    var features = (u.affectedFeatures || []).map(function (id) {
      var f = featureById[id];
      return '<span class="chip">' + esc(id) + (f ? " · " + esc(f.name) : " · no existe") + "</span>";
    }).join("") || '<span class="chip">Sin funcionalidades registradas</span>';
    var stagesRef = (u.relatedStages || []).map(function (id) {
      var s = stageById[id];
      return '<span class="chip">' + esc(id) + (s ? " · " + esc(s.name) : " · no existe") + "</span>";
    }).join("") || '<span class="chip">Ninguna</span>';
    var tasksRef = (u.relatedTasks || []).map(function (id) {
      return '<button type="button" class="chip mono" data-focus-task="' + esc(id) + '" title="Abrir tarea histórica ' + esc(id) + '">' + esc(id) + "</button>";
    }).join("") || '<span class="chip">Ninguna</span>';
    var updatesRef = (u.relatedUpdates || []).map(function (id) {
      return '<button type="button" class="chip mono" data-select-version="' + esc(id) + '" title="Abrir ' + esc(id) + '">' + esc(id) + "</button>";
    }).join("") || '<span class="chip">Ninguna</span>';
    var deps = (u.dependencies || []).map(function (id) {
      var d = updateById[id];
      return '<span class="chip ' + (d && (d.status === "COMPLETADA" || d.status === "APROBADA") ? "ok" : "no") + '">' + esc(id) +
        (d ? " · " + esc(d.name) + " · " + esc(updateStatusInfo(d.status).label) : " · no existe") + "</span>";
    }).join("") || '<span class="chip ok">Sin dependencias</span>';
    var acceptance = (u.acceptanceCriteria || []).map(function (x) { return "<li>" + esc(x) + "</li>"; }).join("");
    var observations = (u.observations || []).map(function (x) { return "<li>" + esc(x) + "</li>"; }).join("");
    var exports = (u.reviewExports || []).map(function (x) {
      return '<span class="chip">' + esc(x.name || x.id || "Expediente") + " · " + esc(fmtDateShort(x.date)) + "</span>";
    }).join("") || '<span class="chip">Ninguno persistido. La exportación se descarga desde el navegador.</span>';
    var docHref = u.document ? "../" + u.document.replace(/^docs\//, "") : "";
    el.innerHTML = '<details class="card version-details">' +
      "<summary>Ficha completa de la actualización</summary>" +
      '<div class="version-details-body">' +
        '<div class="meta-grid">' +
          '<div class="meta-block"><h4>Aprobación</h4><p style="margin:0;color:var(--muted);font-size:13px">' +
            (approval.status === "APROBADA"
              ? "Aprobada por " + esc(approval.approvedBy || "—") + " · " + esc(fmtDate(approval.approvedAt)) + (approval.basis ? " · " + esc(approval.basis) : "")
              : "Aprobación pendiente. No se implementa hasta que el usuario la autorice.") + "</p></div>" +
          '<div class="meta-block"><h4>Tipo, prioridad y fechas</h4><p style="margin:0;color:var(--muted);font-size:13px">' +
            esc(updateTypeLabel(u.changeType)) + " · " + esc(PRIORITY_LABEL[u.priority] || u.priority || "—") +
            " · Creada " + esc(fmtDate(u.createdAt)) + " · Actualizada " + esc(fmtDate(u.updatedAt)) + "</p></div>" +
          '<div class="meta-block"><h4>Sistemas afectados</h4><div class="chips">' + systems + "</div></div>" +
          '<div class="meta-block"><h4>Funcionalidades relacionadas</h4><div class="chips">' + features + "</div></div>" +
          '<div class="meta-block"><h4>Etapas históricas relacionadas</h4><div class="chips">' + stagesRef + "</div></div>" +
          '<div class="meta-block"><h4>Tareas históricas relacionadas</h4><div class="chips">' + tasksRef + "</div></div>" +
          '<div class="meta-block"><h4>Actualizaciones relacionadas</h4><div class="chips">' + updatesRef + "</div></div>" +
          '<div class="meta-block"><h4>Dependencias</h4><div class="chips">' + deps + "</div></div>" +
          '<div class="meta-block" style="grid-column:1/-1"><h4>Documento</h4><p style="margin:0;color:var(--muted);font-size:13px">' +
            (docHref ? '<a href="' + esc(docHref) + '" target="_blank" rel="noopener">' + esc(u.document) + "</a>" : MISSING) + "</p></div>" +
        "</div>" +
        '<div class="meta-grid" style="margin-top:12px">' +
          '<div class="meta-block"><h4>Criterios de aceptación</h4><ul>' + (acceptance || "<li>Sin criterios registrados.</li>") + "</ul></div>" +
          '<div class="meta-block"><h4>Observaciones</h4><ul>' + (observations || "<li>Sin observaciones.</li>") + "</ul></div>" +
        "</div>" +
        '<div class="meta-block" style="margin-top:12px"><h4>Expedientes de revisión</h4><div class="chips">' + exports + "</div></div>" +
      "</div>" +
    "</details>";
  }

  function renderVersionContent() {
    var v = selectedVersion();
    var hint = document.getElementById("version-content-hint");
    if (hint) hint.textContent = "Solo se muestra la versión seleccionada · v" + v.displayVersion + " · " + v.internalId;
    renderVersionHero(v);
    renderVersionAlert(v);
    renderVersionProgress(v);
    renderVersionGroups(v);
    populateFilters();
    restoreFilters();
    renderTasks();
    renderVersionHistory(v);
    renderVersionPending(v);
    renderVersionDetails(v);
  }

  function renderPendingApprovals() {
    var el = document.getElementById("pending-approvals");
    if (!el) return;
    var list = updates.filter(function (u) {
      return u.status === "PROPUESTA" || u.status === "EN_REVISION" || u.status === "PENDIENTE_APROBACION";
    });
    if (!list.length) {
      el.innerHTML = '<div class="empty">No hay actualizaciones pendientes de aprobación.</div>';
      return;
    }
    el.innerHTML = list.map(function (u) {
      var p = updateProgress(u);
      return '<div class="blocked-row">' +
        '<div class="title"><strong>v' + esc(u.displayVersion || fallbackDisplayVersion(u)) + " · " + esc(u.id) + " · " + esc(u.name) + "</strong><small>" +
          esc(updateStatusInfo(u.status).label) + " · " + esc(updateTypeLabel(u.changeType)) + " · " + p.done + "/" + p.total + " tareas de implementación</small></div>" +
        '<div class="row-right">' + updateStatusBadge(u.status) +
        '<button type="button" class="btn ghost small" data-select-version="' + esc(u.id) + '">Abrir versión</button></div>' +
        "</div>";
    }).join("");
  }

  function renderGlobalHistory() {
    var el = document.getElementById("global-history-list");
    if (!el) return;
    var sorted = sessions.slice().sort(function (a, b) {
      return String(b.date || "").localeCompare(String(a.date || ""));
    }).slice(0, 6);
    el.innerHTML = sorted.length
      ? sorted.map(function (s) { return sessionCardHtml(s, { showVersions: true }); }).join("")
      : '<div class="empty">No hay sesiones registradas.</div>';
  }

  function renderGlobalSections() {
    renderSummary();
    renderDistribution();
    renderCurrent();
    renderVerification();
    renderPendingApprovals();
    renderDecisions();
    renderGlobalHistory();
    renderRules();
  }

  function setSelectValue(el, value, fallback) {
    if (!el) return;
    el.value = value;
    if (el.value !== value) el.value = fallback;
  }

  function populateFilters() {
    var v = selectedVersion();
    var list = versionTaskList(v);
    var c = {};
    list.forEach(function (t) { c[t.status] = (c[t.status] || 0) + 1; });
    var statusSel = document.getElementById("status-filter");
    statusSel.innerHTML = '<option value="ALL">Todos los estados (' + list.length + ")</option>" +
      STATUS_ORDER.map(function (k) {
        return '<option value="' + k + '">' + esc(STATUS[k].label) + " (" + (c[k] || 0) + ")</option>";
      }).join("");

    var stageSel = document.getElementById("stage-filter");
    var groups = versionGroupList(v);
    var groupCounts = {};
    list.forEach(function (t) {
      var group = versionGroupForTask(v, t.id);
      if (group) groupCounts[group.id] = (groupCounts[group.id] || 0) + 1;
    });
    if (groups.length) {
      stageSel.disabled = false;
      stageSel.innerHTML = '<option value="ALL">' + (v.kind === "base" ? "Todas las etapas" : "Todos los grupos") + " (" + list.length + ")</option>" +
        groups.map(function (group) {
          return '<option value="' + esc(group.id) + '">' + esc(group.id) + " · " + esc(group.name) + " (" + (groupCounts[group.id] || 0) + ")</option>";
        }).join("");
    } else {
      stageSel.disabled = true;
      stageSel.innerHTML = '<option value="ALL">Sin etapas o grupos propios</option>';
    }

    var prioSel = document.getElementById("priority-filter");
    var priorityCounts = {};
    list.forEach(function (t) { if (t.priority) priorityCounts[t.priority] = (priorityCounts[t.priority] || 0) + 1; });
    var priorities = Object.keys(priorityCounts).sort(function (a, b) {
      return (PRIORITY_ORDER[a] === undefined ? 99 : PRIORITY_ORDER[a]) - (PRIORITY_ORDER[b] === undefined ? 99 : PRIORITY_ORDER[b]);
    });
    prioSel.innerHTML = '<option value="ALL">Todas las prioridades</option>' +
      priorities.map(function (p) {
        return '<option value="' + esc(p) + '">' + esc(PRIORITY_LABEL[p] || p) + " (" + priorityCounts[p] + ")</option>";
      }).join("");
  }

  function restoreFilters() {
    var v = selectedVersion();
    var groups = versionGroupList(v);
    if (!groups.length) {
      state.stage = "ALL";
    } else if (state.stage !== "ALL" && !groups.some(function (g) { return g.id === state.stage; })) {
      state.stage = "ALL";
    }
    document.getElementById("search").value = state.search || "";
    setSelectValue(document.getElementById("status-filter"), state.status || "ALL", "ALL");
    var stageSel = document.getElementById("stage-filter");
    if (stageSel) {
      stageSel.disabled = !groups.length;
      setSelectValue(stageSel, state.stage || "ALL", "ALL");
    }
    setSelectValue(document.getElementById("priority-filter"), state.priority || "ALL", "ALL");
    setSelectValue(document.getElementById("sort-filter"), state.sort || "stage", "stage");
    var group = document.getElementById("group-toggle");
    if (group) {
      group.disabled = !groups.length;
      group.checked = groups.length > 0 && state.groupByStage !== false;
    }
  }

  function filterTasks() {
    var v = selectedVersion();
    var q = (state.search || "").trim().toLowerCase();
    var tokens = q ? q.split(/\s+/).filter(Boolean) : [];
    return versionTaskList(v).filter(function (t) {
      if (state.status !== "ALL" && t.status !== state.status) return false;
      if (state.stage !== "ALL") {
        var group = versionGroupForTask(v, t.id);
        if (!group || group.id !== state.stage) return false;
      }
      if (state.priority !== "ALL" && t.priority !== state.priority) return false;
      if (!tokens.length) return true;
      var hay = [
        t.id, t.title, t.description, t.stageId, t.updateId, taskContextLabel(t, v),
        t.status, t.priority, t.category, t.createdAt, t.updatedAt,
        (t.files || []).join(" "),
        (t.contextFiles || []).join(" "),
        (t.contextDocs || []).join(" "),
        (t.artifacts || []).join(" "),
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
    var v = selectedVersion();
    var groups = versionGroupList(v);
    var groupOrder = {};
    groups.forEach(function (g, i) { groupOrder[g.id] = i; });
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
      var ga = versionGroupForTask(v, a.id);
      var gb = versionGroupForTask(v, b.id);
      var oa = ga && groupOrder[ga.id] !== undefined ? groupOrder[ga.id] : 99;
      var ob = gb && groupOrder[gb.id] !== undefined ? groupOrder[gb.id] : 99;
      return oa !== ob ? oa - ob : natCompare(a.id, b.id);
    });
    return list;
  }

  function taskHtml(t, v) {
    v = v || selectedVersion();
    var context = taskContextLabel(t, v);
    var open = state.open[t.id] ? " open" : "";
    var deps = t.dependencies || [];
    var depsDone = deps.filter(function (id) {
      var d = taskById[id];
      return d && d.status === "COMPLETADO";
    }).length;
    var depsText = deps.length ? depsDone + "/" + deps.length + " deps" : "sin deps";
    var evidenceCount = (t.evidence || []).length;
    var pending = pendingDecisionIds(t);
    var isNext = v.kind === "base" && meta.nextTaskId === t.id;

    var depChips = deps.map(function (id) {
      var d = taskById[id];
      var ok = d && d.status === "COMPLETADO";
      return '<span class="chip ' + (ok ? "ok" : "no") + '"><span class="mono">' + esc(id) + "</span>" +
        (d ? " · " + esc(d.title) : " · no existe") + "</span>";
    }).join("");
    if (!depChips) depChips = '<span class="chip ok">Sin dependencias</span>';

    var related = (t.relatedTaskIds || []).map(function (id) {
      var d = taskById[id];
      return '<button type="button" class="chip mono" data-focus-task="' + esc(id) + '" title="Abrir tarea histórica ' + esc(id) + '">' + esc(id) + "</button>" +
        (d ? '<span class="chip">' + esc(d.title) + "</span>" : "");
    }).join("") || '<span class="chip">Ninguna</span>';

    var decisionChips = (t.decisionIds || []).map(function (id) {
      var d = decisionById[id];
      return '<span class="chip">' + esc(id) + (d ? " · " + esc(DECISION_LABEL[d.status] || d.status) : " · no existe") + "</span>";
    }).join("");
    if (!decisionChips) decisionChips = '<span class="chip">Ninguna</span>';

    var acceptance = (t.acceptanceCriteria || []).map(function (x) { return "<li>" + esc(x) + "</li>"; }).join("");
    var tests = (t.tests || []).map(function (x) { return "<li>" + esc(x) + "</li>"; }).join("");
    var files = (t.files || []).map(function (x) { return '<span class="chip mono">' + esc(x) + "</span>"; }).join("");
    var contextFiles = (t.contextFiles || []).map(function (x) { return '<span class="chip mono">' + esc(x) + "</span>"; }).join("");
    var contextDocs = (t.contextDocs || []).map(function (x) { return '<span class="chip">' + esc(x) + "</span>"; }).join("");
    var artifacts = (t.artifacts || []).map(function (x) { return '<span class="chip mono">' + esc(x) + "</span>"; }).join("");
    var assignment = (t.assignedSessionId || t.assignedAt)
      ? '<div class="meta-block" style="grid-column:1/-1"><h4>Responsable de la tarea</h4><div class="chips">' +
        (t.assignedSessionId ? '<span class="chip mono">' + esc(t.assignedSessionId) + "</span>" : '<span class="chip">Sin sesión registrada</span>') +
        (t.assignedAt ? '<span class="chip">' + esc(fmtDate(t.assignedAt)) + "</span>" : "") + "</div></div>"
      : "";
    var contextChips = (contextFiles || contextDocs)
      ? (contextFiles ? "<div><strong>Código:</strong> " + contextFiles + "</div>" : "") +
        (contextDocs ? "<div><strong>Docs:</strong> " + contextDocs + "</div>" : "")
      : "";
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
          esc(context) + " · " + esc(depsText) +
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
          '<div class="meta-block"><h4>Tareas históricas relacionadas</h4><div class="chips">' + related + "</div></div>" +
          '<div class="meta-block"><h4>Decisiones</h4><div class="chips">' + decisionChips + "</div></div>" +
          '<div class="meta-block"><h4>Criterios de aceptación</h4><ul>' + (acceptance || "<li>Sin criterios registrados.</li>") + "</ul></div>" +
          '<div class="meta-block"><h4>Pruebas necesarias</h4><ul>' + (tests || "<li>Sin pruebas registradas.</li>") + "</ul></div>" +
          '<div class="meta-block" style="grid-column:1/-1"><h4>Archivos / sistemas</h4><div class="chips">' + (files || '<span class="chip">Sin archivos registrados</span>') + "</div></div>" +
          (contextChips ? '<div class="meta-block" style="grid-column:1/-1"><h4>Contexto previo</h4><div class="chips">' + contextChips + "</div></div>" : "") +
          (artifacts ? '<div class="meta-block" style="grid-column:1/-1"><h4>Artefactos de implementación</h4><div class="chips">' + artifacts + "</div></div>" : "") +
          assignment +
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
    var v = selectedVersion();
    var list = sortTasks(filterTasks());
    var el = document.getElementById("task-list");
    var total = versionTaskList(v).length;
    document.getElementById("task-count").textContent = list.length + " de " + total + " tareas de v" + v.displayVersion;
    if (!list.length) {
      el.innerHTML = '<div class="empty">No hay tareas que coincidan con los filtros.<br>' +
        '<button type="button" class="btn ghost" data-clear-filters>Limpiar filtros</button></div>';
      return;
    }

    var groups = versionGroupList(v);
    var canGroup = groups.length > 0;
    var groupToggle = document.getElementById("group-toggle");
    if (groupToggle) {
      groupToggle.disabled = !canGroup;
      groupToggle.checked = canGroup && state.groupByStage !== false;
    }

    if (!canGroup || state.groupByStage === false) {
      el.innerHTML = list.map(function (t) { return taskHtml(t, v); }).join("");
    } else {
      var byGroup = {};
      var order = [];
      list.forEach(function (t) {
        var group = versionGroupForTask(v, t.id);
        var gid = group ? group.id : "__none";
        if (!byGroup[gid]) {
          byGroup[gid] = [];
          order.push(gid);
        }
        byGroup[gid].push(t);
      });
      var groupOrder = {};
      groups.forEach(function (g, i) { groupOrder[g.id] = i; });
      order.sort(function (a, b) {
        var oa = groupOrder[a] === undefined ? 99 : groupOrder[a];
        var ob = groupOrder[b] === undefined ? 99 : groupOrder[b];
        return oa !== ob ? oa - ob : natCompare(a, b);
      });

      el.innerHTML = order.map(function (gid) {
        var group = null;
        for (var i = 0; i < groups.length; i += 1) {
          if (groups[i].id === gid) { group = groups[i]; break; }
        }
        var items = byGroup[gid];
        var gp = group ? group.progress : groupProgressFromTasks(items, "PENDIENTE");
        var isCurrent = v.kind === "base" && gid === meta.currentStageId;
        var jump = group ? ' data-stage-jump="' + esc(gid) + '" role="button" tabindex="0" aria-label="Filtrar solo ' + esc(gid) + '"' : "";
        return '<div class="stage-group">' +
          '<div class="stage-group-head"' + jump + ">" +
            "<h3>" + esc(group ? group.id + " · " + group.name : "Sin grupo") + (isCurrent ? ' <span class="badge s-LISTO">Actual</span>' : "") + "</h3>" +
            '<div class="group-right"><span class="group-meta">' + items.length + " visibles · " +
              esc(groupProgressText(group || { progress: gp, type: "implementation" })) +
            "</span>" + statusBadge(gp.status) + "</div>" +
          "</div>" +
          items.map(function (t) { return taskHtml(t, v); }).join("") +
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

    var selected = selectedVersion();
    var selectedIds = {};
    versionTaskList(selected).forEach(function (t) { selectedIds[t.id] = true; });
    var blocked = allProjectTasks().filter(function (t) {
      if (selectedIds[t.id]) return false;
      return t.status === "BLOQUEADO" || t.status === "REQUIERE_DECISION" || t.status === "POSPUESTO" || pendingDecisionIds(t).length > 0;
    });
    var blockedEl = document.getElementById("blocked-list");
    var blockedHint = document.getElementById("blocked-hint");
    if (blockedHint) {
      blockedHint.textContent = "Tareas bloqueadas, con decisión o pospuestas fuera de la versión seleccionada. Las de la versión actual aparecen arriba.";
    }
    if (!blocked.length) {
      blockedEl.innerHTML = '<div class="empty">No hay tareas bloqueadas, con decisión pendiente ni pospuestas fuera de la versión seleccionada.</div>';
    } else {
      blockedEl.innerHTML = blocked.map(function (t) {
        var extra = pendingDecisionIds(t).length ? " · Decisión: " + pendingDecisionIds(t).join(", ") : "";
        var v = versionById(taskVersionId(t));
        return '<div class="blocked-row">' +
          '<div class="title"><strong>' + esc(t.id) + " · " + esc(t.title) + "</strong><small>" +
            (v ? "v" + esc(v.displayVersion) + " · " + esc(v.internalId) + " · " : "") +
            esc(t.technicalNotes || "Sin observaciones") + esc(extra) + "</small></div>" +
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
      (defs.length ? '<h4 style="margin:14px 0 0;font-size:13px">Estados de tarea</h4><div class="rules-grid">' +
        defs.map(function (k) {
          var d = statusDefs[k] || {};
          return '<div class="rule-def">' + statusBadge(k) +
            "<p>" + esc(d.description || "Sin descripción.") + "</p></div>";
        }).join("") + "</div>" : "") +
      (Object.keys(updateStatusDefs).length ? '<h4 style="margin:14px 0 0;font-size:13px">Estados de actualización</h4><div class="rules-grid">' +
        Object.keys(UPDATE_STATUS).map(function (k) {
          var d = updateStatusDefs[k] || {};
          return '<div class="rule-def">' + updateStatusBadge(k) +
            "<p>" + esc(d.description || "Sin descripción.") + "</p></div>";
        }).join("") + "</div>" : "") +
      (meta.updateProgressDefinition ? '<p style="margin-top:12px"><strong>Progreso de actualizaciones:</strong> ' + esc(meta.updateProgressDefinition) + "</p>" : "") +
      '<p class="hint" style="margin-top:12px">' + esc(meta.statusSource || "") +
      (meta.oneTaskPerSession ? " · Una tarea por sesión." : "") + "</p>";
  }

  function renderAll() {
    ensureVersionSelection();
    var v = selectedVersion();
    document.getElementById("project-name").textContent = meta.displayName || meta.project || "GalacticIndustry";
    document.getElementById("project-description").textContent = meta.description || "";
    document.getElementById("last-updated").textContent = fmtDate(meta.lastUpdated);
    document.getElementById("last-session").textContent = meta.lastSessionId || "—";
    var currentStage = stageById[meta.currentStageId];
    document.getElementById("top-stage").textContent = currentStage ? currentStage.id + " · " + currentStage.name : "—";
    var cu = currentUpdate();
    var cuEl = document.getElementById("top-update");
    if (cuEl) cuEl.textContent = cu ? "v" + (cu.displayVersion || fallbackDisplayVersion(cu)) + " · " + cu.id + " · " + updateStatusInfo(cu.status).label : (updates.length ? updates.length + " registradas" : "—");
    var topVersion = document.getElementById("top-version");
    if (topVersion) topVersion.textContent = "v" + v.displayVersion + " · " + v.internalId;
    var p = taskProgress();
    document.getElementById("top-progress").textContent = p.pct + "% (" + p.done + "/" + p.total + ")";
    document.title = "Panel — " + (meta.displayName || "GalacticIndustry") + " · v" + v.displayVersion + " " + v.internalId;

    renderVersionSelector();
    renderTimeline();
    renderVersionContent();
    renderGlobalSections();
    document.getElementById("view-time").textContent = fmtDate(new Date().toISOString());
  }

  function pushHash(hash) {
    if (window.location.hash === hash) return;
    try {
      history.pushState(null, "", hash);
    } catch (e) {
      window.location.hash = hash.replace(/^#/, "");
    }
  }

  function selectVersion(id, opts) {
    opts = opts || {};
    var v = versionById(id);
    if (!v) return;
    state.versionId = v.id;
    state.open = {};
    state.search = "";
    state.status = "ALL";
    state.stage = "ALL";
    state.priority = "ALL";
    state.versionHistoryAll = false;
    ensureVersionSelection();
    renderVersionSelector();
    renderTimeline();
    renderVersionContent();
    renderGlobalSections();
    var topVersion = document.getElementById("top-version");
    if (topVersion) topVersion.textContent = "v" + v.displayVersion + " · " + v.internalId;
    document.title = "Panel — " + (meta.displayName || "GalacticIndustry") + " · v" + v.displayVersion + " " + v.internalId;
    if (opts.updateHash !== false) pushHash("#version-" + v.id);
    if (opts.scroll !== false) {
      setTimeout(function () { scrollToElement(document.getElementById("version-contenido"), "start"); }, 20);
    }
    saveUiState();
    if (!opts.silent) toast("Versión v" + v.displayVersion + " · " + v.internalId + " seleccionada");
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
    scrollToElement(document.getElementById("version-tareas"), "start");
  }

  function focusTask(id, opts) {
    opts = opts || {};
    var t = taskById[id];
    if (!t) return;
    var vid = taskVersionId(t);
    if (selectedVersion().id !== vid) {
      selectVersion(vid, { updateHash: false, scroll: false, silent: true });
    }
    state.search = "";
    state.status = "ALL";
    state.stage = "ALL";
    state.priority = "ALL";
    state.open = {};
    state.open[id] = true;
    restoreFilters();
    renderTasks();
    if (opts.updateHash !== false) pushHash("#task-" + id);
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

  function focusUpdate(id, opts) {
    opts = opts || {};
    if (!versionById(id)) return;
    selectVersion(id, { updateHash: opts.updateHash !== false, scroll: opts.scroll !== false, silent: opts.silent });
  }

  function handleHash() {
    var hash = window.location.hash || "";
    var m = /^#version-(.+)$/.exec(hash);
    if (m) {
      var vid = decodeURIComponent(m[1]);
      if (versionById(vid)) selectVersion(vid, { updateHash: false, silent: true });
      return;
    }
    m = /^#update-plan-base$/.exec(hash);
    if (m) {
      selectVersion("BASE", { updateHash: false, silent: true });
      return;
    }
    m = /^#update-(.+)$/.exec(hash);
    if (m) {
      var uid = decodeURIComponent(m[1]);
      if (versionById(uid)) {
        selectVersion(uid, { updateHash: false, silent: true });
        return;
      }
    }
    m = /^#update-task-(.+)$/.exec(hash);
    if (m) {
      var utid = decodeURIComponent(m[1]);
      if (taskById[utid]) focusTask(utid, { updateHash: false, silent: true });
      return;
    }
    m = /^#task-(.+)$/.exec(hash);
    if (m) {
      var id = decodeURIComponent(m[1]);
      if (taskById[id]) focusTask(id, { updateHash: false, silent: true });
    }
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
    state.versionMode = loadVersionMode();
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
          state.versionHistoryAll = parsed.versionHistoryAll === true;
          state.hideResolved = parsed.hideResolved === true;
          state.open = parsed.open || {};
          // La versión seleccionada se deriva del tracker y del hash, no del
          // estado guardado: al abrir de nuevo debe mandar meta.currentUpdateId.
        }
      }
      var scroll = sessionStorage.getItem("gi-panel-scroll");
      if (scroll) {
        setTimeout(function () { window.scrollTo(0, parseInt(scroll, 10) || 0); }, 30);
      }
    } catch (e) { /* ignorar */ }
  }

  /* ---------------------------------------------------------------------
     Actualizaciones: propuestas y expedientes de revisión
     --------------------------------------------------------------------- */

  var REPEAT = function (ch, n) { return new Array(n + 1).join(ch); };

  function docHeading(fmt, level, text) {
    if (fmt === "txt") {
      if (level === 1) return text.toUpperCase() + "\n" + REPEAT("=", Math.max(4, text.length));
      if (level === 2) return text + "\n" + REPEAT("-", Math.max(4, text.length));
      return text;
    }
    return REPEAT("#", level) + " " + text;
  }

  function docList(fmt, items, emptyText) {
    var list = (items || []).filter(function (x) { return x !== null && x !== undefined && String(x).trim() !== ""; });
    if (!list.length) return "- " + (emptyText || MISSING);
    return list.map(function (x) { return "- " + x; }).join("\n");
  }

  function uniq(list) {
    var seen = {};
    return (list || []).filter(function (x) {
      var k = String(x);
      if (seen[k]) return false;
      seen[k] = true;
      return true;
    });
  }

  function downloadText(filename, text, mime) {
    try {
      var blob = new Blob([text], { type: mime || "text/markdown;charset=utf-8" });
      var url = URL.createObjectURL(blob);
      var a = document.createElement("a");
      a.href = url;
      a.download = filename;
      a.style.display = "none";
      document.body.appendChild(a);
      a.click();
      setTimeout(function () {
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      }, 120);
      toast("Descargado: " + filename);
    } catch (e) {
      toast("No se pudo descargar el documento: " + e.message);
    }
  }

  function copyText(text) {
    function fallback() {
      var ta = document.createElement("textarea");
      ta.value = text;
      ta.setAttribute("readonly", "");
      ta.style.position = "fixed";
      ta.style.left = "-9999px";
      document.body.appendChild(ta);
      ta.select();
      var ok = false;
      try { ok = document.execCommand("copy"); } catch (e) { ok = false; }
      document.body.removeChild(ta);
      toast(ok ? "JSON copiado al portapapeles" : "No se pudo copiar; usa la descarga .md/.txt");
    }
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(function () {
        toast("JSON copiado al portapapeles");
      }).catch(fallback);
    } else {
      fallback();
    }
  }

  function openModal(id) {
    var el = document.getElementById(id);
    if (!el) return;
    el.hidden = false;
    document.body.style.overflow = "hidden";
    var focus = el.querySelector("input, select, textarea, button");
    if (focus) setTimeout(function () { focus.focus(); }, 30);
  }

  function closeModal(id) {
    var el = document.getElementById(id);
    if (!el) return;
    el.hidden = true;
    if (!document.querySelector(".modal:not([hidden])")) document.body.style.overflow = "";
  }

  function closeAllModals() {
    Array.prototype.forEach.call(document.querySelectorAll(".modal:not([hidden])"), function (el) {
      el.hidden = true;
    });
    document.body.style.overflow = "";
  }

  function selectedIds(type) {
    var bucket = state.review[type] || {};
    return Object.keys(bucket).filter(function (id) { return bucket[id] === true; });
  }

  function reviewSelectionCount() {
    return ["updates", "stages", "tasks", "updateTasks", "systems", "features"].reduce(function (n, type) {
      return n + selectedIds(type).length;
    }, 0);
  }

  function tasksOfStage(stageId) {
    return tasks.filter(function (t) { return t.stageId === stageId; });
  }

  function updateTaskById(id) {
    var found = null;
    updates.forEach(function (u) {
      if (found) return;
      (u.tasks || []).forEach(function (t) { if (t.id === id) found = { update: u, task: t }; });
    });
    return found;
  }

  function relatedIssues(taskIds) {
    var set = {};
    (taskIds || []).forEach(function (id) { set[id] = true; });
    return issues.filter(function (i) {
      return (i.affectedTasks || []).some(function (id) { return set[id]; });
    });
  }

  function relatedDecisions(taskList) {
    var ids = {};
    (taskList || []).forEach(function (t) {
      (t.decisionIds || []).forEach(function (id) { ids[id] = true; });
    });
    return Object.keys(ids).map(function (id) { return decisionById[id] || { id: id, title: "No existe en el tracker", status: "—" }; });
  }

  function taskImplementationSummary(taskList) {
    var parts = [];
    (taskList || []).forEach(function (t) {
      (t.executionLog || []).forEach(function (l) {
        if (l.summary) parts.push(t.id + " (" + (l.sessionId || "sin sesión") + "): " + l.summary);
      });
    });
    return uniq(parts);
  }

  function taskEvidence(taskList) {
    var out = [];
    (taskList || []).forEach(function (t) {
      (t.evidence || []).forEach(function (e) { out.push(t.id + ": " + e); });
    });
    return uniq(out);
  }

  function taskFiles(taskList) {
    var out = [];
    (taskList || []).forEach(function (t) { out = out.concat(t.files || []); });
    return uniq(out);
  }

  function taskArtifacts(taskList) {
    var out = [];
    (taskList || []).forEach(function (t) { out = out.concat(t.artifacts || []); });
    return uniq(out);
  }

  function taskContextDocs(taskList) {
    var out = [];
    (taskList || []).forEach(function (t) {
      out = out.concat(t.contextFiles || []);
      out = out.concat(t.contextDocs || []);
    });
    return uniq(out);
  }

  function taskTests(taskList) {
    var out = [];
    (taskList || []).forEach(function (t) {
      (t.tests || []).forEach(function (x) { out.push(t.id + ": " + x); });
    });
    return uniq(out);
  }

  function taskCriteria(taskList) {
    var out = [];
    (taskList || []).forEach(function (t) {
      (t.acceptanceCriteria || []).forEach(function (x) { out.push(t.id + ": " + x); });
    });
    return uniq(out);
  }

  function taskNotes(taskList) {
    var out = [];
    (taskList || []).forEach(function (t) {
      if (t.technicalNotes) out.push(t.id + ": " + t.technicalNotes);
    });
    return uniq(out);
  }

  function taskDependencies(taskList) {
    var out = [];
    (taskList || []).forEach(function (t) {
      (t.dependencies || []).forEach(function (d) { out.push(t.id + " depende de " + d); });
      (t.relatedTaskIds || []).forEach(function (d) { out.push(t.id + " se relaciona con " + d); });
    });
    return uniq(out);
  }

  function docTaskBody(fmt, t, context) {
    var deps = (t.dependencies || []).concat(t.relatedTaskIds || []);
    var decisions = (t.decisionIds || []).map(function (id) {
      var d = decisionById[id];
      return id + (d ? " · " + d.title + " · " + d.status + (d.decision ? ": " + d.decision : "") : " · no existe");
    });
    var logLines = (t.executionLog || []).map(function (l) {
      return (l.sessionId || "—") + " · " + (l.date || "—") + " · " + (l.agent || "—") + " · " + (l.status || "—") + " · " + (l.summary || "");
    });
    var issueLines = relatedIssues([t.id]).map(function (i) {
      return i.id + " · " + i.title + " · " + i.status + " · " + (i.description || "");
    });
    return [
      docHeading(fmt, 2, t.id + " · " + t.title),
      docHeading(fmt, 3, "1. Identificación"),
      docList(fmt, [
        "ID: " + t.id,
        "Contexto: " + (context || MISSING),
        "Estado: " + statusInfo(t.status).label,
        "Prioridad: " + (PRIORITY_LABEL[t.priority] || t.priority || "—"),
        "Categoría: " + (t.category || "—")
      ]),
      docHeading(fmt, 3, "2. Funcionamiento previsto"),
      t.description || MISSING,
      docHeading(fmt, 3, "3. Estado de implementación"),
      "Estado registrado: " + statusInfo(t.status).label + ". " +
        ((t.executionLog || []).length ? "Ejecuciones registradas: " + (t.executionLog || []).length + "." : "No hay ejecuciones registradas."),
      docHeading(fmt, 3, "4. Resumen de lo implementado realmente"),
      docList(fmt, taskImplementationSummary([t]).concat(taskEvidence([t])), "No hay resumen de implementación ni evidencias registradas."),
      docHeading(fmt, 3, "5. Archivos y clases modificados"),
      docList(fmt, (t.files || []).concat(t.artifacts || []).concat(t.contextFiles || []).concat(t.contextDocs || []), "No hay archivos registrados para esta tarea."),
      docHeading(fmt, 3, "6. Pruebas realizadas y resultados"),
      docList(fmt, (t.tests || []).concat(logLines), "No hay pruebas ni resultados registrados."),
      docHeading(fmt, 3, "7. Criterios de aceptación"),
      docList(fmt, t.acceptanceCriteria || [], "No hay criterios de aceptación registrados."),
      docHeading(fmt, 3, "8. Problemas conocidos"),
      docList(fmt, issueLines, "No hay problemas registrados que afecten a esta tarea."),
      docHeading(fmt, 3, "9. Limitaciones"),
      t.technicalNotes || MISSING,
      docHeading(fmt, 3, "10. Dependencias"),
      docList(fmt, deps, "No hay dependencias registradas."),
      docHeading(fmt, 3, "11. Decisiones de diseño relacionadas"),
      docList(fmt, decisions, "No hay decisiones relacionadas registradas.")
    ].join("\n\n");
  }

  function docStageBody(fmt, stage) {
    var ts = tasksOfStage(stage.id);
    var sp = stageProgress(stage);
    var taskLines = ts.map(function (t) {
      return t.id + " · " + t.title + " · " + statusInfo(t.status).label;
    });
    var deps = (stage.dependencies || []).slice();
    return [
      docHeading(fmt, 2, stage.id + " · " + stage.name),
      docHeading(fmt, 3, "1. Identificación"),
      docList(fmt, [
        "ID: " + stage.id,
        "Tipo: " + (stage.type || "—"),
        "Estado: " + statusInfo(sp.status).label,
        "Progreso: " + sp.done + "/" + sp.implTotal + " tareas de implementación (" + sp.pct + "%)"
      ]),
      docHeading(fmt, 3, "2. Funcionamiento previsto"),
      stage.objective || MISSING,
      docHeading(fmt, 3, "3. Estado de implementación"),
      "Estado calculado a partir de sus tareas: " + statusInfo(sp.status).label + ". " +
        (stage.gate ? "Gate de la etapa: " + stage.gate : "No hay gate registrado."),
      docHeading(fmt, 3, "4. Resumen de lo implementado realmente"),
      docList(fmt, taskImplementationSummary(ts).concat(taskEvidence(ts)), "No hay resúmenes ni evidencias registradas para las tareas de la etapa."),
      docHeading(fmt, 3, "5. Archivos y clases modificados"),
      docList(fmt, taskFiles(ts).concat(taskArtifacts(ts)).concat(taskContextDocs(ts)), "No hay archivos registrados en las tareas de la etapa."),
      docHeading(fmt, 3, "6. Pruebas realizadas y resultados"),
      docList(fmt, taskTests(ts), "No hay pruebas registradas en las tareas de la etapa."),
      docHeading(fmt, 3, "7. Criterios de aceptación"),
      docList(fmt, taskCriteria(ts), "No hay criterios registrados en las tareas de la etapa."),
      docHeading(fmt, 3, "8. Problemas conocidos"),
      docList(fmt, relatedIssues(ts.map(function (t) { return t.id; })).map(function (i) {
        return i.id + " · " + i.title + " · " + i.status + " · " + (i.description || "");
      }), "No hay problemas registrados que afecten a la etapa."),
      docHeading(fmt, 3, "9. Limitaciones"),
      docList(fmt, taskNotes(ts), "No hay limitaciones documentadas en las tareas de la etapa."),
      docHeading(fmt, 3, "10. Dependencias"),
      docList(fmt, deps.concat(taskDependencies(ts)), "No hay dependencias registradas."),
      docHeading(fmt, 3, "11. Decisiones de diseño relacionadas"),
      docList(fmt, relatedDecisions(ts).map(function (d) {
        return d.id + " · " + d.title + " · " + d.status + (d.decision ? ": " + d.decision : "");
      }), "No hay decisiones relacionadas registradas."),
      docHeading(fmt, 3, "Tareas incluidas"),
      docList(fmt, taskLines, "La etapa no tiene tareas registradas.")
    ].join("\n\n");
  }

  function docCatalogBody(fmt, item, kind) {
    var ids = item.taskIds || [];
    var ts = ids.map(function (id) { return taskById[id]; }).filter(Boolean);
    var stagesList = (item.stageIds || []).map(function (id) {
      var s = stageById[id];
      return id + (s ? " · " + s.name : " · no existe");
    });
    var counts = {};
    ts.forEach(function (t) { counts[t.status] = (counts[t.status] || 0) + 1; });
    var statusLine = Object.keys(counts).map(function (k) { return statusInfo(k).label + ": " + counts[k]; }).join(" · ") || "Sin tareas asociadas.";
    return [
      docHeading(fmt, 2, item.id + " · " + item.name),
      docHeading(fmt, 3, "1. Identificación"),
      docList(fmt, [
        "ID: " + item.id,
        "Tipo de elemento: " + (kind === "system" ? "Sistema" : "Funcionalidad"),
        "Etapas: " + (stagesList.length ? stagesList.join(", ") : "—"),
        "Tareas asociadas: " + (ids.length ? ids.join(", ") : "—")
      ]),
      docHeading(fmt, 3, "2. Funcionamiento previsto"),
      item.description || MISSING,
      docHeading(fmt, 3, "3. Estado de implementación"),
      statusLine + ". Estado agregado de las tareas asociadas; no es un estado oficial del tracker.",
      docHeading(fmt, 3, "4. Resumen de lo implementado realmente"),
      docList(fmt, taskImplementationSummary(ts).concat(taskEvidence(ts)), "No hay resúmenes ni evidencias registradas."),
      docHeading(fmt, 3, "5. Archivos y clases modificados"),
      docList(fmt, taskFiles(ts).concat(taskArtifacts(ts)).concat(taskContextDocs(ts)), "No hay archivos registrados."),
      docHeading(fmt, 3, "6. Pruebas realizadas y resultados"),
      docList(fmt, taskTests(ts), "No hay pruebas registradas."),
      docHeading(fmt, 3, "7. Criterios de aceptación"),
      docList(fmt, taskCriteria(ts), "No hay criterios registrados."),
      docHeading(fmt, 3, "8. Problemas conocidos"),
      docList(fmt, relatedIssues(ids).map(function (i) {
        return i.id + " · " + i.title + " · " + i.status + " · " + (i.description || "");
      }), "No hay problemas registrados que afecten a este elemento."),
      docHeading(fmt, 3, "9. Limitaciones"),
      docList(fmt, taskNotes(ts), "No hay limitaciones documentadas."),
      docHeading(fmt, 3, "10. Dependencias"),
      docList(fmt, taskDependencies(ts), "No hay dependencias registradas."),
      docHeading(fmt, 3, "11. Decisiones de diseño relacionadas"),
      docList(fmt, relatedDecisions(ts).map(function (d) {
        return d.id + " · " + d.title + " · " + d.status + (d.decision ? ": " + d.decision : "");
      }), "No hay decisiones relacionadas registradas.")
    ].join("\n\n");
  }

  function docUpdateBody(fmt, u) {
    var ts = u.tasks || [];
    var taskLines = ts.map(function (t) { return t.id + " · " + t.title + " · " + statusInfo(t.status).label; });
    var approval = u.approval || {};
    return [
      docHeading(fmt, 2, u.id + " · " + u.name),
      docHeading(fmt, 3, "1. Identificación"),
      docList(fmt, [
        "ID: " + u.id,
        "Versión de presentación: " + (u.displayVersion || fallbackDisplayVersion(u)),
        "Estado de la actualización: " + updateStatusInfo(u.status).label,
        "Tipo de cambio: " + updateTypeLabel(u.changeType),
        "Prioridad: " + (PRIORITY_LABEL[u.priority] || u.priority || "—"),
        "Creada: " + (u.createdAt || "—"),
        "Actualizada: " + (u.updatedAt || "—"),
        "Documento: " + (u.document || "—"),
        "Aprobación: " + (approval.status || "—") + (approval.approvedBy ? " por " + approval.approvedBy : "") + (approval.approvedAt ? " el " + approval.approvedAt : "")
      ]),
      docHeading(fmt, 3, "2. Funcionamiento previsto"),
      "Objetivo: " + (u.objective || MISSING) + "\n\n" + (u.description || MISSING) + "\n\nMotivación: " + (u.motivation || MISSING),
      docHeading(fmt, 3, "3. Estado de implementación"),
      updateStatusInfo(u.status).label + ". Tareas: " + ts.length + ". " +
        (u.implementationLog || []).length + " entradas en el registro de implementación.",
      docHeading(fmt, 3, "4. Resumen de lo implementado realmente"),
      docList(fmt, (u.implementationLog || []).map(function (l) {
        return (l.sessionId || "—") + " · " + (l.date || "—") + " · " + (l.status || "—") + " · " + (l.summary || "");
      }).concat(taskImplementationSummary(ts)).concat(taskEvidence(ts)), "No hay implementación ni evidencias registradas."),
      docHeading(fmt, 3, "5. Archivos y clases modificados"),
      docList(fmt, taskFiles(ts).concat(taskArtifacts(ts)).concat(taskContextDocs(ts)), "No hay archivos registrados en las tareas de la actualización."),
      docHeading(fmt, 3, "6. Pruebas realizadas y resultados"),
      docList(fmt, ((u.implementationLog || []).reduce(function (out, l) { return out.concat(l.tests || []); }, [])).concat(taskTests(ts)), "No hay pruebas registradas."),
      docHeading(fmt, 3, "7. Criterios de aceptación"),
      docList(fmt, u.acceptanceCriteria || [], "No hay criterios de aceptación registrados."),
      docHeading(fmt, 3, "8. Problemas conocidos"),
      docList(fmt, relatedIssues(ts.map(function (t) { return t.id; })).map(function (i) {
        return i.id + " · " + i.title + " · " + i.status + " · " + (i.description || "");
      }), "No hay problemas registrados que afecten a esta actualización."),
      docHeading(fmt, 3, "9. Limitaciones"),
      docList(fmt, (u.observations || []).concat(taskNotes(ts)), "No hay limitaciones u observaciones registradas."),
      docHeading(fmt, 3, "10. Dependencias"),
      docList(fmt, (u.dependencies || []).map(function (id) {
        var d = updateById[id];
        return id + (d ? " · " + d.name + " · " + updateStatusInfo(d.status).label : " · no existe");
      }).concat((u.relatedTasks || []).map(function (id) { return "Tarea histórica relacionada: " + id; })), "No hay dependencias registradas."),
      docHeading(fmt, 3, "11. Decisiones de diseño relacionadas"),
      docList(fmt, relatedDecisions(ts).map(function (d) {
        return d.id + " · " + d.title + " · " + d.status + (d.decision ? ": " + d.decision : "");
      }), "No hay decisiones relacionadas registradas."),
      docHeading(fmt, 3, "Tareas incluidas"),
      docList(fmt, taskLines, "La actualización no tiene tareas registradas.")
    ].join("\n\n");
  }

  function buildReviewDocument(fmt) {
    var lines = [];
    var counts = {
      updates: selectedIds("updates").length,
      stages: selectedIds("stages").length,
      tasks: selectedIds("tasks").length,
      updateTasks: selectedIds("updateTasks").length,
      systems: selectedIds("systems").length,
      features: selectedIds("features").length
    };
    lines.push(docHeading(fmt, 1, "Expediente de revisión · " + (meta.displayName || "GalacticIndustry")));
    lines.push("Generado: " + new Date().toLocaleString("es-ES") + "\nFuente: IMPLEMENTATION_TRACKER.json (copia embebida del panel)\n" +
      "Este documento reúne datos registrados. Los campos ausentes se marcan como \"" + MISSING + "\"; no se han inventado detalles técnicos.");
    lines.push(docHeading(fmt, 2, "Selección"));
    lines.push(docList(fmt, [
      "Actualizaciones: " + counts.updates,
      "Etapas: " + counts.stages,
      "Tareas del plan base: " + counts.tasks,
      "Tareas de actualización: " + counts.updateTasks,
      "Sistemas: " + counts.systems,
      "Funcionalidades: " + counts.features
    ]));
    lines.push(docHeading(fmt, 2, "Cómo usar este expediente"));
    lines.push("Revisa cada elemento, contrasta las observaciones con el código real y prepara una propuesta complementaria. " +
      "No modifiques el plan maestro ni marques tareas históricas como reabiertas. Al final hay una sección para tus observaciones.");

    selectedIds("updates").forEach(function (id) {
      if (updateById[id]) lines.push(docUpdateBody(fmt, updateById[id]));
    });
    selectedIds("stages").forEach(function (id) {
      if (stageById[id]) lines.push(docStageBody(fmt, stageById[id]));
    });
    selectedIds("tasks").forEach(function (id) {
      if (taskById[id] && id.indexOf("UPD-") !== 0) lines.push(docTaskBody(fmt, taskById[id], "Plan base · " + taskById[id].stageId));
    });
    selectedIds("updateTasks").forEach(function (id) {
      var found = updateTaskById(id);
      if (found) lines.push(docTaskBody(fmt, found.task, "Actualización " + found.update.id));
    });
    selectedIds("systems").forEach(function (id) {
      if (systemById[id]) lines.push(docCatalogBody(fmt, systemById[id], "system"));
    });
    selectedIds("features").forEach(function (id) {
      if (featureById[id]) lines.push(docCatalogBody(fmt, featureById[id], "feature"));
    });

    lines.push(docHeading(fmt, 2, "OBSERVACIONES DEL USUARIO"));
    lines.push("- \n- \n- ");
    return lines.join("\n\n");
  }

  function reviewItemHtml(type, id, title, metaText, checked) {
    return '<label class="review-item">' +
      '<input type="checkbox" data-review-type="' + esc(type) + '" data-review-id="' + esc(id) + '"' + (checked ? " checked" : "") + ">" +
      "<span><strong>" + esc(id) + " · " + esc(title) + "</strong><small>" + esc(metaText || "") + "</small></span>" +
      "</label>";
  }

  function renderReviewList() {
    var el = document.getElementById("review-list");
    if (!el) return;
    var q = (state.reviewQuery || "").trim().toLowerCase();
    function match(text) { return !q || String(text || "").toLowerCase().indexOf(q) !== -1; }
    function group(title, type, items) {
      var visible = items.filter(function (item) { return match(item.search); });
      if (!visible.length) return "";
      return '<div class="review-group"><h4>' + esc(title) + " · " + visible.length + "</h4><div class=\"review-items\">" +
        visible.map(function (item) {
          return reviewItemHtml(type, item.id, item.title, item.meta, state.review[type][item.id] === true);
        }).join("") + "</div></div>";
    }

    var updateItems = updates.map(function (u) {
      return { id: u.id, title: u.name, meta: "v" + (u.displayVersion || fallbackDisplayVersion(u)) + " · " + updateStatusInfo(u.status).label + " · " + (u.tasks || []).length + " tareas", search: [u.id, u.name, u.description, u.motivation, u.displayVersion].join(" ") };
    });
    var updateTaskItems = updateTasks().map(function (t) {
      var f = updateTaskById(t.id);
      var uv = f ? updateVersion(f.update) : null;
      return { id: t.id, title: t.title, meta: (uv ? "v" + uv.displayVersion + " · " + uv.internalId : "—") + " · " + statusInfo(t.status).label, search: [t.id, t.title, t.description].join(" ") };
    });
    var stageItems = stages.map(function (s) {
      return { id: s.id, title: s.name, meta: "Etapa · " + stageProgress(s).pct + "%", search: [s.id, s.name, s.objective].join(" ") };
    });
    var taskItems = tasks.map(function (t) {
      return { id: t.id, title: t.title, meta: t.stageId + " · " + statusInfo(t.status).label, search: [t.id, t.title, t.description, t.stageId].join(" ") };
    });
    var systemItems = (reviewCatalog.systems || []).map(function (s) {
      return { id: s.id, title: s.name, meta: s.description, search: [s.id, s.name, s.description].join(" ") };
    });
    var featureItems = (reviewCatalog.features || []).map(function (f) {
      return { id: f.id, title: f.name, meta: f.description, search: [f.id, f.name, f.description].join(" ") };
    });

    var html = group("Actualizaciones", "updates", updateItems) +
      group("Tareas de actualización", "updateTasks", updateTaskItems) +
      group("Etapas", "stages", stageItems) +
      group("Tareas del plan base", "tasks", taskItems) +
      group("Sistemas", "systems", systemItems) +
      group("Funcionalidades", "features", featureItems);

    el.innerHTML = html || '<div class="empty">No hay elementos que coincidan con la búsqueda.</div>';
    updateReviewCount();
  }

  function updateReviewCount() {
    var n = reviewSelectionCount();
    var countEl = document.getElementById("review-count");
    var hintEl = document.getElementById("review-hint");
    var mdBtn = document.getElementById("review-md");
    var txtBtn = document.getElementById("review-txt");
    if (countEl) countEl.textContent = n + " seleccionado" + (n === 1 ? "" : "s");
    if (hintEl) hintEl.textContent = n ? "Listo para generar el expediente." : "Sin selección.";
    if (mdBtn) mdBtn.disabled = n === 0;
    if (txtBtn) txtBtn.disabled = n === 0;
  }

  function openReviewModal() {
    renderReviewList();
    openModal("review-modal");
  }

  function proposalFields() {
    var systems = Array.prototype.slice.call(document.querySelectorAll("#prop-systems input:checked")).map(function (i) { return i.value; });
    var features = Array.prototype.slice.call(document.querySelectorAll("#prop-features input:checked")).map(function (i) { return i.value; });
    return {
      id: nextUpdateId(),
      displayVersion: nextDisplayVersion(),
      name: (document.getElementById("prop-name").value || "").trim(),
      description: (document.getElementById("prop-description").value || "").trim(),
      objective: (document.getElementById("prop-description").value || "").trim(),
      motivation: (document.getElementById("prop-motivation").value || "").trim(),
      changeType: document.getElementById("prop-type").value,
      priority: document.getElementById("prop-priority").value,
      affectedSystems: systems,
      affectedFeatures: features,
      relatedTasks: (document.getElementById("prop-related").value || "").split(",").map(function (x) { return x.trim(); }).filter(Boolean),
      observations: (document.getElementById("prop-observations").value || "").trim()
    };
  }

  function proposalJson(p) {
    return JSON.stringify({
      id: p.id,
      displayVersion: p.displayVersion || nextDisplayVersion(),
      name: p.name,
      objective: p.objective || p.description,
      status: "PROPUESTA",
      changeType: p.changeType,
      priority: p.priority,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      description: p.description,
      motivation: p.motivation || "Pendiente de completar.",
      affectedSystems: p.affectedSystems,
      affectedFeatures: p.affectedFeatures,
      relatedStages: [],
      relatedTasks: p.relatedTasks || [],
      relatedUpdates: [],
      dependencies: [],
      document: "docs/updates/" + p.id + ".md",
      approval: {
        required: true,
        status: "PENDIENTE",
        approvedBy: null,
        approvedAt: null,
        basis: p.observations || "Pendiente de aprobación del usuario."
      },
      acceptanceCriteria: [],
      tasks: [],
      groups: [],
      observations: p.observations ? [p.observations] : [],
      implementationLog: [],
      reviewExports: []
    }, null, 2);
  }

  function buildProposalDocument(fmt, p) {
    var systems = p.affectedSystems.map(function (id) {
      var s = systemById[id];
      return id + (s ? " · " + s.name : "");
    });
    var features = p.affectedFeatures.map(function (id) {
      var f = featureById[id];
      return id + (f ? " · " + f.name : "");
    });
    var lines = [];
    lines.push(docHeading(fmt, 1, p.id + " · " + p.name));
    lines.push(docHeading(fmt, 2, "Identificación"));
    lines.push(docList(fmt, [
      "ID: " + p.id,
      "Versión de presentación propuesta: " + (p.displayVersion || nextDisplayVersion()),
      "Estado: PROPUESTA (no aprobada, no implementable todavía)",
      "Tipo de cambio: " + updateTypeLabel(p.changeType),
      "Prioridad: " + (PRIORITY_LABEL[p.priority] || p.priority),
      "Creada: " + new Date().toISOString(),
      "Documento propuesto: docs/updates/" + p.id + ".md"
    ]));
    lines.push(docHeading(fmt, 2, "Descripción"));
    lines.push(p.description || MISSING);
    lines.push(docHeading(fmt, 2, "Objetivo principal"));
    lines.push(p.objective || p.description || MISSING);
    lines.push(docHeading(fmt, 2, "Motivo y justificación"));
    lines.push(p.motivation || "Pendiente de completar por el agente o el usuario.");
    lines.push(docHeading(fmt, 2, "Sistemas afectados"));
    lines.push(docList(fmt, systems, "Ninguno seleccionado."));
    lines.push(docHeading(fmt, 2, "Funcionalidades relacionadas"));
    lines.push(docList(fmt, features, "Ninguna seleccionada."));
    lines.push(docHeading(fmt, 2, "Tareas históricas relacionadas"));
    lines.push(docList(fmt, p.relatedTasks || [], "Ninguna indicada."));
    lines.push(docHeading(fmt, 2, "Qué no funciona o qué no satisface al usuario"));
    lines.push(p.observations || "Pendiente de completar. Añadir la crítica concreta y el comportamiento esperado.");
    lines.push(docHeading(fmt, 2, "Cómo funciona actualmente"));
    lines.push("Pendiente de contrastar con el código real por el agente que procese la propuesta. No se afirma nada no verificado.");
    lines.push(docHeading(fmt, 2, "Qué se quiere cambiar"));
    lines.push("Pendiente de detallar. Mantener separado de la descripción general.");
    lines.push(docHeading(fmt, 2, "Partes del código posiblemente implicadas"));
    lines.push(docList(fmt, [], "No documentado todavía. El agente debe localizarlas en el código real."));
    lines.push(docHeading(fmt, 2, "Qué se conservará"));
    lines.push(docList(fmt, [], "Pendiente de definir."));
    lines.push(docHeading(fmt, 2, "Qué se modificará"));
    lines.push(docList(fmt, [], "Pendiente de definir."));
    lines.push(docHeading(fmt, 2, "Qué se añadirá o eliminará"));
    lines.push(docList(fmt, [], "Pendiente de definir."));
    lines.push(docHeading(fmt, 2, "Criterios de aceptación"));
    lines.push(docList(fmt, [], "Pendiente de definir antes de aprobar la actualización."));
    lines.push(docHeading(fmt, 2, "Tareas propuestas"));
    lines.push(docList(fmt, [], "Pendiente de convertir en tareas " + p.id + "-TNN con dependencias y pruebas."));
    lines.push(docHeading(fmt, 2, "Pruebas que verificarán la corrección"));
    lines.push(docList(fmt, [], "Pendiente de definir."));
    lines.push(docHeading(fmt, 2, "Observaciones"));
    lines.push(p.observations || "Sin observaciones adicionales.");
    lines.push(docHeading(fmt, 2, "Aprobación"));
    lines.push("Estado: PENDIENTE. Esta propuesta no se aprueba automáticamente. El usuario debe autorizarla y el agente debe registrar `approvedBy`, `approvedAt` y `basis` en el tracker.");
    lines.push(docHeading(fmt, 2, "Bloque JSON para el tracker"));
    lines.push(fmt === "txt" ? proposalJson(p) : "```json\n" + proposalJson(p) + "\n```");
    return lines.join("\n\n");
  }

  function proposalValid(p) {
    if (!p.name) { toast("La propuesta necesita un nombre"); return false; }
    if (!p.description) { toast("La propuesta necesita una descripción"); return false; }
    var unknown = (p.relatedTasks || []).filter(function (id) {
      return id.indexOf("UPD-") === 0 || !taskById[id] || (taskById[id] && taskById[id].updateId);
    });
    if (unknown.length) {
      toast("Tareas históricas no encontradas: " + unknown.join(", "));
      return false;
    }
    return true;
  }

  function saveDraft(p) {
    try {
      var raw = localStorage.getItem("gi-update-drafts");
      var drafts = raw ? JSON.parse(raw) : [];
      if (!Array.isArray(drafts)) drafts = [];
      drafts = drafts.filter(function (d) { return d.id !== p.id; });
      drafts.unshift({ id: p.id, name: p.name, savedAt: new Date().toISOString(), data: p });
      localStorage.setItem("gi-update-drafts", JSON.stringify(drafts.slice(0, 10)));
      renderDrafts();
    } catch (e) { /* localStorage puede estar restringido en file:// */ }
  }

  function renderDrafts() {
    var box = document.getElementById("draft-box");
    if (!box) return;
    var drafts = [];
    try {
      var raw = localStorage.getItem("gi-update-drafts");
      drafts = raw ? JSON.parse(raw) : [];
    } catch (e) { drafts = []; }
    if (!Array.isArray(drafts) || !drafts.length) {
      box.hidden = true;
      box.innerHTML = "";
      return;
    }
    box.hidden = false;
    box.innerHTML = "<strong>Borradores locales</strong> (no son la fuente canónica):<ul>" +
      drafts.map(function (d, i) {
        return '<li><button type="button" class="chip" data-load-draft="' + i + '">' + esc(d.id) + " · " + esc(d.name) + "</button> " +
          esc(fmtDate(d.savedAt)) + "</li>";
      }).join("") + "</ul>";
  }

  function loadDraft(index) {
    var drafts = [];
    try { drafts = JSON.parse(localStorage.getItem("gi-update-drafts") || "[]"); } catch (e) { drafts = []; }
    var d = drafts[index];
    if (!d || !d.data) return;
    var p = d.data;
    document.getElementById("prop-name").value = p.name || "";
    document.getElementById("prop-description").value = p.description || "";
    document.getElementById("prop-motivation").value = p.motivation || "";
    document.getElementById("prop-type").value = p.changeType || "MEJORA";
    document.getElementById("prop-priority").value = p.priority || "P2";
    document.getElementById("prop-related").value = (p.relatedTasks || []).join(", ");
    document.getElementById("prop-observations").value = p.observations || "";
    Array.prototype.forEach.call(document.querySelectorAll("#prop-systems input"), function (i) {
      i.checked = (p.affectedSystems || []).indexOf(i.value) !== -1;
    });
    Array.prototype.forEach.call(document.querySelectorAll("#prop-features input"), function (i) {
      i.checked = (p.affectedFeatures || []).indexOf(i.value) !== -1;
    });
    toast("Borrador " + p.id + " cargado");
  }

  function renderProposalOptions() {
    var systemsEl = document.getElementById("prop-systems");
    var featuresEl = document.getElementById("prop-features");
    if (!systemsEl || !featuresEl) return;
    systemsEl.innerHTML = (reviewCatalog.systems || []).map(function (s) {
      return '<label><input type="checkbox" value="' + esc(s.id) + '"> <span><strong>' + esc(s.id) + "</strong> · " + esc(s.name) + "</span></label>";
    }).join("") || '<span class="hint">No hay sistemas catalogados.</span>';
    featuresEl.innerHTML = (reviewCatalog.features || []).map(function (f) {
      return '<label><input type="checkbox" value="' + esc(f.id) + '"> <span><strong>' + esc(f.id) + "</strong> · " + esc(f.name) + "</span></label>";
    }).join("") || '<span class="hint">No hay funcionalidades catalogadas.</span>';
  }

  function updateProposalMeta() {
    var idEl = document.getElementById("proposal-id");
    var metaEl = document.getElementById("proposal-meta");
    var id = nextUpdateId();
    var displayVersion = nextDisplayVersion();
    if (idEl) idEl.textContent = "ID propuesto: " + id + " · versión " + displayVersion;
    if (metaEl) {
      metaEl.innerHTML = '<p class="hint">Al descargar, guarda el archivo como <code>docs/updates/' + esc(id) +
        '.md</code>, registra la actualización en <code>updates</code> con <code>displayVersion: "' + esc(displayVersion) + '"</code> y ejecuta <code>python3 tools/sync-dashboard.py</code>. ' +
        "El navegador no puede escribir en el proyecto desde <code>file://</code>.</p>";
    }
  }

  function openProposalModal() {
    renderProposalOptions();
    renderDrafts();
    updateProposalMeta();
    openModal("proposal-modal");
  }

  function dateStamp() {
    var d = new Date();
    var pad = function (n) { return n < 10 ? "0" + n : String(n); };
    return d.getFullYear() + pad(d.getMonth() + 1) + pad(d.getDate()) + "-" + pad(d.getHours()) + pad(d.getMinutes());
  }

  function downloadProposal(fmt) {
    var p = proposalFields();
    if (!proposalValid(p)) return;
    saveDraft(p);
    var text = buildProposalDocument(fmt, p);
    downloadText(p.id + "-propuesta." + fmt, text, fmt === "md" ? "text/markdown;charset=utf-8" : "text/plain;charset=utf-8");
    toast("Propuesta descargada. Guárdala en docs/updates/ y registra " + p.id + " en el tracker.");
  }

  function downloadReview(fmt) {
    if (reviewSelectionCount() === 0) {
      toast("Selecciona al menos un elemento para el expediente");
      return;
    }
    var text = buildReviewDocument(fmt);
    downloadText("expediente-revision-" + dateStamp() + "." + fmt, text,
      fmt === "md" ? "text/markdown;charset=utf-8" : "text/plain;charset=utf-8");
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
        var keepVersion = state.versionId;
        applyTrackerData(next);
        state.versionId = keepVersion && versionById(keepVersion) ? keepVersion : null;
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
      if (!target) return;
      var rect = target.getBoundingClientRect ? target.getBoundingClientRect() : null;
      var top = rect ? rect.top + (window.scrollY || window.pageYOffset || 0) : target.offsetTop;
      if (top <= y) active = a;
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

    var modeBtn = document.getElementById("version-mode-btn");
    if (modeBtn) modeBtn.addEventListener("click", function () {
      state.versionMode = state.versionMode === "descriptive" ? "compact" : "descriptive";
      saveVersionMode(state.versionMode);
      renderVersionSelector();
      saveUiState();
      toast(state.versionMode === "descriptive" ? "Vista descriptiva activada" : "Vista compacta activada");
    });

    var newUpdateBtn = document.getElementById("new-update-btn");
    if (newUpdateBtn) newUpdateBtn.addEventListener("click", openProposalModal);
    var proposalForm = document.getElementById("proposal-form");
    if (proposalForm) proposalForm.addEventListener("submit", function (ev) {
      ev.preventDefault();
      downloadProposal("md");
    });
    var reviewBtn = document.getElementById("review-btn");
    if (reviewBtn) reviewBtn.addEventListener("click", openReviewModal);

    var propMd = document.getElementById("proposal-md");
    if (propMd) propMd.addEventListener("click", function () { downloadProposal("md"); });
    var propTxt = document.getElementById("proposal-txt");
    if (propTxt) propTxt.addEventListener("click", function () { downloadProposal("txt"); });
    var propJson = document.getElementById("proposal-json");
    if (propJson) propJson.addEventListener("click", function () {
      var p = proposalFields();
      if (!proposalValid(p)) return;
      saveDraft(p);
      copyText(proposalJson(p));
    });

    var reviewMd = document.getElementById("review-md");
    if (reviewMd) reviewMd.addEventListener("click", function () { downloadReview("md"); });
    var reviewTxt = document.getElementById("review-txt");
    if (reviewTxt) reviewTxt.addEventListener("click", function () { downloadReview("txt"); });

    var reviewSearch = document.getElementById("review-search");
    if (reviewSearch) reviewSearch.addEventListener("input", function () {
      state.reviewQuery = this.value;
      renderReviewList();
    });
    var reviewAll = document.getElementById("review-all");
    if (reviewAll) reviewAll.addEventListener("click", function () {
      Array.prototype.forEach.call(document.querySelectorAll("#review-list input[data-review-type]"), function (input) {
        state.review[input.getAttribute("data-review-type")][input.getAttribute("data-review-id")] = true;
        input.checked = true;
      });
      updateReviewCount();
    });
    var reviewNone = document.getElementById("review-none");
    if (reviewNone) reviewNone.addEventListener("click", function () {
      state.review = { stages: {}, tasks: {}, updateTasks: {}, features: {}, systems: {}, updates: {} };
      renderReviewList();
    });

    document.addEventListener("change", function (ev) {
      var input = ev.target;
      if (!input || input.tagName !== "INPUT" || input.type !== "checkbox") return;
      var type = input.getAttribute("data-review-type");
      var id = input.getAttribute("data-review-id");
      if (!type || !id) return;
      if (!state.review[type]) state.review[type] = {};
      if (input.checked) state.review[type][id] = true;
      else delete state.review[type][id];
      updateReviewCount();
    });

    document.getElementById("to-top").addEventListener("click", function () {
      try { window.scrollTo({ top: 0, behavior: motionMode() }); } catch (e) { window.scrollTo(0, 0); }
    });

    document.addEventListener("click", function (ev) {
      var el = closest(ev.target, "[data-close-modal]");
      if (el) {
        ev.preventDefault();
        closeModal(el.getAttribute("data-close-modal"));
        return;
      }
      el = closest(ev.target, "[data-select-version]");
      if (el) {
        ev.preventDefault();
        selectVersion(el.getAttribute("data-select-version"), { updateHash: true, scroll: true });
        return;
      }
      el = closest(ev.target, "[data-focus-update]");
      if (el) {
        ev.preventDefault();
        focusUpdate(el.getAttribute("data-focus-update"), { updateHash: true });
        return;
      }
      el = closest(ev.target, "[data-load-draft]");
      if (el) {
        ev.preventDefault();
        loadDraft(parseInt(el.getAttribute("data-load-draft"), 10) || 0);
        return;
      }
      el = closest(ev.target, "[data-focus-task]");
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
      el = closest(ev.target, "[data-version-history-toggle]");
      if (el) {
        state.versionHistoryAll = !state.versionHistoryAll;
        renderVersionHistory(selectedVersion());
        saveUiState();
        return;
      }
      el = closest(ev.target, "[data-history-toggle]");
      if (el) {
        state.historyAll = !state.historyAll;
        renderGlobalHistory();
        saveUiState();
        return;
      }
      el = closest(ev.target, "[data-clear-filters]");
      if (el) {
        clearFilters();
      }
    });

    document.addEventListener("keydown", function (ev) {
      if (ev.key === "Escape" && document.querySelector(".modal:not([hidden])")) {
        ev.preventDefault();
        closeAllModals();
        return;
      }
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
