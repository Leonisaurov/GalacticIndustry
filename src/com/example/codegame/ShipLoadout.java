package com.example.codegame;

/**
 * Loadout activo de la nave: niveles de mejora y ranuras genericas de modulos.
 *
 * <p>Es la fuente unica de los niveles; {@link Upgrades} actua como fachada de
 * tienda y no guarda una segunda copia. Las formulas de movimiento y combate
 * siguen en {@link Upgrades} para no cambiar el comportamiento visible.</p>
 */
class ShipLoadout {

    /** Almacen de persistencia; Upgrades lo implementa sobre Preferences o memoria. */
    interface Store {
        int getInteger(String key);

        void putInteger(String key, int value);

        String getString(String key);

        void putString(String key, String value);
    }

    static final int MOTOR = 0;
    static final int DEPOSITO = 1;
    static final int CANON = 2;
    static final int BLINDAJE = 3;
    static final int GASOLINA = 4;
    static final int BODEGA = 5;
    /** Láser minero: potencia, penetración y capacidad del capacitor. */
    static final int LASER = 6;
    /** Rayo tractor: alcance, velocidad y fragmentos simultáneos. */
    static final int TRACTOR = 7;
    static final int COUNT = 8;
    static final int MAX_LEVEL = 5;

    static final int MODULE_SLOTS = 4;
    /** Modulo previsto para el piloto automatico; aun no se exige para volar. */
    static final String AUTOPILOT_MODULE = "autopilot";

    private final Store store;
    private final int[] levels = new int[COUNT];
    private final String[] modules = new String[MODULE_SLOTS];

    ShipLoadout(Store store) {
        if (store == null) {
            throw new IllegalArgumentException("store nulo");
        }
        this.store = store;
        load();
    }

    /** Relee niveles y modulos del almacen. */
    void load() {
        for (int i = 0; i < COUNT; i++) {
            levels[i] = clamp(store.getInteger("lvl" + i), 0, MAX_LEVEL);
        }
        for (int i = 0; i < MODULE_SLOTS; i++) {
            modules[i] = normalize(store.getString("mod" + i));
        }
    }

    int level(int index) {
        return validIndex(index) ? levels[index] : 0;
    }

    boolean canUpgrade(int index) {
        return validIndex(index) && levels[index] < MAX_LEVEL;
    }

    boolean upgrade(int index) {
        if (!canUpgrade(index)) {
            return false;
        }
        setLevel(index, levels[index] + 1);
        return true;
    }

    void setLevel(int index, int value) {
        if (!validIndex(index)) {
            return;
        }
        levels[index] = clamp(value, 0, MAX_LEVEL);
        store.putInteger("lvl" + index, levels[index]);
    }

    int moduleSlots() {
        return MODULE_SLOTS;
    }

    String module(int slot) {
        return slot >= 0 && slot < MODULE_SLOTS ? modules[slot] : null;
    }

    boolean installModule(int slot, String id) {
        if (slot < 0 || slot >= MODULE_SLOTS || id == null || id.isEmpty()) {
            return false;
        }
        modules[slot] = id;
        store.putString("mod" + slot, id);
        return true;
    }

    boolean removeModule(int slot) {
        if (slot < 0 || slot >= MODULE_SLOTS) {
            return false;
        }
        modules[slot] = null;
        store.putString("mod" + slot, null);
        return true;
    }

    boolean hasModule(String id) {
        return slotOf(id) >= 0;
    }

    int slotOf(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < MODULE_SLOTS; i++) {
            if (id.equals(modules[i])) {
                return i;
            }
        }
        return -1;
    }

    boolean hasAutopilot() {
        return hasModule(AUTOPILOT_MODULE);
    }

    int installedModules() {
        int count = 0;
        for (int i = 0; i < MODULE_SLOTS; i++) {
            if (modules[i] != null) {
                count++;
            }
        }
        return count;
    }

    private static boolean validIndex(int index) {
        return index >= 0 && index < COUNT;
    }

    private static String normalize(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : value > max ? max : value;
    }
}
