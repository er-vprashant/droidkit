# Changelog

All notable changes to DroidKit will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Network Inspector with HTTP/HTTPS traffic interception
- Mock response system for testing API endpoints offline
- JSON beautification toggle in network detail view
- Copy as cURL functionality for network requests
- Filter network calls by HTTP method (GET, POST, PUT, DELETE)
- Network call detail view with Overview, Request, and Response tabs
- Mock editor with URL pattern matching (exact and wildcard)
- Network request/response body capture (up to 100KB)
- Request duration tracking
- Support for custom headers in mock responses
- Network delay simulation for mock responses

### Changed
- Updated README with Network Inspector documentation
- Optimized CI workflow for cost reduction (~40% faster, 60% fewer steps)
- Added Gradle caching to CI for faster builds
- Added concurrency control to cancel redundant CI runs
- Added 30-minute timeout protection for CI jobs

### Infrastructure
- Added CODEOWNERS file for automated review requests
- Added branch protection guidelines and release workflow documentation
- Created CHANGELOG.md for release tracking

### Fixed
- Beautify button state now persists across tab switches
- Increased network body capture limit from 10KB to 100KB to handle larger JSON responses

## [1.0.0] - TBD

### Added
- Initial release
- Storage Inspector for SharedPreferences and SQLite databases
- Deep Link Tester with history and presets
- Push Notification Tester with live preview
- Shake to open functionality
- Persistent notification launcher
- Auto-initialization via ContentProvider
- Zero-config setup with `debugImplementation`
- Material 3 UI with dark theme
- Sample app demonstrating all features

### Architecture
- Built with Jetpack Compose
- Manual dependency injection (no Hilt requirement)
- Debug-only source set for zero release footprint
- Internal preferences isolation
- Ring buffer storage for efficient memory usage

---

## Release Notes Template

Use this template for future releases:

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features and capabilities

### Changed
- Changes to existing functionality

### Deprecated
- Features marked for removal in future versions

### Removed
- Features removed in this version

### Fixed
- Bug fixes

### Security
- Security vulnerability fixes
```

## Versioning Guidelines

- **MAJOR** (X.0.0): Breaking API changes
- **MINOR** (0.X.0): New features, backward compatible
- **PATCH** (0.0.X): Bug fixes, backward compatible

## Link Format

[Unreleased]: https://github.com/er-vprashant/droidkit/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/er-vprashant/droidkit/releases/tag/v1.0.0
