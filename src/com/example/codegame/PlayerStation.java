package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Estación personal del jugador: una plataforma vacía y acoplable, sin almacén,
 * reparación, refinería ni mercado por defecto. Los servicios solo aparecen si
 * se construye el módulo correspondiente en una ranura. Es una entidad distinta
 * de la nave nodriza.
 */
final class PlayerStation {

    static final String ID = "player-station";
    static final int SLOTS = 6;
    static final float RADIUS = 60f;
    static final float DOCK_RADIUS = 110f;
    static final float SAFE_RADIUS = 150f;

    final String id = ID;
    final String name = "PLATAFORMA";
    final Vector2 pos = new Vector2();
    /** Definición visual/servicios base: todo desactivado. */
    final StationDef def = GameData.PLAYER_PLATFORM;
    private final StationModule[] slots = new StationModule[SLOTS];
    private boolean built;
    private String sectorId = GameData.START_SECTOR_ID;

    PlayerStation() {
    }

    /** Plataforma ya colocada en el mundo (para pruebas y T-902). */
    PlayerStation(float x, float y) {
        place(x, y, GameData.START_SECTOR_ID);
    }

    void place(float x, float y, String sectorId) {
        pos.set(x, y);
        this.sectorId = sectorId == null ? GameData.START_SECTOR_ID : sectorId;
        built = true;
    }

    void restore(boolean built, float x, float y, String sectorId) {
        this.built = built;
        this.pos.set(x, y);
        this.sectorId = sectorId == null ? GameData.START_SECTOR_ID : sectorId;
    }

    String sectorId() {
        return sectorId;
    }

    boolean inSector(String id) {
        return built && sectorId.equals(id);
    }

    /** Dibujo de la plataforma vacía: un cuadrado sencillo con luz de borde. */
    void draw(ShapeRenderer shapes, float time) {
        if (!built || shapes == null) {
            return;
        }
        float pulse = 0.72f + 0.28f * MathUtils.sin(time * 2.2f);
        shapes.setColor(0.08f, 0.14f, 0.24f, 0.95f);
        shapes.rect(pos.x - RADIUS, pos.y - RADIUS, RADIUS * 2f, RADIUS * 2f);
        shapes.setColor(0.22f, 0.42f, 0.68f, 0.85f);
        shapes.rect(pos.x - RADIUS + 8f, pos.y - RADIUS + 8f,
                RADIUS * 2f - 16f, RADIUS * 2f - 16f);
        shapes.setColor(0.08f, 0.14f, 0.24f, 1f);
        shapes.rect(pos.x - RADIUS + 14f, pos.y - RADIUS + 14f,
                RADIUS * 2f - 28f, RADIUS * 2f - 28f);
        shapes.setColor(0.45f, 0.95f, 1f, 0.35f * pulse);
        shapes.rectLine(pos.x - RADIUS, pos.y + RADIUS,
                pos.x + RADIUS, pos.y + RADIUS, 3f);
        shapes.rectLine(pos.x - RADIUS, pos.y - RADIUS,
                pos.x - RADIUS, pos.y + RADIUS, 3f);
        shapes.rectLine(pos.x + RADIUS, pos.y - RADIUS,
                pos.x + RADIUS, pos.y + RADIUS, 3f);
        shapes.rectLine(pos.x - RADIUS, pos.y - RADIUS,
                pos.x + RADIUS, pos.y - RADIUS, 3f);
        shapes.setColor(0.45f, 0.95f, 1f, 0.20f + 0.15f * pulse);
        shapes.circle(pos.x, pos.y, 16f + 3f * pulse, 18);
    }

    boolean built() {
        return built;
    }

    int slotCount() {
        return SLOTS;
    }

    StationModule moduleAt(int slot) {
        return slot >= 0 && slot < SLOTS ? slots[slot] : null;
    }

