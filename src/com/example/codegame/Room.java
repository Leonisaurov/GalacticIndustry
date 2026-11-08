package com.example.codegame;

/** Habitación rectangular de un interior. Puede mapearse a un módulo. */
final class Room {

    final String id;
    final String name;
    final float x;
    final float y;
    final float width;
    final float height;
    /** Ranura de módulo asociada, o -1 para el núcleo. */
    final int moduleSlot;
    final ModuleDef module;

    Room(String id, String name, float x, float y, float width, float height,
         int moduleSlot, ModuleDef module) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = Math.max(40f, width);
        this.height = Math.max(40f, height);
        this.moduleSlot = moduleSlot;
        this.module = module;
    }

    boolean contains(float px, float py) {
        return px >= x - width * 0.5f && px <= x + width * 0.5f
                && py >= y - height * 0.5f && py <= y + height * 0.5f;
    }

    float centerX() {
        return x;
    }

    float centerY() {
        return y;
    }
}
