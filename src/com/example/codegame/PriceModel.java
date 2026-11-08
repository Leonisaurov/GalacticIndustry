package com.example.codegame;

/**
 * Modelo de precios del mercado. El precio medio depende del stock frente al
 * objetivo; compra y venta se separan con un spread configurable.
 *
 * <pre>factor = clamp(sqrt(target / max(stock, 1)), 0.50, 2.00)
 * medio  = basePrice * factor
 * venta  = medio * (1 - spread)
 * compra = medio * (1 + spread)</pre>
 */
final class PriceModel {

    static final float DEFAULT_SPREAD = 0.08f;
    static final float MIN_FACTOR = 0.50f;
    static final float MAX_FACTOR = 2.00f;
    static final float MIN_PRICE = 0.01f;

    private PriceModel() {
    }

    static float factor(int stock, int target) {
        float safeStock = Math.max(1, stock);
        float safeTarget = Math.max(1, target);
        float factor = (float) Math.sqrt(safeTarget / safeStock);
        return clamp(factor, MIN_FACTOR, MAX_FACTOR);
    }

    static float midPrice(float basePrice, int stock, int target) {
        return Math.max(MIN_PRICE, basePrice * factor(stock, target));
    }

    static float buyPrice(float basePrice, int stock, int target, float spread) {
        return Math.max(MIN_PRICE, midPrice(basePrice, stock, target)
                * (1f + clamp(spread, 0f, 0.5f)));
    }

    static float sellPrice(float basePrice, int stock, int target, float spread) {
        return Math.max(MIN_PRICE, midPrice(basePrice, stock, target)
                * (1f - clamp(spread, 0f, 0.5f)));
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }
}
