# Changelog

All notable changes to **ARGB Color Lens** are documented here.

The project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Planned
- More Java constant-expression formats, subject to safe PSI evaluation.

## [0.4.2] - 2026-10-17

### Fixed
- Replace the deprecated `ReadAction.compute(ThrowableComputable)` call with `ReadAction.computeBlocking` for the brief hover PSI lookup on the EDT, removing that deprecated API usage while retaining explicit read access.

## [0.4.1] - 2026-10-16

### Fixed
- Package the Gradle 9 signing task dependency and X.509 certificate decoding corrections for automated JetBrains Marketplace releases.
- No changes to plugin functionality compared with 0.4.0.

## [0.4.0] - 2026-10-16

### Added
- Settings page at **Settings → Tools → ARGB Color Lens**.
- Application-wide persistent option to enable or disable the hover expression highlight.
- Custom highlight color picker and an option to derive the highlight from each swatch.

### Fixed
- Perform hover smart-pointer, Java PSI, and document resolution inside an explicit read action, fixing `Read access is allowed from inside read-action only` exceptions in IntelliJ 2026.2+.

### Changed
- Hover tooltips now omit color values and source expressions while text highlighting is enabled; disabling highlighting restores the descriptive ARGB/reference labels.
- Tooltip labels show symbolic Java constant names only for references and do not duplicate literal hexadecimal values.
- Highlight backgrounds blend with the IDE theme for readability, even for fully transparent ARGB swatches.
- New settings and explanatory text are translated into English and Spanish.

## [0.3.0] - 2026-10-16

### Added
- Hover over an ARGB gutter icon to temporarily highlight its corresponding Java expression.
- Tooltips include both the ARGB value and source expression for easier identification.
- English and Spanish localization bundles, selected automatically from the IDE language.

### Changed
- Standardized all plugin-owned UI messages in resource bundles, with English fallback.
- Retained the left-click local editor, right-click constant editor, and alpha-aware native picker.

## [0.2.0] - 2026-10-16

### Added
- Custom gutter swatches with checkerboard transparency previews.
- **Left-click** to edit only the current expression.
- **Right-click** to edit the original `final` field when applicable.
- Native color picker with RGB and alpha controls.
- Smart PSI pointers to reacquire an expression after the picker closes.

### Changed
- Replaced the original `ElementColorProvider` with a custom Java line-marker provider to support distinct mouse actions.
- Changes are applied once after color-picker confirmation, rather than continuously replacing PSI while selecting colors.
- The picker uses **Choose**, instead of the misleading **Copy** action.
- Standardized project metadata for its first public version.

### Fixed
- Avoided relying on invalidated PSI references during color editing.
- Corrected Kotlin pointer variance and nullability issues encountered during development.
- Adjusted Linux CI to invoke the Gradle wrapper via Bash.

## [0.1.0] - 2026-10-16

### Added
- Initial prototype with ARGB hexadecimal swatches in Java files.
- Basic resolution of direct `final` Java field references.

[Unreleased]: https://github.com/LeonardoAliaga/ARGBColorLens/compare/v0.4.2...HEAD
[0.4.2]: https://github.com/LeonardoAliaga/ARGBColorLens/releases/tag/v0.4.2
[0.4.1]: https://github.com/LeonardoAliaga/ARGBColorLens/releases/tag/v0.4.1
[0.4.0]: https://github.com/LeonardoAliaga/ARGBColorLens/releases/tag/v0.4.0
[0.3.0]: https://github.com/LeonardoAliaga/ARGBColorLens/releases/tag/v0.3.0
[0.2.0]: https://github.com/LeonardoAliaga/ARGBColorLens/releases/tag/v0.2.0
