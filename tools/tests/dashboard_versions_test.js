#!/usr/bin/env node
/*
 * Prueba de renderizado del panel por versiones.
 *
 * Ejecuta dashboard.js en un DOM mínimo de Node (sin navegador ni red) y
 * comprueba que el selector, la línea de tiempo y la plantilla dinámica
 * responden a los datos reales del tracker.
 *
 * Uso:
 *   node tools/tests/dashboard_versions_test.js
 *   GI_PROJECT_ROOT=/ruta/al/proyecto node tools/tests/dashboard_versions_test.js
 */
"use strict";

const fs = require("fs");
const path = require("path");
const vm = require("vm");

const ROOT = process.env.GI_PROJECT_ROOT || process.cwd();
const HTML = fs.readFileSync(path.join(ROOT, "docs/dashboard/index.html"), "utf8");
const JS = fs.readFileSync(path.join(ROOT, "docs/dashboard/dashboard.js"), "utf8");
const DATA_MATCH = /<script id="tracker-data" type="application\/json">([\s\S]*?)<\/script>/.exec(HTML);
if (!DATA_MATCH) {
  console.error("FAIL: no se encontró el bloque tracker-data en index.html");
  process.exit(1);
}
const BASE_DATA = JSON.parse(DATA_MATCH[1].replace(/<\\\//g, "</"));
const DERIVED_MATCH = /<script id="derived-data" type="application\/json">([\s\S]*?)<\/script>/.exec(HTML);
if (!DERIVED_MATCH) {
  console.error("FAIL: no se encontró el bloque derived-data en index.html");
  process.exit(1);
}
const BASE_DERIVED = JSON.parse(DERIVED_MATCH[1].replace(/<\\\//g, "</"));

let failures = 0;
function check(condition, message) {
  if (condition) {
    console.log("ok: " + message);
  } else {
    failures += 1;
    console.error("FAIL: " + message);
  }
}
function text(env, id) {
  const el = env.document.getElementById(id);
  return el ? el.innerHTML : "";
}
function plain(env, id) {
  const el = env.document.getElementById(id);
  return el ? String(el.textContent || "") : "";
}
function attr(env, id, name) {
  const el = env.document.getElementById(id);
  return el ? el.getAttribute(name) : null;
}
function clickDocument(env, selector, dataName, dataValue) {
  const handlers = env.document.__listeners.click || [];
  const target = {
    nodeType: 1,
    parentElement: null,
    matches: function (sel) { return sel === selector; },
    getAttribute: function (name) { return name === dataName ? dataValue : null; }
  };
  handlers.forEach(function (fn) {
    fn({ target: target, preventDefault: function () {} });
  });
}
function clickElement(env, id) {
  const el = env.document.getElementById(id);
  const handlers = (el && el.__listeners.click) || [];
  handlers.forEach(function (fn) { fn({ preventDefault: function () {} }); });
}

class ClassList {
  constructor() { this.set = new Set(); }
  add() { for (const c of arguments) this.set.add(c); }
  remove() { for (const c of arguments) this.set.delete(c); }
  toggle(c, on) {
    const value = on === undefined ? !this.set.has(c) : !!on;
    if (value) this.set.add(c); else this.set.delete(c);
    return value;
  }
  contains(c) { return this.set.has(c); }
}

class Element {
  constructor(tag, id) {
    this.tagName = String(tag || "div").toUpperCase();
    this.id = id || "";
    this.nodeType = 1;
    this.children = [];
    this.parentElement = null;
    this.attributes = {};
    this.style = {};
    this.classList = new ClassList();
    this.__listeners = {};
    this._innerHTML = "";
    this.textContent = "";
    this.value = "";
    this.checked = false;
    this.disabled = false;
    this.hidden = false;
    this.open = false;
    this.offsetWidth = 1;
  }
  get className() { return Array.from(this.classList.set).join(" "); }
  set className(value) { this.classList.set = new Set(String(value).split(/\s+/).filter(Boolean)); }
  get innerHTML() { return this._innerHTML; }
  set innerHTML(value) { this._innerHTML = String(value); }
  addEventListener(type, fn) { (this.__listeners[type] = this.__listeners[type] || []).push(fn); }
  setAttribute(name, value) { this.attributes[name] = String(value); }
  getAttribute(name) { return Object.prototype.hasOwnProperty.call(this.attributes, name) ? this.attributes[name] : null; }
  removeAttribute(name) { delete this.attributes[name]; }
  appendChild(child) { this.children.push(child); child.parentElement = this; return child; }
  removeChild(child) { this.children = this.children.filter(function (x) { return x !== child; }); }
  querySelector() { return null; }
  querySelectorAll() { return []; }
  closest(selector) { return this.matches(selector) ? this : null; }
  matches() { return false; }
  focus() {}
  select() {}
  click() {}
  scrollIntoView() {}
  remove() {}
}

function makeStorage() {
  const map = new Map();
  return {
    getItem: function (k) { return map.has(k) ? map.get(k) : null; },
    setItem: function (k, v) { map.set(k, String(v)); },
    removeItem: function (k) { map.delete(k); },
    clear: function () { map.clear(); }
  };
}

function makeEnv(tracker, options) {
  options = options || {};
  const elements = {};
  const idRe = /id="([^"]+)"/g;
  let m;
  while ((m = idRe.exec(HTML)) !== null) {
    elements[m[1]] = new Element("div", m[1]);
  }
  if (!elements["tracker-data"]) throw new Error("Falta tracker-data");
  elements["tracker-data"].textContent = JSON.stringify(tracker);
  // Sin options.derived el bloque queda vacío: el panel debe usar su respaldo
  // local (HTML antiguo). Con options.derived se comprueba el consumo real.
  if (elements["derived-data"]) {
    elements["derived-data"].textContent = options.derived ? JSON.stringify(options.derived) : "";
  }

  const document = {
    title: "",
    documentElement: new Element("html"),
    body: new Element("body"),
    __listeners: {},
    getElementById: function (id) { return elements[id] || null; },
    createElement: function (tag) { return new Element(tag); },
    querySelector: function (selector) {
      if (selector && selector.charAt(0) === "#") return elements[selector.slice(1)] || null;
      return null;
    },
    querySelectorAll: function () { return []; },
    addEventListener: function (type, fn) { (this.__listeners[type] = this.__listeners[type] || []).push(fn); },
    execCommand: function () { return false; }
  };
  const localStorage = makeStorage();
  const sessionStorage = makeStorage();
  if (options.mode) localStorage.setItem("gi-version-mode", options.mode);
  const location = { protocol: options.protocol || "file:", hash: options.hash || "", href: "file:///docs/dashboard/index.html" };
  const history = {
    pushState: function (_state, _title, hash) { location.hash = hash || ""; }
  };
  const window = {
    document: document,
    location: location,
    history: history,
    localStorage: localStorage,
    sessionStorage: sessionStorage,
    scrollY: 0,
    matchMedia: function () { return { matches: false }; },
    scrollTo: function () {},
    requestAnimationFrame: function (fn) { return fn(); },
    addEventListener: function () {},
    navigator: { clipboard: null }
  };
  const context = {
    document: document,
    window: window,
    navigator: window.navigator,
    history: history,
    localStorage: localStorage,
    sessionStorage: sessionStorage,
    console: console,
    setTimeout: setTimeout,
    clearTimeout: clearTimeout,
    setInterval: setInterval,
    clearInterval: clearInterval,
    URL: { createObjectURL: function () { return "blob:test"; }, revokeObjectURL: function () {} },
    Blob: function Blob() {},
    fetch: options.fetch || function () { throw new Error("fetch no debe usarse con file://"); },
    JSON: JSON,
    Math: Math,
    Date: Date,
    String: String,
    Number: Number,
    Array: Array,
    Object: Object,
    RegExp: RegExp,
    parseInt: parseInt,
    parseFloat: parseFloat,
    isNaN: isNaN
  };
  vm.createContext(context);
  vm.runInContext(JS, context, { filename: "dashboard.js" });
  return { context: context, document: document, window: window, elements: elements };
}

function clone(value) { return JSON.parse(JSON.stringify(value)); }

// 1. Estado real: la versión actual del tracker se selecciona por defecto.
const env = makeEnv(BASE_DATA);
const selector = text(env, "version-selector");
const timeline = text(env, "version-timeline");
const currentId = BASE_DATA.meta.currentUpdateId;
const currentVersion = BASE_DATA.updates.find(function (u) { return u.id === currentId; })
  || BASE_DATA.updates[BASE_DATA.updates.length - 1];
const currentLabel = "v" + currentVersion.displayVersion;
check(selector.indexOf("v0.1") !== -1 && selector.indexOf(currentLabel) !== -1, "el selector muestra de v0.1 a la versión actual");
check(selector.indexOf(currentVersion.id) !== -1 && selector.indexOf("BASE") !== -1, "el selector muestra los identificadores internos");
check(selector.indexOf('data-select-version="' + currentVersion.id + '"') !== -1, "el selector incluye la versión actual");
check(selector.indexOf("version-chip selected") !== -1, "el modo compacto está activo por defecto");
check(selector.indexOf("version-card") === -1, "el modo compacto no renderiza tarjetas descriptivas");
check(timeline.indexOf("v0.1") < timeline.indexOf(currentLabel), "la línea de tiempo va de la versión inicial a la más reciente");
check(text(env, "version-hero").indexOf("Versión " + currentVersion.displayVersion) !== -1, "la cabecera muestra la versión actual");
check(text(env, "version-hero").indexOf(currentVersion.id) !== -1, "la cabecera muestra el identificador actual");
check(text(env, "version-hero").indexOf(currentVersion.name) !== -1, "la cabecera muestra el nombre de la versión");
const implCurrent = currentVersion.tasks.filter(function (t) { return t.category === "implementation" && t.countsForProgress === true; });
const doneCurrent = implCurrent.filter(function (t) { return t.status === "COMPLETADO"; }).length;
check(
  text(env, "version-progress").indexOf(doneCurrent + " de " + implCurrent.length) !== -1,
  "el progreso de " + currentVersion.id + " cuenta " + doneCurrent + " de " + implCurrent.length + " tareas"
);
if ((currentVersion.groups || []).length) {
  check(text(env, "version-groups").indexOf(currentVersion.groups[0].id) !== -1, currentVersion.id + " muestra sus grupos propios");
} else {
  check(text(env, "version-groups").indexOf("Sin etapas ni grupos propios") !== -1, currentVersion.id + " no inventa grupos");
}
check(text(env, "task-list").indexOf(currentVersion.tasks[0].id) !== -1, "la lista muestra la tarea de " + currentVersion.id);
const otherUpdate = BASE_DATA.updates.find(function (u) { return u.id !== currentVersion.id && u.tasks.length; });
if (otherUpdate) {
  check(text(env, "task-list").indexOf(otherUpdate.tasks[0].id) === -1, "la lista no mezcla tareas de " + otherUpdate.id);
}
check(text(env, "task-list").indexOf("T-001") === -1, "la lista no mezcla tareas del plan inicial");
const referencedSessions = {};
currentVersion.tasks.forEach(function (t) {
  (t.executionLog || []).forEach(function (l) { if (l.sessionId) referencedSessions[l.sessionId] = true; });
});
(currentVersion.implementationLog || []).forEach(function (l) { if (l.sessionId) referencedSessions[l.sessionId] = true; });
const referencedIds = Object.keys(referencedSessions);
if (referencedIds.length) {
  check(text(env, "version-history").indexOf(referencedIds[0]) !== -1, "el historial de " + currentVersion.id + " incluye su sesión");
}
const pendingStatuses = ["PROPUESTA", "EN_REVISION", "PENDIENTE_APROBACION"];
const isPending = pendingStatuses.indexOf(currentVersion.status) !== -1;
check(
  isPending
    ? text(env, "pending-approvals").indexOf(currentVersion.id) !== -1
    : text(env, "pending-approvals").indexOf(currentVersion.id) === -1,
  isPending ? currentVersion.id + " aparece como propuesta pendiente" : currentVersion.id + " no aparece como propuesta pendiente"
);
check(fs.readdirSync(path.join(ROOT, "docs/dashboard")).every(function (f) { return !/^UPD-.*\.html$/.test(f); }), "no hay páginas HTML por actualización");

// 2. Cambio de selección: UPD-001 (sin grupos propios) y BASE (con etapas).
const globalBefore = text(env, "summary-cards");
clickDocument(env, "[data-select-version]", "data-select-version", "UPD-001");
check(text(env, "version-hero").indexOf("UPD-001") !== -1, "seleccionar UPD-001 cambia la cabecera");
check(text(env, "version-progress").indexOf("6 de 6") !== -1, "el progreso de UPD-001 cuenta 6 de 6");
check(text(env, "task-list").indexOf("UPD-001-T01") !== -1, "la lista muestra las tareas de UPD-001");
check(text(env, "task-list").indexOf("UPD-006-T01") === -1, "la lista ya no muestra UPD-006");
check(text(env, "version-groups").indexOf("Sin etapas ni grupos propios") !== -1, "UPD-001 no muestra etapas del plan inicial");
check(text(env, "version-groups").indexOf("S0") === -1, "UPD-001 no reutiliza S0");
check(text(env, "summary-cards") === globalBefore, "el resumen global no cambia al seleccionar una versión");

clickDocument(env, "[data-select-version]", "data-select-version", "BASE");
check(text(env, "version-hero").indexOf("Implementación inicial") !== -1, "la versión inicial es seleccionable");
check(text(env, "version-hero").indexOf("Versión 0.1") !== -1, "la versión inicial muestra 0.1");
check(text(env, "version-groups").indexOf("S0") !== -1 && text(env, "version-groups").indexOf("S11") !== -1, "la versión inicial muestra sus etapas S0–S11");
check(text(env, "task-list").indexOf("T-001") !== -1, "la versión inicial muestra sus tareas");
check(text(env, "task-list").indexOf("UPD-001-T01") === -1, "la versión inicial no mezcla tareas de actualizaciones");

// 2a. Filtros y búsqueda sobre la versión seleccionada.
const statusFilter = env.document.getElementById("status-filter");
statusFilter.value = "POSPUESTO";
(statusFilter.__listeners.change || []).forEach(function (fn) { fn.call(statusFilter); });
check(text(env, "task-list").indexOf("T-1103") !== -1 && text(env, "task-list").indexOf("T-1007") !== -1, "el filtro de estado muestra las tareas pospuestas");
check(text(env, "task-list").indexOf("T-001") === -1, "el filtro de estado excluye las demás tareas");
statusFilter.value = "ALL";
(statusFilter.__listeners.change || []).forEach(function (fn) { fn.call(statusFilter); });
const searchInput = env.document.getElementById("search");
searchInput.value = "rayo tractor";
(searchInput.__listeners.input || []).forEach(function (fn) { fn.call(searchInput); });
check(text(env, "task-list").indexOf("T-410") !== -1, "la búsqueda encuentra tareas por texto");
check(text(env, "task-list").indexOf("T-001") === -1, "la búsqueda excluye tareas que no coinciden");
const clearBtn = env.document.getElementById("clear-btn");
(clearBtn.__listeners.click || []).forEach(function (fn) { fn.call(clearBtn); });
check(text(env, "task-list").indexOf("T-001") !== -1, "limpiar filtros restaura la lista de la versión");

// 2b. Enlaces a tareas y compatibilidad con hashes antiguos.
clickDocument(env, "[data-focus-task]", "data-focus-task", "UPD-004-T01");
check(text(env, "version-hero").indexOf("UPD-004") !== -1, "abrir una tarea de actualización selecciona su versión");
check(text(env, "task-list").indexOf("UPD-004-T01") !== -1, "la tarea de actualización queda visible");
clickDocument(env, "[data-focus-task]", "data-focus-task", "T-102");
check(text(env, "version-hero").indexOf("Implementación inicial") !== -1, "abrir una tarea base selecciona la versión 0.1");
check(text(env, "task-list").indexOf("T-102") !== -1, "la tarea base queda visible");

// 2c. Historial largo de la versión inicial.
clickDocument(env, "[data-version-history-toggle]", "data-version-history-toggle", "1");
check(text(env, "version-history").indexOf("Mostrar solo las últimas 8") !== -1, "el historial largo se puede expandir");

// 3. Modo descriptivo y persistencia local.
clickElement(env, "version-mode-btn");
check(text(env, "version-selector").indexOf("version-card") !== -1, "el control cambia al modo descriptivo");
check(text(env, "version-selector").indexOf("Objetivo:") !== -1, "el modo descriptivo muestra el objetivo");
check(env.window.localStorage.getItem("gi-version-mode") === "descriptive", "el modo descriptivo se guarda en localStorage");

// 4. Estado de propuesta pendiente: no se presenta como trabajo en desarrollo.
const pendingData = clone(BASE_DATA);
const pendingUpdate = pendingData.updates.find(function (u) { return u.id === "UPD-001"; });
pendingUpdate.status = "PENDIENTE_APROBACION";
pendingUpdate.approval.status = "PENDIENTE";
pendingUpdate.approval.approvedBy = null;
pendingUpdate.approval.approvedAt = null;
pendingUpdate.tasks.forEach(function (t) { t.status = "PENDIENTE"; });
pendingData.meta.currentUpdateId = "UPD-001";
const pendingEnv = makeEnv(pendingData);
check(text(pendingEnv, "version-alert").indexOf("propuesta") !== -1, "una propuesta pendiente muestra un aviso claro");
check(text(pendingEnv, "pending-approvals").indexOf("UPD-001") !== -1, "la propuesta pendiente aparece en la sección global");
check(
  text(pendingEnv, "version-progress").indexOf('<div class="label">Progreso de implementación</div><div class="value">0%</div>') !== -1,
  "una propuesta no se presenta con progreso ficticio de completada"
);

// 5. Hash profundo y compatibilidad con enlaces antiguos.
const hashEnv = makeEnv(BASE_DATA, { hash: "#version-UPD-002" });
check(text(hashEnv, "version-hero").indexOf("UPD-002") !== -1, "el hash #version-UPD-002 selecciona UPD-002");

const taskHashEnv = makeEnv(BASE_DATA, { hash: "#task-UPD-002-T01" });
check(text(taskHashEnv, "version-hero").indexOf("UPD-002") !== -1, "el hash #task-UPD-002-T01 selecciona su versión");
check(text(taskHashEnv, "task-list").indexOf("UPD-002-T01") !== -1, "el hash #task-UPD-002-T01 abre la tarea");

const oldUpdateHashEnv = makeEnv(BASE_DATA, { hash: "#update-UPD-003" });
check(text(oldUpdateHashEnv, "version-hero").indexOf("UPD-003") !== -1, "el hash antiguo #update-UPD-003 sigue funcionando");

const oldTaskHashEnv = makeEnv(BASE_DATA, { hash: "#update-task-UPD-004-T01" });
check(text(oldTaskHashEnv, "version-hero").indexOf("UPD-004") !== -1, "el hash antiguo #update-task-… sigue funcionando");

// 6. Preferencia de modo persistida entre sesiones.
const descriptiveEnv = makeEnv(BASE_DATA, { mode: "descriptive" });
check(text(descriptiveEnv, "version-selector").indexOf("version-card") !== -1, "el modo descriptivo guardado se restaura");
check(text(descriptiveEnv, "version-selector").indexOf("Objetivo:") !== -1, "el modo restaurado muestra el objetivo");

// 7. Estados de actualización: en progreso y bloqueada.
const progressData = clone(BASE_DATA);
const progressUpdate = progressData.updates.find(function (u) { return u.id === "UPD-002"; });
progressUpdate.status = "EN_PROGRESO";
progressUpdate.tasks[0].status = "EN_PROGRESO";
progressData.meta.currentUpdateId = "UPD-002";
const progressEnv = makeEnv(progressData);
check(text(progressEnv, "version-hero").indexOf("En progreso") !== -1, "una versión en progreso muestra su estado");
check(text(progressEnv, "current-main").indexOf("v0.3") !== -1, "el estado actual muestra la versión de la tarea activa");

const blockedData = clone(BASE_DATA);
const blockedUpdate = blockedData.updates.find(function (u) { return u.id === "UPD-003"; });
blockedUpdate.status = "BLOQUEADA";
blockedData.meta.currentUpdateId = "UPD-003";
const blockedEnv = makeEnv(blockedData);
check(text(blockedEnv, "version-alert").toLowerCase().indexOf("bloqueada") !== -1, "una versión bloqueada muestra un aviso claro");
check(text(blockedEnv, "version-hero").indexOf("Bloqueada") !== -1, "una versión bloqueada conserva su estado en la cabecera");

// 8. Selección por defecto sin meta.currentUpdateId.
const noCurrentData = clone(BASE_DATA);
noCurrentData.meta.currentUpdateId = null;
const noCurrentEnv = makeEnv(noCurrentData);
const latestVersionId = BASE_DATA.updates[BASE_DATA.updates.length - 1].id;
check(text(noCurrentEnv, "version-hero").indexOf(latestVersionId) !== -1, "sin actualización actual se selecciona la versión más reciente");

const multiActiveData = clone(BASE_DATA);
multiActiveData.meta.currentUpdateId = null;
multiActiveData.updates.forEach(function (u) {
  if (u.id !== "UPD-001" && u.id !== "UPD-002") u.status = "COMPLETADA";
});
multiActiveData.updates.find(function (u) { return u.id === "UPD-001"; }).status = "EN_PROGRESO";
multiActiveData.updates.find(function (u) { return u.id === "UPD-002"; }).status = "EN_PROGRESO";
const multiActiveEnv = makeEnv(multiActiveData);
check(text(multiActiveEnv, "version-hero").indexOf("UPD-002") !== -1, "con varias activas se selecciona la más reciente activa");

// 9. Añadir una actualización nueva no requiere otra plantilla ni otra página.
const newData = clone(BASE_DATA);
newData.updates.push({
  id: "UPD-900",
  displayVersion: "0.99",
  objective: "Demostrar que una versión nueva usa la misma plantilla.",
  name: "Versión nueva de prueba",
  status: "PROPUESTA",
  changeType: "MEJORA",
  priority: "P2",
  createdAt: "2026-11-08T16:00:00-06:00",
  updatedAt: "2026-11-08T16:00:00-06:00",
  description: "Actualización de prueba para la plantilla dinámica.",
  motivation: "Verificar que añadir una actualización no crea otro HTML.",
  affectedSystems: [],
  affectedFeatures: [],
  relatedStages: [],
  relatedTasks: [],
  relatedUpdates: [],
  dependencies: [],
  document: "docs/updates/UPD-900.md",
  approval: {
    required: true,
    status: "PENDIENTE",
    approvedBy: null,
    approvedAt: null,
    basis: "Prueba."
  },
  acceptanceCriteria: [],
  tasks: [
    {
      id: "UPD-900-T01",
      updateId: "UPD-900",
      title: "Tarea de la versión nueva",
      description: "Tarea de prueba.",
      category: "implementation",
      priority: "P2",
      status: "PENDIENTE",
      countsForProgress: true,
      dependencies: [],
      relatedTaskIds: [],
      decisionIds: [],
      files: [],
      acceptanceCriteria: [],
      tests: [],
      technicalNotes: "",
      executionLog: [],
      evidence: [],
      createdAt: "2026-11-08T16:00:00-06:00",
      updatedAt: "2026-11-08T16:00:00-06:00"
    }
  ],
  groups: [
    {
      id: "G1",
      name: "Grupo de la versión nueva",
      objective: "Agrupar la tarea nueva.",
      description: "Grupo de prueba.",
      status: "PENDIENTE",
      taskIds: ["UPD-900-T01"],
      dependencies: [],
      tests: ["Prueba de grupo"]
    }
  ],
  observations: [],
  implementationLog: [],
  reviewExports: []
});
const newEnv = makeEnv(newData);
check(text(newEnv, "version-selector").indexOf("v0.99") !== -1 && text(newEnv, "version-selector").indexOf("UPD-900") !== -1, "una actualización nueva aparece en el selector");
check(text(newEnv, "version-timeline").indexOf("v0.99") !== -1, "una actualización nueva aparece en la línea de tiempo");
clickDocument(newEnv, "[data-select-version]", "data-select-version", "UPD-900");
check(text(newEnv, "version-hero").indexOf("UPD-900") !== -1, "la actualización nueva usa la misma cabecera");
check(text(newEnv, "task-list").indexOf("UPD-900-T01") !== -1, "la actualización nueva usa la misma lista de tareas");
check(text(newEnv, "version-groups").indexOf("G1") !== -1, "la actualización nueva muestra sus grupos explícitos");

// 10. Indicadores derivados canónicos (UPD-011).
// Sin bloque derivado el panel conserva el cálculo local de respaldo.
check(
  text(env, "summary-cards").indexOf(BASE_DERIVED.progresoBase.porcentaje + "%") !== -1,
  "sin derived-data el panel conserva el cálculo de respaldo"
);
const derivedRealEnv = makeEnv(BASE_DATA, { derived: BASE_DERIVED });
check(
  text(derivedRealEnv, "summary-cards").indexOf(BASE_DERIVED.progresoBase.porcentaje + "%") !== -1,
  "los KPIs del panel coinciden con derived-data"
);
check(
  plain(derivedRealEnv, "top-progress").indexOf(BASE_DERIVED.progresoBase.porcentaje + "%") !== -1,
  "la franja superior usa el progreso derivado"
);
const estadoActual = BASE_DATA.updates.find(function (u) { return u.id === BASE_DERIVED.actualizacionActual; });
check(
  !estadoActual || ["PROPUESTA", "EN_REVISION", "PENDIENTE_APROBACION"].indexOf(estadoActual.status) === -1,
  "ninguna propuesta sin aprobar figura como Actualización actual"
);
check(
  plain(derivedRealEnv, "top-update").indexOf(BASE_DERIVED.actualizacionActual) !== -1,
  "top-update muestra la actualización derivada"
);

// Si los derivados cambian y el tracker no, los KPIs deben cambiar: prueba de
// que el JS consume DERIVED en vez de recalcular por su cuenta.
const derivadosMutados = clone(BASE_DERIVED);
derivadosMutados.progresoBase = { hechas: 7, total: 10, porcentaje: 70 };
derivadosMutados.actualizacionActual = "UPD-001";
derivadosMutados.verificacion = Object.assign({}, BASE_DERIVED.verificacion, { resultado: "REVISAR" });
const derivedMutadoEnv = makeEnv(BASE_DATA, { derived: derivadosMutados });
check(text(derivedMutadoEnv, "summary-cards").indexOf("70%") !== -1, "alterar derived-data altera los KPIs del panel");
check(plain(derivedMutadoEnv, "top-update").indexOf("UPD-001") !== -1, "currentUpdate() lee la actualización derivada");
check(text(derivedMutadoEnv, "verification").indexOf("REVISAR") !== -1, "el badge de verificación lee DERIVED");

// 11. Refresco HTTP validado (UPD-011-T04). Thenable síncrono para resolver
// la cadena de promesas de refreshData() sin depender del loop de eventos.
function thenableSincrono(valor) {
  return {
    then: function (fn) {
      try { return thenableSincrono(fn(valor)); }
      catch (e) { return thenableRechazado(e); }
    },
    catch: function () { return this; }
  };
}
function thenableRechazado(error) {
  return {
    then: function () { return this; },
    catch: function (fn) { fn(error); return thenableSincrono(null); }
  };
}
function envConFetch(trackerRemoto) {
  return makeEnv(BASE_DATA, {
    protocol: "https:",
    derived: BASE_DERIVED,
    fetch: function () {
      return thenableSincrono({
        ok: true,
        text: function () { return JSON.stringify(trackerRemoto); }
      });
    }
  });
}

const jsonInvalido = { meta: { lastUpdated: BASE_DERIVED.derivadoDe }, tasks: [], stages: [], updates: "no-es-un-array" };
const envRechazo = envConFetch(jsonInvalido);
check(plain(envRechazo, "data-source").indexOf("JSON rechazado") !== -1, "un JSON vivo con forma inválida se rechaza con warn");
check(plain(envRechazo, "data-source").indexOf("JSON en vivo") === -1, "nunca se marca ok ante un JSON rechazado");
check(text(envRechazo, "summary-cards").indexOf(BASE_DERIVED.progresoBase.porcentaje + "%") !== -1, "se conservan los KPIs de los embebidos");

const desincronizado = clone(BASE_DATA);
desincronizado.meta.lastUpdated = "2000-01-01T00:00:00Z";
const envDesfase = envConFetch(desincronizado);
check(plain(envDesfase, "data-source").indexOf("JSON rechazado") !== -1, "un tracker desincronizado con derived-data no se aplica");

const envVivo = envConFetch(BASE_DATA);
check(plain(envVivo, "data-source").indexOf("JSON en vivo") !== -1, "un JSON coherente se aplica y marca ok");

if (failures) {
  console.error("\n" + failures + " comprobaciones fallaron.");
  process.exit(1);
}
console.log("\nOK: panel por versiones verificado.");
