# AGENTS.md — Morphe patches for AIS

GitHub repo: `RjBiermann/brave-waffle`. Releases `patches-*.mpp` via semantic-release on every push.

Target: `com.streamdev.aiostreamer` (AIS v6.7.1, versionCode 6719).
One universal APK serves phone + Android TV (TV UI is runtime-detected via
`android.software.leanback` — see workspace root AGENTS.md for decompile details).

## Patches

- **Unlock PRO** (`ProUnlockPatch.kt`): hooks `Lcom/streamdev/aiostreamer/datatypes/login/LoginStatus;->getPro()J`
  to `return-wide` `Long.MAX_VALUE`, and no-ops `setPro(J)` (return-void). Every local
  PRO check is `getPro() > now` (mostly `getUnixtime()`, one site compares
  `System.currentTimeMillis()/1000` — `g75`) and all reads go through the getter, so
  both client states collapse: not-logged-in ≈ logged-in-free ≈ PRO for all client-side
  gates. Fingerprint targets an unobfuscated class/method — expected to survive updates.
  Hash safety: `x93.b()` (request-hash builder) reads `getPro()` then stores it via
  `setPro()` — the no-op keeps `loginStatus.pro` at 0 in the RSA-encrypted hash, which
  the server accepts (verified: `sig=orig, pro=0` → 200; `pro=MAX` in the hash → 500
  redownload error). If the server ever tightens hash validation further, patch the
  `cmp-long` sites instead (classes eb0, g75, jp9, p81, v5a in the decompile).
  Server-side walls (PornDB for anonymous, TV browse for non-PRO accounts) are NOT
  covered by this patch — see workspace root AGENTS.md "Login state model".
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
