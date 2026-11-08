package com.example.codegame;

/** Entrada abstracta del interior: vector de movimiento e intención de salir. */
final class InteriorInput {

    float moveX;
    float moveY;
    boolean exit;

    void set(float x, float y) {
        moveX = x;
        moveY = y;
    }

    void reset() {
        moveX = 0f;
        moveY = 0f;
        exit = false;
    }
}
