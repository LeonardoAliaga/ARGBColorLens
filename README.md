# ARGB Color Lens

**Preview and edit ARGB colors directly in Java code in IntelliJ IDEA.**

ARGB Color Lens adds a color swatch to the editor gutter for Java literals written as `0xAARRGGBB`, including references to `final` fields initialized with an ARGB literal. You can choose whether to change an individual usage or the original color definition.

[![Version](https://img.shields.io/badge/version-0.4.3-blue)](https://github.com/LeonardoAliaga/ARGBColorLens)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![IntelliJ](https://img.shields.io/badge/IntelliJ_Platform-2026.2%2B-7F52FF)](https://www.jetbrains.com/idea/)

## Features

- **ARGB previews:** gutter swatches for `0xAARRGGBB` values in Java, including the alpha (transparency) channel.
- **Single preview per expression:** redundant native swatches for a handled ARGB literal or its variable declaration are hidden; the plugin's hover and mouse actions remain available.
- **Configurable hover highlighting:** optionally disable highlighting, select a custom highlight color, or match each color swatch automatically. Settings persist across IDE restarts.
- **Context-sensitive tooltips:** with hover highlighting enabled, the tooltip stays short and shows only mouse-action hints, without hexadecimal values or source expressions. With highlighting disabled, it shows the ARGB value and the symbolic name for references such as `UiTheme.TEXT_PRIMARY`; literals are shown only once.
- **Automatic localization:** English is the default; Spanish is used automatically when the IDE language is Spanish. Other languages fall back to English.
- **Reference awareness:** resolves usages such as `UiTheme.TEXT_PRIMARY` when the referenced `final` field has a direct ARGB literal initializer.
- **Left-click — local edit:** choose a color and replace only that expression with an ARGB literal.
- **Right-click — definition edit:** choose a color and change the source field's initializer, preserving its usages.
- **Transparent preview:** checkerboard background beneath semitransparent swatches.
- **Native color picker:** RGB and opacity controls provided by IntelliJ IDEA. Changes are applied after confirmation; cancel leaves code untouched.

### Example

```java
public final class UiTheme {
    public static final int TEXT_PRIMARY = 0xFF212121;
    public static final int BG_WELCOME = 0x804995B6;
}

// In another Java class:
render(0x00000000, UiTheme.BG_WELCOME, UiTheme.TEXT_PRIMARY);
```

Clicking a swatch associated with `UiTheme.TEXT_PRIMARY`:

| Action | Behavior |
| --- | --- |
| Hover | Highlight the corresponding source expression temporarily (no source modifications) |
| Left-click | Replace that usage with a chosen `0xAARRGGBB` value |
| Right-click | Update `UiTheme.TEXT_PRIMARY` at its declaration |
| Cancel | Leave the source code unchanged |

For a direct hexadecimal literal, either click edits that literal.

## Highlight settings

Open **Settings → Tools → ARGB Color Lens** (on macOS: **Preferences → Tools → ARGB Color Lens**). These settings apply across all projects.

- **Highlight the source expression on gutter swatch hover** — uncheck to disable highlighting completely. Color editing remains available.
- **Use the corresponding ARGB swatch color** — automatically derives the highlight from each color swatch, including constants such as `UiTheme.TEXT_PRIMARY`.
- **Custom highlight color** — when the automatic checkbox is off, select a specific color using IntelliJ's native color chooser.

Changes take effect when you click **Apply** or **OK**, without restarting the IDE. With highlighting enabled, hover tooltips omit ARGB values and source expressions to stay compact; disabling highlighting restores those details. A subtle background and outline are blended with the editor theme for legibility; the highlight uses the swatch RGB channels even if the original ARGB alpha is fully transparent.

## Compatibility and limitations

- **Target:** IntelliJ IDEA using IntelliJ Platform **2026.2 (build 262) or newer**.
- **Language analyzed:** Java. The plugin implementation itself is in Kotlin.
- Supports **eight-digit** hexadecimal integer literals `0xAARRGGBB` and **direct initializers** of `final` Java fields.
- Does not yet evaluate computed colors, chained constants, CSS values, `Color` objects, or arbitrary expressions.
- IntelliJ may group gutter icons when multiple color expressions share a line. Hover highlighting works for individually accessible swatches; IDE-managed grouped icons are a remaining limitation.
- Standalone files opened without a full project may have reduced PSI/reference resolution support.

## Install

Once published, install from **Settings → Plugins → Marketplace** by searching for **ARGB Color Lens**.

For a locally built ZIP:

1. Run `./gradlew buildPlugin` (Windows: `.\gradlew.bat buildPlugin`).
2. Open **Settings → Plugins → ⚙ → Install Plugin from Disk**.
3. Select the ZIP in `build/distributions/`.

## Build and contribute

Requirements: **JDK 25**, the Gradle wrapper bundled with this repository, and internet access to fetch IntelliJ Platform dependencies.

```powershell
.\gradlew.bat compileKotlin
.\gradlew.bat runIde
.\gradlew.bat buildPlugin
.\gradlew.bat verifyPlugin
```

On macOS and Linux, use `bash ./gradlew` instead of `.\gradlew.bat`.

On a new version in `main`, GitHub Actions automatically builds and creates a **GitHub Release** with the plugin ZIP; Marketplace publishing is optional and requires secure setup. See [publishing guide](docs/PUBLISHING.md).

Test changes inside the **sandbox IDE** opened by `runIde` before installing them in your main IDE. Please open an [issue](https://github.com/LeonardoAliaga/ARGBColorLens/issues) for bugs or feature requests. See [publication checklist](docs/PUBLISHING.md) and [test checklist](docs/TESTING.md).

## Español

**ARGB Color Lens** permite previsualizar colores Java `0xAARRGGBB` y constantes como `UiTheme.TEXT_PRIMARY` directamente desde el editor.

- **Clic izquierdo:** modifica solo el uso seleccionado.
- **Clic derecho:** modifica el color original de la constante.
- **Hover:** resalta temporalmente la expresión Java correspondiente; puede desactivarse y personalizarse su color desde **Settings → Tools → ARGB Color Lens**.
- **Idioma:** la interfaz del plugin se adapta automáticamente al inglés o español del IDE.
- **Tooltips:** con el resaltado activado solo muestran indicaciones breves de los clics. Al desactivar el resaltado vuelven a mostrar el ARGB y, si corresponde, la referencia como `UiTheme.TEXT_PRIMARY`, sin repetir los literales.
- Se conserva la transparencia ARGB y se utiliza el selector de colores de IntelliJ.

La versión **0.4.3** evita los cuadros de color duplicados en literales y declaraciones, conservando las acciones del plugin. La **0.4.0** incorpora la configuración de resaltado y mejoras en los tooltips. Consulta [CHANGELOG.md](CHANGELOG.md) para conocer los cambios.

## Author and links

Developed by **Leonardo Aliaga**.

- GitHub: [@LeonardoAliaga](https://github.com/LeonardoAliaga)
- Instagram: [@aliaga1924](https://www.instagram.com/aliaga1924/)
- Issues and feedback: [GitHub Issues](https://github.com/LeonardoAliaga/ARGBColorLens/issues)

Released under the [MIT License](LICENSE). Independent community plugin; not affiliated with JetBrains.
