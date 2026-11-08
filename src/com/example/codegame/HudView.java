package com.example.codegame;

/**
 * Estado efímero que MyGame entrega al HUD de partida en cada fotograma.
 * Se reutiliza para no crear objetos en render().
 */
final class HudView {

    int state;
    boolean runActive;
    boolean canRecover;
    float time;
    float throttle;

    float joyCx;
    float joyCy;
    float joyR;
    float knobX;
    float knobY;
    boolean joyAiming;

    Button pause;
    Button thrust;
    Button recover;
    Button dock;
    boolean canDock;
    Button rescue;
    boolean canRescue;
    Button map;
    boolean canOpenMap;
    Button radar;
    boolean canOpenRadar;
    Button buildBase;
    boolean canBuildBase;
    boolean rescueActive;
    float rescueSeconds;

    /** Brújula básica: dirección relativa a la nodriza más cercana. */
    boolean compassValid;
    boolean compassLocal;
    boolean compassBehind;
    boolean compassHere;
    float compassAngle;
    float compassDistance;
    String compassLabel = "";
    /** Brújula avanzada (módulo NAVEGACION). */
    boolean hasNavigation;
    NavTarget navTarget;
    /** Guiado de atraque manual (UPD-006-T06). */
    boolean dockGuide;
    boolean inDockCorridor;
    float dockDistance;
    float dockLateral;
    float dockAngle;
    float dockSpeed;
}
