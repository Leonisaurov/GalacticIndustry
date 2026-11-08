package com.example.codegame;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Bodega de la nave. La capacidad se mide en masa (UM), no en unidades: cada
 * mineral ocupa su propia masa. La API rechaza el exceso y permite consultar,
 * añadir y quitar.
 */
class CargoHold {

    static final float DEFAULT_CAPACITY = 100f;
    private static final float EPSILON = 0.0001f;

    private final EnumMap<Commodity, Integer> items = new EnumMap<>(Commodity.class);
    private float capacity;

    CargoHold() {
        this(DEFAULT_CAPACITY);
    }

    CargoHold(float capacity) {
        this.capacity = Math.max(0f, capacity);
    }

    float capacity() {
        return capacity;
    }

    /** Cambia la capacidad; no elimina carga ya existente. */
    void setCapacity(float capacity) {
        this.capacity = Math.max(0f, capacity);
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    int amount(Commodity commodity) {
        if (commodity == null) {
            return 0;
        }
        Integer value = items.get(commodity);
        return value == null ? 0 : value;
    }

    float massOf(Commodity commodity) {
        return amount(commodity) * (commodity == null ? 0f : commodity.massPerUnit);
    }

    /** Masa ocupada actualmente, en UM. */
    float usedMass() {
        float total = 0f;
        for (Map.Entry<Commodity, Integer> entry : items.entrySet()) {
            total += entry.getValue() * entry.getKey().massPerUnit;
        }
        return total;
    }

    /** Masa libre, nunca negativa. */
    float freeMass() {
        return Math.max(0f, capacity - usedMass());
    }

    boolean overCapacity() {
        return usedMass() > capacity + EPSILON;
    }

    /** Indica si la cantidad completa cabe en la bodega. */
    boolean canAdd(Commodity commodity, int amount) {
        if (commodity == null || amount <= 0) {
            return amount == 0;
        }
        float needed = amount * commodity.massPerUnit;
        return needed <= freeMass() + EPSILON;
    }

    /**
     * Añade lo que quepa y devuelve la cantidad aceptada. Si no cabe nada,
     * devuelve 0. Para exigir que quepa todo, usa {@link #addExact}.
     */
    int add(Commodity commodity, int amount) {
        if (commodity == null || amount <= 0) {
            return 0;
        }
        int fit = (int) Math.floor((freeMass() + EPSILON) / commodity.massPerUnit);
        int accepted = Math.min(amount, Math.max(0, fit));
        if (accepted <= 0) {
            return 0;
        }
        items.put(commodity, amount(commodity) + accepted);
        return accepted;
    }

    /** Añade la cantidad completa o no modifica la bodega. */
    boolean addExact(Commodity commodity, int amount) {
        if (!canAdd(commodity, amount)) {
            return false;
        }
        add(commodity, amount);
        return true;
    }

    /** Quita hasta la cantidad disponible y devuelve lo retirado. */
    int remove(Commodity commodity, int amount) {
        if (commodity == null || amount <= 0) {
            return 0;
        }
        int current = amount(commodity);
        int removed = Math.min(current, amount);
        if (removed <= 0) {
            return 0;
        }
        if (removed == current) {
            items.remove(commodity);
        } else {
            items.put(commodity, current - removed);
        }
        return removed;
    }

    /** Quita la cantidad completa o no modifica la bodega. */
    boolean removeExact(Commodity commodity, int amount) {
        if (amount < 0 || amount(commodity) < amount) {
            return false;
        }
        if (amount > 0) {
            remove(commodity, amount);
        }
        return true;
    }

    /**
     * Restaura una cantidad desde un guardado sin aplicar la capacidad actual.
     * Sirve para conservar el inventario si la capacidad cambia entre versiones.
     */
    void restore(Commodity commodity, int amount) {
        if (commodity == null || amount <= 0) {
            items.remove(commodity);
        } else {
            items.put(commodity, amount);
        }
    }

    /** Copia ordenada de las entradas para UI, guardado o mercado. */
    List<Cargo> entries() {
        List<Cargo> result = new ArrayList<>();
        for (Commodity commodity : Commodity.values()) {
            int value = amount(commodity);
            if (value > 0) {
                result.add(new Cargo(commodity, value));
            }
        }
        return result;
    }

    /** Valor base total de la bodega, en créditos. */
    float totalValue() {
        float total = 0f;
        for (Map.Entry<Commodity, Integer> entry : items.entrySet()) {
            total += entry.getValue() * entry.getKey().basePrice;
        }
        return total;
    }

    void clear() {
        items.clear();
    }
}
