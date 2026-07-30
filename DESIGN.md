---
name: RvxMobile
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
  on-surface: '#e5e2e1'
  on-surface-variant: '#c7c4d7'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#908fa0'
  outline-variant: '#464554'
  surface-tint: '#c0c1ff'
  primary: '#c0c1ff'
  on-primary: '#1000a9'
  primary-container: '#8083ff'
  on-primary-container: '#0d0096'
  inverse-primary: '#494bd6'
  secondary: '#44e2cd'
  on-secondary: '#003731'
  secondary-container: '#03c6b2'
  on-secondary-container: '#004d44'
  tertiary: '#ddb7ff'
  on-tertiary: '#490080'
  tertiary-container: '#b76dff'
  on-tertiary-container: '#400071'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e1e0ff'
  primary-fixed-dim: '#c0c1ff'
  on-primary-fixed: '#07006c'
  on-primary-fixed-variant: '#2f2ebe'
  secondary-fixed: '#62fae3'
  secondary-fixed-dim: '#3cddc7'
  on-secondary-fixed: '#00201c'
  on-secondary-fixed-variant: '#005047'
  tertiary-fixed: '#f0dbff'
  tertiary-fixed-dim: '#ddb7ff'
  on-tertiary-fixed: '#2c0051'
  on-tertiary-fixed-variant: '#6900b3'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
typography:
  display-lg:
    fontFamily: Hanken Grotesk
    fontSize: 57px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  title-lg:
    fontFamily: Hanken Grotesk
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  code-sm:
    fontFamily: Geist
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 16px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  edge_margin: 20px
  gutter: 12px
---

## Brand & Style

The design system is engineered for a high-end, technical utility experience. It merges the structural logic of **Material 3** with a **premium dark-mode aesthetic**, focusing on precision and efficiency. The style leverages "Glass-Material" hybridity—where the rigorous grid and functional components of Jetpack Compose are elevated through subtle translucency, deep obsidian layering, and vibrant kinetic accents.

The target audience consists of power users who value performance and a sophisticated "pro" toolset. The UI evokes a sense of "Electric Utility": calm and unobtrusive during passive states, but energetic and responsive during interaction.

**Design Style: Modern Corporate / Glassmorphism Hybrid**
- **Material Foundations:** Adherence to M3 layout logic and interaction patterns.
- **Atmospheric Depth:** Usage of background blurs and tonal stacking to create a high-end feel.
- **Precision:** High-contrast accents against dark surfaces to guide the eye to critical actions.

## Colors

The palette is optimized for OLED displays and high-luminance accessibility. The primary theme is **Dark**, utilizing a tiered grayscale for depth.

- **Primary (Electric Indigo):** Used for key actions, active states, and focus indicators.
- **Secondary (Teal):** Used for success states, data visualizations, and secondary utility toggles.
- **Neutral / Surfaces:** The base is a deep `Obsidian`. Elevated surfaces transition into `Charcoal`, ensuring that glass effects have enough contrast to remain legible.
- **Luminance-Based Theming:** In Light Mode (fallback), the Electric Indigo deepens to `#4F46E5`, and surfaces shift to a clean, high-luminance `#F8FAFC` to maintain the premium feel without losing brand identity.

## Typography

Typography balances technical clarity with modern character.

- **Headlines:** **Hanken Grotesk** provides a clean, geometric structure that feels more "pro" and modern than standard system fonts.
- **Body:** **Inter** is used for maximum legibility in utility contexts, handling data-heavy screens with ease.
- **Labels/Technical Data:** **Geist** (a developer-centric mono-influence sans) is used for labels, buttons, and status indicators to reinforce the technical nature of the app.

Scale large headlines down on mobile devices to prevent excessive wrapping. Use medium weights for titles to ensure they stand out against vibrant glass backgrounds.

## Layout & Spacing

This design system follows a **fluid, 8dp-based grid** consistent with Jetpack Compose development standards.

- **Margins:** Standard screen margins are set at `20px` to give the UI a more premium, airy feel compared to the standard 16dp.
- **Gutters:** Internal spacing between card elements uses `12px` to keep related utilities grouped but distinct.
- **Layout Model:** Use a 4-column grid for mobile and an 8-column grid for tablets. Content should be contained within high-radius cards that stretch horizontally to the edge margins.

## Elevation & Depth

The design system rejects traditional "flat" Material design in favor of **Tonal & Glass Layering**:

1.  **Level 0 (Base):** Obsidian `#0A0A0B`.
2.  **Level 1 (Cards):** Charcoal `#1C1C1E` with a 1px inner stroke of 10% white to define edges.
3.  **Level 2 (Active/Glass):** Surfaces use a background blur (20px - 40px) and 60% opacity of the surface color.
4.  **Shadows:** High-quality, multi-layered shadows. Use a "soft-glow" shadow for primary buttons: `0px 10px 20px rgba(99, 102, 241, 0.2)`.

Depth is primarily communicated via color luminance—higher elevation elements are lighter/more translucent.

## Shapes

The shape language is characterized by "Hyper-Radii." To achieve the high-end utility feel, shapes are significantly more rounded than standard industry defaults.

- **Primary Containers:** `28px` corner radius.
- **Buttons & Chips:** Fully pill-shaped (`rounded-full`) or `24px` radius.
- **Small Elements (Inputs/Checkboxes):** `12px` radius.

The extreme roundedness softens the technical nature of the app, making the "Electric Indigo" accents feel integrated rather than aggressive.

## Components

### Buttons
- **Primary:** Gradient-filled (Indigo to Purple) or solid Electric Indigo. Pill-shaped. Subtle inner glow on top edge.
- **Secondary:** Transparent with a 1.5px Teal border.
- **Ghost:** No background, Geist-font labels in Teal or White.

### Cards & Surfaces
- Surfaces must use a 1px border (`#FFFFFF` at 8% opacity) to ensure separation from the obsidian background.
- Apply a `Modifier.blur()` or `BackdropFilter` for top-level headers and navigation bars to create the glassmorphism effect.

### Inputs
- **Text Fields:** Filled style with a very dark grey background (`#161618`). Bottom indicator line is replaced by a full-border focus state in Electric Indigo.
- **Selection:** Use M3-style Switch components but tinted with the Teal secondary color for the "on" state.

### Lists
- Items should be separated by space rather than dividers.
- List items feature a `24px` radius when pressed (ripple effect should be centered and subtle).

### Chips
- Compact, pill-shaped markers for filtering. Use high-contrast backgrounds (Teal with black text) for active states.