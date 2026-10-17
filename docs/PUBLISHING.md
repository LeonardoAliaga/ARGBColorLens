# Publishing ARGB Color Lens

This project targets **IntelliJ IDEA 2026.2+ / IntelliJ Platform build 262+**, is written in Kotlin and relies on the bundled Java plugin.

## Before publishing

1. Confirm that the `main` branch contains version **0.4.0** and that its CI build is green.
2. Run `compileKotlin`, `check`, `buildPlugin`, and `verifyPlugin` using JDK **25**.
3. Run `runIde` and complete the [manual test checklist](TESTING.md), including the distinction between left and right click and Cancel behavior.
4. Inspect the generated `build/distributions/*.zip`, including the packaged `META-INF/plugin.xml`, plugin name/version/vendor and icon.
5. Prepare at least one genuine screenshot of the plugin working on a Java file. Do not present conceptual UI mockups as screenshots.
6. Review the **MIT** licensing choice before publishing, and check that third-party code/assets are compatible.

## First JetBrains Marketplace upload (manual)

Per JetBrains' official instructions, **the first plugin publication must be uploaded manually**.

1. Sign in at https://plugins.jetbrains.com/ with your JetBrains account.
2. Choose **Add new plugin**, upload the verified plugin ZIP and complete the listing requirements.
3. Display name: **ARGB Color Lens**; permanent ID: `io.github.leonardoaliaga.argbcolorlens`.
4. Vendor: **Leonardo Aliaga**. Homepage/source: https://github.com/LeonardoAliaga/ARGBColorLens.
5. Description and release notes are declared in `src/main/resources/META-INF/plugin.xml`.
6. Select the appropriate free plugin/license settings and provide screenshot(s).
7. Follow the signing requirements presented by Marketplace. Keep all signing keys, certificates, and Marketplace tokens **out of Git**.
8. Submit the listing and wait for JetBrains' verification and review.

## Building locally

Windows PowerShell:

```powershell
.\gradlew.bat clean check buildPlugin verifyPlugin
```

Linux/macOS:

```bash
bash ./gradlew clean check buildPlugin verifyPlugin
```

The installable ZIP is in `build/distributions/`. To test it, use **Settings → Plugins → Install Plugin from Disk** in a separate IDE instance.

## Future releases

- Bump `version` in `gradle.properties` using semantic versioning.
- Update `CHANGELOG.md` and the `<change-notes>` entry in `plugin.xml`.
- Build, test and verify before creating a Git tag (for example `v0.2.0`).
- After the first manual Marketplace release, optional automation can be configured securely using JetBrains' `publishPlugin` and signing tasks. The repository intentionally **does not publish automatically** on a GitHub release.

Official references:
- https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html
- https://plugins.jetbrains.com/docs/intellij/plugin-signing.html
- https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html
