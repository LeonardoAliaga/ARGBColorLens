# Publishing ARGB Color Lens

This project targets **IntelliJ IDEA 2026.2+ / IntelliJ Platform build 262+**, is written in Kotlin and relies on the bundled Java plugin.

## Before publishing

1. Confirm that the `main` branch contains version **0.4.1** and that its CI build is green.
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

## Automated releases from `main`

The source of truth is `version=...` in `gradle.properties`. The workflow
`.github/workflows/release.yml` runs on every push to `main`, but only builds and
creates a public **GitHub Release** when the corresponding `v<version>` tag does not
yet exist. It runs `check buildPlugin verifyPlugin` before tagging, then attaches
the distributable ZIP. Further commits with the same version do not create
duplicate releases.

**Increase the version only after the release is ready.** An existing Git tag
must not be reused for new plugin code. Before merging a new version, complete
`docs/TESTING.md` in the sandbox IDE; GitHub Actions does not reproduce UI mouse events.

### JetBrains Marketplace credentials (manual setup, never commit secrets)

The Marketplace listing must already exist (first upload was manual).
Create a personal Marketplace token under **JetBrains Marketplace → Profile →
My Tokens**. Generate an authentication token and record it in GitHub repository
Settings → Secrets and variables → Actions → Secrets → New repository secret,
under exactly this name:

- `PUBLISH_TOKEN`: JetBrains Marketplace personal access token.

For plugin signing, follow the official instructions at
https://plugins.jetbrains.com/docs/intellij/plugin-signing.html to generate a
private key and X.509 certificate chain **locally**. Back up the private key
securely, never commit it, and store these three more repository secrets:

- `CERTIFICATE_CHAIN`: certificate chain PEM, or its Base64 representation.
- `PRIVATE_KEY`: private key PEM, or its Base64 representation.
- `PRIVATE_KEY_PASSWORD`: password chosen for the private key.

The IntelliJ Platform Gradle Plugin supports PEM or Base64-encoded certificate
and private key content. All four secrets must be configured before enabling
publication. **Do not paste any secret into issues, commits, logs, or chats.**

### Verify the signing secrets before enabling publication

Run **GitHub Actions → Verify Plugin Signing → Run workflow**, select `main`,
and inspect the **Sign and verify ZIP without publishing** job. The workflow
reads the three signing secrets, builds the plugin, runs `signPlugin` and
`verifyPluginSignature`, and does **not** run `publishPlugin`. It also runs
automatically the first time the verification workflow is added to `main`.

The build explicitly declares that `verifyPluginSignature` depends on
`signPlugin`; this is required by Gradle 9 when the verifier reads the signed
ZIP produced by the signing task.

The certificate secret is stored Base64-encoded for GitHub Actions.
The Gradle verifier decodes it to PEM before handing it to the Marketplace ZIP
Signer CLI, while `signPlugin` accepts the original Base64 value.

A successful run confirms the key and certificate can sign this plugin ZIP. It
does **not** verify the Marketplace token or upload any version.

Only after signing succeeds and the manual IDE checklist is complete, enable
Marketplace publication:

Next go to GitHub Settings → Secrets and variables → Actions → **Variables**
and create a repository variable:

- `MARKETPLACE_AUTO_PUBLISH` = `true`

Until this variable is enabled, GitHub Releases still work but Marketplace
uploads are skipped. When enabled, the workflow calls `signPlugin`,
`verifyPluginSignature`, and `publishPlugin` for each **newly tagged** version
only. The Marketplace may still review an upload before it is publicly visible.

### Publishing v0.4.1 after enabling secrets

The `v0.4.0` tag was created before signing CI fixes and must remain immutable.
Release `v0.4.1` from a new commit including those fixes. When `MARKETPLACE_AUTO_PUBLISH`
is `true` at merge time, the `main` push can publish that new version automatically
following the GitHub Release. Otherwise, after the GitHub Release is created,
in GitHub Actions → **Release Plugin** → Run workflow, choose branch `main` and set
**Publish this version to JetBrains Marketplace** to `true`.
Avoid retrying after a successful Marketplace upload, because JetBrains rejects
duplicate version numbers.

For the next update, increment `gradle.properties`, update the changelog and
`plugin.xml` release notes, test it, and merge into `main`. The automation
will create `v<version>` once and publish to Marketplace if enabled.

Official references:
- https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html
- https://plugins.jetbrains.com/docs/intellij/plugin-signing.html
- https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets
