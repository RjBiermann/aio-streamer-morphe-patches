# AGENTS.md — Morphe patches for AIS

GitHub repo: `RjBiermann/brave-waffle`. Releases `patches-*.mpp` via semantic-release on every push.

Target: `com.streamdev.aiostreamer` (AIS v6.7.1, versionCode 6719).
One universal APK serves phone + Android TV (TV UI is runtime-detected via
`android.software.leanback` — see workspace root AGENTS.md for decompile details).

## Patches

- **Unlock PRO** (`ProUnlockPatch.kt`): hooks `Lcom/streamdev/aiostreamer/datatypes/login/LoginStatus;->getPro()J`
  to `return-wide` `Long.MAX_VALUE`. Every PRO check in the app is `getPro() > getUnixtime()`.
  Fingerprint targets an unobfuscated class/method — expected to survive updates.
  Known ceiling: the getter value is also fed into the `Bearer` request hash built by
  `x93.b()`; if the server starts rejecting it, patch the `cmp-long` sites instead
  (classes eb0, jp9, v5a, p81 in the decompile).
- **Remove ads** (`RemoveAdsPatch.kt`): empties the three VMAP ad-tag URL const-strings
  (standard/popup player, "c" player `ql0`, swipe player `os7`). URLs are stored as raw
  base64 literals — the app's domain must never appear in plain text anywhere in this repo.

## Rules

- **No plain-text app domain, ever.** New strings: base64 first
  (`printf 'https://...' | base64 -w0`).
- Conventional commits: `feat:` → minor release, `fix:` → patch, `chore:` → no release.
- `README.md` patches-list section, `patches-bundle.json`, `CHANGELOG.md` are
  CI-generated — don't hand-edit.
- API for string replacement: `string(x).matchAllMethodIndicesForEach { }` comes from
  `app.morphe:morphe-patches-library:1.6.2` (`app.morphe.util`), not from morhe-patcher.
- `Compatibility.appIconColor` must be 0xRRGGBB (no alpha) Kotlin Int.
- Gradle resolves `app.morphe.patches` plugin from `maven.pkg.github.com/MorpheApp/registry`
  — works with Actions GITHUB_TOKEN; locally requires a classic PAT with
  `read:packages` (env GITHUB_ACTOR/GITHUB_TOKEN or gradle.properties gpr.user/gpr.key).

## Test

Patch a local APK and verify (recipe in workspace root AGENTS.md). Morphe Desktop CLI
expected at `../bin/morphe-desktop.jar`.

```bash
java -jar ../bin/morphe-desktop.jar patch ../<stock apk>.apk -o out.apk -p <mpp>
```
