package com.example.codegame;

/** Entrada de un fotograma: giro (teclado), rumbo del joystick, nivel del acelerador y disparo. */
class Controls {

    /** Giro por teclado: +1 izquierda, -1 derecha, 0 sin giro. */
    float turn;
    /** True mientras el joystick está fuera del centro: la nave gira hacia {@link #aimDeg}. */
    boolean aiming;
    /** Rumbo del joystick en grados (0 = derecha, sentido antihorario). */
    float aimDeg;
    /** Nivel del acelerador entre 0 y 1. */
    float thrust;
    /** Disparar en este fotograma. */
    boolean fire;
}
