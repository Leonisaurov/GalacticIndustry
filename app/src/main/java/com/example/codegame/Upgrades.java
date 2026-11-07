package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.HashMap;
import java.util.Map;

/** Mejoras de la tienda y monedas guardadas entre partidas. */
class Upgrades {

    static final int MOTOR = 0;
    static final int DEPOSITO = 1;
    static final int CANON = 2;
    static final int BLINDAJE = 3;
    static final int GASOLINA = 4;
    static final int COUNT = 5;
    static final int MAX_LEVEL = 5;
    /** Precio base de la recarga de emergencia; se duplica con cada compra. */
    static final int REFILL_BASE = 10;

    static final String[] NAMES = {"Motor", "Deposito", "Canon", "Blindaje", "Gasolina"};
    static final String[] DESCS = {
            "Mas aceleracion y velocidad",
            "Mas combustible maximo",
            "Mas dano y cadencia de disparo",
            "Mas vidas al empezar",
            "Menos consumo de combustible"
    };

    /** Preferencias de Android; null cuando se usa el almacén en memoria (tests). */
    private final Preferences prefs;
    private final Map<String, Integer> memory = new HashMap<>();
    private final Map<String, String> memoryText = new HashMap<>();

    Upgrades() {
        this.prefs = Gdx.app.getPreferences("codegame");
    }

    /** Constructor sin Gdx, para pruebas de lógica en la JVM. */
    Upgrades(boolean inMemory) {
        this.prefs = null;
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

    int level(int index) {
        return getInt("lvl" + index);
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

    /** Precio actual de la recarga: 10, 20, 40... Tras 24 compras deja de duplicarse para no desbordar. */
    int refillCost() {
        return REFILL_BASE << Math.min(refillCount(), 24);
    }

    void recordRefill() {
        putInt("refills", refillCount() + 1);
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
        putInt("lvl" + index, level(index) + 1);
        return true;
    }

    float thrust() {
        return 260f + 60f * level(MOTOR);
    }

    float maxSpeed() {
        return 220f + 40f * level(MOTOR);
    }

    float maxFuel() {
        return 100f + 30f * level(DEPOSITO);
    }

    int damage() {
        return 1 + level(CANON);
    }

    float fireCooldown() {
        return 0.35f - 0.04f * level(CANON);
    }

    int lives() {
        return 3 + level(BLINDAJE);
    }

    /** Multiplicador del consumo de combustible: 1 sin mejora, 0.6 con nivel máximo. */
    float fuelUse() {
        return 1f - 0.08f * level(GASOLINA);
    }
}