    int moduleCount() {
        int count = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (slots[i] != null) {
                count++;
            }
        }
        return count;
    }

    boolean hasModule(ModuleDef module) {
        if (module == null) {
            return false;
        }
        for (int i = 0; i < SLOTS; i++) {
            if (slots[i] != null && slots[i].def == module) {
                return true;
            }
        }
        return false;
    }

    /** Máscara de servicios disponibles (0 = plataforma vacía). */
    int serviceMask() {
        int mask = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (slots[i] != null) {
                mask |= 1 << slots[i].service().ordinal();
            }
        }
        return mask;
    }

    boolean hasService(ModuleDef.Service service) {
        return service != null && (serviceMask() & (1 << service.ordinal())) != 0;
    }

    boolean storage() {
        return hasService(ModuleDef.Service.STORAGE);
    }

    boolean repair() {
        return hasService(ModuleDef.Service.REPAIR);
    }

    boolean refinery() {
        return hasService(ModuleDef.Service.REFINERY);
    }

    boolean market() {
        return hasService(ModuleDef.Service.MARKET);
    }

    int freeSlots() {
        return SLOTS - moduleCount();
    }

    /** Resultado de una construcción o ampliación. */
    static final class BuildResult {
        final boolean ok;
        final String error;
        final StationModule module;

        private BuildResult(boolean ok, String error, StationModule module) {
            this.ok = ok;
            this.error = error;
            this.module = module;
        }
    }

    /**
     * Construye un módulo en una ranura vacía. Única vía para obtener un
     * servicio: cobra créditos y materiales, valida requisitos y no deja
     * recursos a medias si algo falla.
     */
    BuildResult build(int slot, ModuleDef module, Upgrades upgrades, CargoHold cargo) {
        if (!built || module == null || slot < 0 || slot >= SLOTS
                || slots[slot] != null) {
            return new BuildResult(false, "RANURA INVALIDA", null);
        }
        if (module.requires != null && !hasModule(module.requires)) {
            return new BuildResult(false, "FALTA MODULO PREVIO", null);
        }
        if (upgrades == null || upgrades.coins() < module.creditCost) {
            return new BuildResult(false, "FALTAN CREDITOS", null);
        }
        if (cargo == null) {
            return new BuildResult(false, "SIN CARGA", null);
        }
        for (Commodity commodity : Commodity.values()) {
            if (cargo.amount(commodity) < module.material(commodity)) {
                return new BuildResult(false, "FALTAN MATERIALES", null);
            }
        }
        upgrades.spendCoins(module.creditCost);
        for (Commodity commodity : Commodity.values()) {
            int amount = module.material(commodity);
            if (amount > 0) {
                cargo.removeExact(commodity, amount);
            }
        }
        StationModule installed = new StationModule(module);
        slots[slot] = installed;
        return new BuildResult(true, "", installed);
    }

    /** Amplía un módulo existente; el servicio no cambia. */
    BuildResult upgrade(int slot, Upgrades upgrades, CargoHold cargo) {
        StationModule module = moduleAt(slot);
        if (module == null) {
            return new BuildResult(false, "RANURA VACIA", null);
        }
        if (!module.canUpgrade()) {
            return new BuildResult(false, "NIVEL MAXIMO", module);
        }
        int credits = module.upgradeCredits();
        if (upgrades == null || upgrades.coins() < credits) {
            return new BuildResult(false, "FALTAN CREDITOS", module);
        }
        if (cargo == null) {
            return new BuildResult(false, "SIN CARGA", module);
        }
        for (Commodity commodity : Commodity.values()) {
            if (cargo.amount(commodity) < module.upgradeMaterial(commodity)) {
                return new BuildResult(false, "FALTAN MATERIALES", module);
            }
        }
        upgrades.spendCoins(credits);
        for (Commodity commodity : Commodity.values()) {
            int amount = module.upgradeMaterial(commodity);
            if (amount > 0) {
                cargo.removeExact(commodity, amount);
            }
        }
        module.upgrade();
        return new BuildResult(true, "", module);
    }

    StationModule remove(int slot) {
        if (slot < 0 || slot >= SLOTS) {
            return null;
        }
        StationModule removed = slots[slot];
        slots[slot] = null;
        return removed;
    }

    float distanceTo(Vector2 point) {
        return point == null || !built ? -1f : point.dst(pos);
    }

    boolean canDock(Vector2 point, float speed) {
        return built && distanceTo(point) <= DOCK_RADIUS && speed < 30f;
    }

    boolean inSafeZone(Vector2 point) {
        return point != null && inSafeZone(point.x, point.y);
    }

    boolean inSafeZone(float x, float y) {
        if (!built) {
            return false;
        }
        float dx = x - pos.x;
        float dy = y - pos.y;
        return dx * dx + dy * dy < SAFE_RADIUS * SAFE_RADIUS;
    }
}
