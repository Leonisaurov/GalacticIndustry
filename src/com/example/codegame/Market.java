package com.example.codegame;

import java.util.EnumMap;
import java.util.Map;

/**
 * Mercado de una estación: stock y objetivo por mineral, con precios de compra
 * y venta derivados del stock. Los objetivos y el spread son configurables por
 * estación. La producción/consumo y las transacciones llegan en T-602/T-604.
 */
final class Market {

    static final int DEFAULT_TARGET = 100;

    private final EnumMap<Commodity, Integer> stock = new EnumMap<>(Commodity.class);
    private final EnumMap<Commodity, Integer> target = new EnumMap<>(Commodity.class);
    private float spread = PriceModel.DEFAULT_SPREAD;

    Market() {
        this(null, 1f);
    }

    /**
     * @param targets         objetivo por mineral; los que falten usan
     *                        {@link #DEFAULT_TARGET}.
     * @param initialFraction fracción del objetivo con la que arranca el stock.
     */
    Market(Map<Commodity, Integer> targets, float initialFraction) {
        float fraction = Math.max(0f, initialFraction);
        for (Commodity commodity : Commodity.values()) {
            int targetValue = targets == null ? DEFAULT_TARGET
                    : Math.max(1, targets.getOrDefault(commodity, DEFAULT_TARGET));
            target.put(commodity, targetValue);
            stock.put(commodity, Math.max(0, Math.round(targetValue * fraction)));
        }
    }

    int stock(Commodity commodity) {
        Integer value = stock.get(commodity);
        return value == null ? 0 : value;
    }

    int target(Commodity commodity) {
        Integer value = target.get(commodity);
        return value == null ? DEFAULT_TARGET : value;
    }

    void setStock(Commodity commodity, int value) {
        if (commodity != null) {
            stock.put(commodity, Math.max(0, value));
        }
    }

    void setTarget(Commodity commodity, int value) {
        if (commodity != null) {
            target.put(commodity, Math.max(1, value));
        }
    }

    /** Suma (o resta) stock; nunca baja de cero. Devuelve el stock resultante. */
    int addStock(Commodity commodity, int delta) {
        int next = Math.max(0, stock(commodity) + delta);
        setStock(commodity, next);
        return next;
    }

    float spread() {
        return spread;
    }

    void setSpread(float spread) {
        this.spread = Math.max(0f, Math.min(0.5f, spread));
    }

    float buyPrice(Commodity commodity) {
        return PriceModel.buyPrice(commodity.basePrice, stock(commodity),
                target(commodity), spread);
    }

    float sellPrice(Commodity commodity) {
        return PriceModel.sellPrice(commodity.basePrice, stock(commodity),
                target(commodity), spread);
    }

    float midPrice(Commodity commodity) {
        return PriceModel.midPrice(commodity.basePrice, stock(commodity),
                target(commodity));
    }
}
