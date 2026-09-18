# AGENTS.md — morphe-patches (patch repo)

GitHub: `RjBiermann/brave-waffle`. Kotlin Morphe patches for AIO Streamer
(`com.streamdev.aiostreamer` v6.7.1, versionCode 6719, phone+TV in one universal APK —
TV UI is runtime-detected via `hasSystemFeature("android.software.leanback")` in
`smali/dp0.smali`). Workspace-level notes (layout, stock APK, emulator recipes) are in
`../AGENTS.md`; builder specifics in `../builder/AGENTS.md`.

## Document index

| File | Contents |
|---|---|
| `AGENTS.md` (this file) | Build, release rules, patch API gotchas, test recipe |
| `AGENTS-patches.md` | Patch knowledge: API surface, PRO hook, hash validation, each patch's internals |
| `AGENTS-login.md` | Login state model (not-logged-in / free / PRO), per-state behavior table |
| `AGENTS-tv.md` | TV gate chain (patched), server walls, TV emulator notes |

## Build & release

```bash
# local build (gh token needs read:packages for the morphe registry)
export GITHUB_ACTOR=RjBiermann GITHUB_TOKEN=$(gh auth token)
./gradlew :patches:build -q    # → patches/build/libs/patches-*.mpp
```

- CI releases `patches-*.mpp` on every push (semantic-release, conventional commits).
- Release rules: `feat:` → minor, `fix:` → patch, `chore:` → no release.
- Generated files (`README.md` patches list, `patches-bundle.json`, `CHANGELOG.md`)
  are CI-generated — don't hand-edit; they update only on releases.
- `git pull --rebase` before push (semantic-release commits tags/files on remote).
- CI release-run FAIL on non-main branches is only the semantic-release dev-branch
  backmerge (no dev branch exists) — the .mpp asset is published regardless.

## Test

Patch a local APK and verify (recipe in `../AGENTS.md`). Morphe Desktop CLI
expected at `../bin/morphe-desktop.jar`.

```bash
java -jar ../bin/morphe-desktop.jar patch ../<stock apk>.apk -o out.apk -p <mpp>
```

## Gotchas

- Morphe CLI flag differences vs ReVanced: `list-patches --patches=<file>` (`-p` is
  `--with-packages`, a boolean!). j-hc's utils.sh already falls back to the right form.
- `--patches` accepts a GitHub `owner/repo` URL directly with the morphe CLI.
- `appIconColor` in Compatibility must be 0xRRGGBB (no alpha byte), Kotlin `Int`.
- Strings in patches: Morphe patcher v1.13 patch API — string replacement via
  `string(<url>).matchAllMethodIndicesForEach` from `app.morphe.util`
  (needs `app.morphe:morphe-patches-library:1.6.2` in deps; NOT part of morhe-patcher).
- Registry auth: Morphe gradle plugin resolves from `maven.pkg.github.com/MorpheApp/registry`
  — works in Actions with GITHUB_TOKEN; locally needs a PAT with `read:packages`
  (env `GITHUB_ACTOR`/`GITHUB_TOKEN` or gradle.properties `gpr.user`/`gpr.key`).
- Patch API: `bytecodePatch(name, description, default=true)`,
  `Fingerprint(definingClass, name, returnType)`, `method.addInstruction(index, "<smali>")`;
  no `InterfaceReference` class in this dexlib2 fork — use `MethodReference` +
  check `definingClass`/`name`.
