# HostFlow · Paleta de colores

Paleta definida a partir de las capturas de referencia (**Booksy**). Se usa en todo el
frontend. Los valores viven como tokens de Tailwind en `src/styles/globals.css` (bloque
`@theme` para el modo claro y bloque `.dark` para el oscuro) y se consumen como utilidades
(`bg-lime`, `text-teal`, `border-line`, `bg-paper`, …).

## Neutros (modo claro · preseleccionado)

| Token | Variable CSS | Valor | Uso |
|---|---|---|---|
| Papel | `--color-paper` | `#FFFFFF` | Fondo principal |
| Nube | `--color-cloud` | `#F6F6F4` | Fondo de secciones / tarjetas apagadas |
| Tinta | `--color-ink` | `#111111` | Texto principal |
| Tinta suave | `--color-ink-soft` | `rgba(17,17,17,.72)` | Texto secundario |
| Apagado | `--color-muted` | `rgba(17,17,17,.5)` | Texto terciario / placeholders |
| Línea | `--color-line` | `rgba(17,17,17,.1)` | Bordes sutiles |
| Línea fuerte | `--color-line-strong` | `rgba(17,17,17,.2)` | Bordes en hover / inputs |
| Superficie | `--color-overlay` | `rgba(17,17,17,.04)` | Fondos de hover |
| Sombra | `--color-shadow` | `rgba(17,17,17,.12)` | Color de las sombras |

## Neutros (modo oscuro)

| Token | Variable CSS | Valor |
|---|---|---|
| Papel | `--color-paper` | `#111111` |
| Nube | `--color-cloud` | `#1A1A1A` |
| Tinta | `--color-ink` | `#F4F4F1` |
| Tinta suave | `--color-ink-soft` | `rgba(244,244,241,.72)` |
| Apagado | `--color-muted` | `rgba(244,244,241,.52)` |
| Línea | `--color-line` | `rgba(244,244,241,.1)` |
| Línea fuerte | `--color-line-strong` | `rgba(244,244,241,.22)` |
| Superficie | `--color-overlay` | `rgba(244,244,241,.05)` |
| Sombra | `--color-shadow` | `rgba(0,0,0,.55)` |

## Acentos (idénticos en ambos modos)

| Token | Variable CSS | Valor | Uso |
|---|---|---|---|
| Lima | `--color-lime` | `#D4F53C` | **Acción primaria**: botones, foco, destacados |
| Lima intenso | `--color-lime-strong` | `#B9E02C` | Hover de botones lima / gradientes |
| Lima suave | `--color-lime-soft` | `#EEFB9A` | Fondos de badges / highlights |
| Tinta sobre lima | `--color-lime-ink` | `#33400C` | Texto de botones lima |
| Teal | `--color-teal` | `#0FA3A3` | Acción secundaria, datos, enlaces |
| Teal intenso | `--color-teal-strong` | `#0B8787` | Hover de teal, enlaces de texto |
| Teal suave | `--color-teal-soft` | `rgba(15,163,163,.12)` | Fondos de estados / chips |
| Rosa | `--color-pink` | `#F4B8C4` | Acentos suaves (precios, estados) |
| Rosa intenso | `--color-pink-strong` | `#E892A6` | Hover de rosa |
| Rosa suave | `--color-pink-soft` | `rgba(244,184,196,.2)` | Fondos suaves |
| Peligro | `--color-danger` | `#E5484D` | Errores |
| Éxito | `--color-success` | `#12A150` | Confirmaciones |

## Muestras

| | Lima | Teal | Rosa | Carbón | Papel |
|---|---|---|---|---|---|
| Hex | `#D4F53C` | `#0FA3A3` | `#F4B8C4` | `#111111` | `#FFFFFF` |
| RGB | 212, 245, 60 | 15, 163, 163 | 244, 184, 196 | 17, 17, 17 | 255, 255, 255 |

## Contraste

- Botón primario: `--color-lime` (`#D4F53C`) + texto `--color-lime-ink` (`#33400C`) → contraste alto.
- Texto sobre fondo `--color-paper`: `--color-ink` → contraste ≥ 15:1.
- Teal sobre blanco: `#0FA3A3` → ~4.6:1 (texto grande / iconos). Para texto pequeño usar
  `--color-teal-strong` `#0B8787`.

## Modo claro / oscuro

1. El **modo claro** es el preseleccionado (`localStorage` → `hostflow.theme`).
2. La clase `.dark` se agrega/quita en `<html>` desde `src/lib/theme/ThemeContext.jsx`.
3. `.dark` re-declara los tokens `--color-*`, así que todo el sistema (incluidos los formularios,
   que usan `appearance-none` para no heredar el estilo nativo) cambia de golpe.

## Reglas de uso

1. **Un acento primario por pantalla**: el lima se reserva para la acción principal.
2. El teal acompaña a datos, enlaces y acciones secundarias.
3. El rosa se usa con moderación: precios, avisos suaves y detalles decorativos.
4. Nunca poner texto oscuro sobre lima con bajo contraste: usar siempre `--color-lime-ink`.
5. Respetar `prefers-reduced-motion` (ya gestionado en `src/styles/globals.css`).
6. Los colores de marca del alojamiento (Perfil → Personalización visual) son independientes del
   tema: se mantienen iguales en modo claro y oscuro.
