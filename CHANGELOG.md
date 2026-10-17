# Changelog

All notable changes to **ARGB Color Lens** are documented here.

The project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Planned
- Configurable preferences for swatch behavior and visual appearance.
- More Java constant-expression formats, subject to safe PSI evaluation.

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

[Unreleased]: https://github.com/LeonardoAliaga/ARGBColorLens/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/LeonardoAliaga/ARGBColorLens/releases/tag/v0.3.0
[0.2.0]: https://github.com/LeonardoAliaga/ARGBColorLens/releases/tag/v0.2.0
