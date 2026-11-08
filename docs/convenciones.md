# Convenciones

- Comentarios, textos de UI y mensajes de test en **español**.
- La fuente `lsans-15` es ASCII: evita acentos y caracteres raros en textos de
  UI (`Menu`, `Canon`, `Deposito`…). Si hace falta un acento, comprueba antes que
  el glifo exista en el `.fnt`.
- Ángulos en grados, 0 = derecha, sentido antihorario.
- Acelerar y disparar consumen combustible; no rompas esa economía sin
  actualizar `SelfTest`.
- Para pruebas en dispositivo usa `tools/run-tests.sh` y ejecuta solo los tests
  necesarios; no lances `--all` por costumbre. La telemetría dice el estado
  real; las capturas son solo para diseño.
- Si cambias la duración o las fases de la muerte, ajusta los tests `05_death`
  y `08_visual_death`.
- No introduzcas asignaciones de `Color` ni de objetos en el bucle de render;
  usa constantes y campos reutilizables.
- Mantén el orden shapes → aditivo → texto y restaura la mezcla con
  `Visuals.normal()` + `beginShapes()` tras cada pasada aditiva.
- El piloto automático debe frenar al perder el objetivo (`STOP`); no lo
  canceles en seco dejando la inercia a máxima velocidad.
- No uses `new BitmapFont()`: los recursos de los jars no se empaquetan. La
  fuente vive en `assets/fonts/`.
- No documentes aquí: `AGENTS.md` es el índice; el detalle va en `docs/`.

## Accesibilidad (T-1106)

- Botones táctiles de al menos **44 u** de alto; los botones pequenos de mapa,
  mercado y gestion se han ampliado.
- Los avisos no dependen solo del color: usan prefijo `!` y texto explicito
  (`! O2 CRITICO`, `! SIN COMBUSTIBLE`).
- Los textos largos usan `textLeftFit`/`textCenteredFit` para reducir escala y
  evitar cortes en pantallas estrechas.
- La fuente sigue siendo ASCII; no se introducen acentos ni glifos ausentes.
- La localizacion completa a otros idiomas queda para mas adelante.
