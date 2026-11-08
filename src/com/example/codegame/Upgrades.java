package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.HashMap;
import java.util.Map;

/** Mejoras de la tienda y monedas guardadas entre partidas. */
class Upgrades {

    static final int MOTOR = ShipLoadout.MOTOR;
    static final int DEPOSITO = ShipLoadout.DEPOSITO;
    static final int CANON = ShipLoadout.CANON;
    static final int BLINDAJE = ShipLoadout.BLINDAJE;
    static final int GASOLINA = ShipLoadout.GASOLINA;
    static final int COUNT = ShipLoadout.COUNT;
    static final int MAX_LEVEL = ShipLoadout.MAX_LEVEL;
    /** Precio del combustible en la tienda temporal: 1 crédito por unidad. */
    static final float FUEL_PRICE_PER_UNIT = 1f;
    /** Coste mínimo de una recarga de combustible. */
    static final int FUEL_REFILL_MIN = 5;
    /** Precio base de la recarga de emergencia (compatibilidad histórica). */
    static final int REFILL_BASE = 10;
    /** Precio del O2 en la tienda temporal: 0,5 créditos por unidad. */
    static final float O2_PRICE_PER_UNIT = 0.5f;
    /** Coste mínimo de una recarga de O2. */
    static final int O2_REFILL_MIN = 5;

    static final String[] NAMES = {"Motor", "Deposito", "Canon", "Blindaje", "Gasolina"};
    static final String[] DESCS = {
            "Mas aceleracion y velocidad",
            "Mas combustible maximo",
            "Mas dano y cadencia de disparo",
            "Refuerza la integridad del casco",
            "Menos consumo de combustible"
    };

    /** Preferencias de Android; null cuando se usa el almacén en memoria (tests). */
    private final Preferences prefs;
    private final Map<String, Integer> memory = new HashMap<>();
    private final Map<String, String> memoryText = new HashMap<>();
    private final ShipLoadout.Store store;
    /** Fuente unica de niveles y ranuras; Upgrades no duplica esos datos. */
    final ShipLoadout loadout;

    Upgrades() {
        this.prefs = Gdx.app.getPreferences("codegame");
        this.store = new ShipLoadout.Store() {
            @Override
            public int getInteger(String key) {
                return getInt(key);
            }

            @Override
            public void putInteger(String key, int value) {
                putInt(key, value);
            }

            @Override
            public String getString(String key) {
                return getText(key);
            }

            @Override
            public void putString(String key, String value) {
                putText(key, value);
            }
        };
        this.loadout = new ShipLoadout(store);
    }

    /** Constructor sin Gdx, para pruebas de lógica en la JVM. */
    Upgrades(boolean inMemory) {
        this.prefs = null;
        this.store = new ShipLoadout.Store() {
            @Override
            public int getInteger(String key) {
                return getInt(key);
            }

            @Override
            public void putInteger(String key, int value) {
                putInt(key, value);
            }

            @Override
            public String getString(String key) {
                return getText(key);
            }

            @Override
            public void putString(String key, String value) {
                putText(key, value);
            }
        };
        this.loadout = new ShipLoadout(store);
    }

    private int getInt(String key) {
        if (prefs != null) {
            return prefs.getInteger(key, 0);
        }
        Integer v = memory.get(key);
        return v == null ? 0 : v;
    }

    private void putInt(String key, int value) {
        if (prefs != null) {
            prefs.putInteger(key, value);
            prefs.flush();
        } else {
            memory.put(key, value);
        }
    }

    int coins() {
        return getInt("coins");
    }

    void addCoins(int amount) {
        putInt("coins", coins() + amount);
    }

    /** Fija el saldo al cargar una partida; nunca deja valores negativos. */
    void setCoins(int value) {
        putInt("coins", Math.max(0, value));
    }

    /** Fija el contador de recargas al cargar una partida. */
    void setRefillCount(int value) {
        putInt("refills", Math.max(0, value));
    }

    int level(int index) {
        return loadout.level(index);
    }

    int cost(int index) {
        int next = level(index) + 1;
        return 50 * next * next;
    }

    boolean canBuy(int index) {
        return level(index) < MAX_LEVEL && coins() >= cost(index);
    }

    /** Veces que se ha comprado la recarga. */
    int refillCount() {
        return getInt("refills");
    }

    /** Precio actual de la recarga: 1 crédito/unidad, mínimo 5. */
    int refillCost(float missingFuel) {
        if (missingFuel < 0.5f) {
            return 0;
        }
        return Math.max(FUEL_REFILL_MIN,
                (int) Math.ceil(missingFuel * FUEL_PRICE_PER_UNIT));
    }

    /** Compatibilidad: coste de rellenar el depósito desde vacío. */
    int refillCost() {
        return refillCost(maxFuel());
    }

    void recordRefill() {
        putInt("refills", refillCount() + 1);
    }

    /**
     * Coste de rellenar O2: 0 si está lleno; si falta algo, 0,5 créditos por
     * unidad con un mínimo de 5. No usa el contador de recargas de combustible.
     */
    int o2RefillCost(float missingOxygen) {
        if (missingOxygen < 0.5f) {
            return 0;
        }
        return Math.max(O2_REFILL_MIN, (int) Math.ceil(missingOxygen * O2_PRICE_PER_UNIT));
    }

    /** Texto guardado (p. ej. la partida en curso). Con null borra la entrada. */
    String getText(String key) {
        if (prefs != null) {
            return prefs.getString(key, null);
        }
        return memoryText.get(key);
    }

    void putText(String key, String value) {
        if (prefs != null) {
            if (value == null) {
                prefs.remove(key);
            } else {
                prefs.putString(key, value);
            }
            prefs.flush();
        } else if (value == null) {
            memoryText.remove(key);
        } else {
            memoryText.put(key, value);
        }
    }

    /** Descuenta monedas si hay saldo suficiente. */
    boolean spendCoins(int amount) {
        if (coins() < amount) {
            return false;
        }
        putInt("coins", coins() - amount);
        return true;
    }

    boolean buy(int index) {
        if (!canBuy(index)) {
            return false;
        }
        putInt("coins", coins() - cost(index));
        loadout.upgrade(index);
        return true;
    }

    float thrust() {
        return 260f + 60f * level(MOTOR);
    }

    float maxSpeed() {
        return 220f + 40f * level(MOTOR);
    }

    float maxFuel() {
        return 160f + 30f * level(DEPOSITO);
    }

    int damage() {
        return 1 + level(CANON);
    }

    float fireCooldown() {
        return 0.35f - 0.04f * level(CANON);
    }

    /** Integridad maxima del casco: 100 sin mejora y 225 al nivel 5 de Blindaje. */
    float hull() {
        return 100f + 25f * level(BLINDAJE);
    }

    /** Multiplicador del consumo de combustible: 1 sin mejora, 0.6 con nivel máximo. */
    float fuelUse() {
        return 1f - 0.08f * level(GASOLINA);
    }

    /** Loadout activo de la nave; lo usara tambien ShipState en tareas posteriores. */
    ShipLoadout loadout() {
        return loadout;
    }
}
