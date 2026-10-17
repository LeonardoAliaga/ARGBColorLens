# Manual testing checklist (0.4.0)

Open a **full Java project** inside the sandbox IntelliJ IDEA started by `runIde`.

```java
public final class PaletteDemo {
    static final int BASE = 0xFF212121;
    static final int TRANSPARENT = 0x804995B6;

    static void draw(int color) { }

    static void test() {
        draw(0x00000000);
        draw(BASE);
        draw(BASE);
        draw(TRANSPARENT);
    }
}
```

- [ ] Each ARGB literal shows a gutter swatch.
- [ ] `BASE` and `TRANSPARENT` usages resolve and show swatches.
- [ ] The semitransparent swatch displays a checkerboard underneath the color.
- [ ] Left-click on one usage of `BASE`, choose a different color, confirm. Only that occurrence is replaced by a hexadecimal literal; other uses still say `BASE`.
- [ ] Undo and redo work for the single usage.
- [ ] Right-click on `BASE`, choose a different color, confirm. The field initializer changes; all usages keep referring to `BASE`.
- [ ] Right-click on a direct literal changes only that literal.
- [ ] The color picker offers opacity and confirms with **Choose** (not Copy).
- [ ] Cancelling either selector leaves the code unchanged.
- [ ] Reopen the selector after an edit; no `PsiInvalidElementAccessException` occurs.
- [ ] A non-ARGB integer such as `0xFF` is not recognized as a color.
- [ ] Multiple colors on one physical source line are checked for gutter icon grouping.

These manual checks are required; building alone does not confirm the UI works.

## Hover and localization (0.3.0)

- [ ] Hover the swatch for `BASE`: the exact `BASE` expression in the editor is temporarily highlighted.
- [ ] Hover the swatch for `TRANSPARENT`: the source expression is highlighted and the tooltip includes its name.
- [ ] Move the pointer out of the swatch/gutter: the highlight disappears without editing code.
- [ ] Hover different swatches on a single source line where they remain individually accessible: each highlights the associated expression, not an adjacent color.
- [ ] Verify IntelliJ's grouping behavior for multiple icons on the same line; grouped icons may require additional UI in a future version.
- [ ] Switch the IDE to English and restart: swatch tooltips and picker captions are in English.
- [ ] Switch the IDE to Spanish and restart: swatch tooltips and picker captions are in Spanish.
- [ ] Use another locale: plugin-owned text falls back to English.
- [ ] Left/right click actions still modify only the intended usage/definition, and Cancel does not change code.
- [ ] Repeated hover/edit/undo does not produce invalid PSI exceptions, leaks, or a highlight that remains after closing an editor.

## Settings and tooltips (0.4.0)

- [ ] Settings → Tools → ARGB Color Lens appears in a project and in application settings.
- [ ] Uncheck **Highlight the source expression on gutter swatch hover**, click Apply: the previous highlight disappears and subsequent hovers do not highlight source text; both click actions still work.
- [ ] Re-enable highlighting and disable **Use the corresponding ARGB swatch color**. Choose a custom color, click Apply, and verify the source highlight uses that shade.
- [ ] Choose **Use the corresponding ARGB swatch color**, click Apply. Hover distinctly colored swatches and verify that each expression highlights with a matching tint.
- [ ] With a fully transparent literal `0x00000000`, the highlight remains visible enough to identify the source expression.
- [ ] With either mode selected, the highlight is removed on mouse exit and is never written into the Java source.
- [ ] Apply settings, restart IntelliJ, and confirm they persist across projects and IDE restarts.
- [ ] Change settings then press Cancel: the previous saved settings remain active.
- [ ] In Settings, **Reset** restores the persisted values; the custom chooser is disabled in automatic-color mode.
- [ ] Hover a reference such as `UiTheme.TEXT_PRIMARY`: tooltip contains both its color value and symbolic reference.
- [ ] Hover a direct hexadecimal literal: tooltip contains the value once, not as `0xFF212121 — 0xFF212121`.
- [ ] Verify the Settings labels and tooltip behavior in both English and Spanish IDE locales.
- [ ] Confirm left-click / right-click color editing is unaffected.

## Hover read-access regression (v0.4.0)

- [ ] Hover literal and constant swatches repeatedly without a `Read access is allowed from inside read-action only` error in `idea.log`.
- [ ] Switch between Java tabs and hover immediately after opening a file; no `PsiInvalidElementAccessException` occurs.
- [ ] Highlight remains temporary and disappears on gutter mouse exit.
- [ ] Switching custom/matching highlight modes, disabling hover highlighting, and both color edit buttons still work.
