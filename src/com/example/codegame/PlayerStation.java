package com.example.codegame;

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
    private final ModuleDef[] slots = new ModuleDef[SLOTS];
    private boolean built;

    PlayerStation() {
    }

    /** Plataforma ya colocada en el mundo (para pruebas y T-902). */
    PlayerStation(float x, float y) {
        place(x, y);
    }

    void place(float x, float y) {
        pos.set(x, y);
        built = true;
    }

    boolean built() {
        return built;
    }

    int slotCount() {
        return SLOTS;
    }

    ModuleDef moduleAt(int slot) {
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
            if (slots[i] == module) {
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
                mask |= 1 << slots[i].service.ordinal();
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

    /**
     * Coloca un módulo en una ranura vacía. El coste y la construcción
     * transaccional llegan en T-903; aquí solo se valida la estructura y los
     * requisitos.
     */
    boolean install(int slot, ModuleDef module) {
        if (!built || module == null || slot < 0 || slot >= SLOTS
                || slots[slot] != null) {
            return false;
        }
        if (module.requires != null && !hasModule(module.requires)) {
            return false;
        }
        slots[slot] = module;
        return true;
    }

    ModuleDef remove(int slot) {
        if (slot < 0 || slot >= SLOTS) {
            return null;
        }
        ModuleDef removed = slots[slot];
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
        if (!built || point == null) {
            return false;
        }
        return point.dst(pos) < SAFE_RADIUS;
    }
}
