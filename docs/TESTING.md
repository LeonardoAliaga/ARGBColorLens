# Manual testing checklist (0.2.0)

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
