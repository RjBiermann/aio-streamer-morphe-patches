# AGENTS.md — morphe-patches (patch repo)

GitHub: `RjBiermann/brave-waffle`. Kotlin Morphe patches for AIO Streamer
(`com.streamdev.aiostreamer` v6.7.1, versionCode 6719, phone+TV in one universal APK —
TV UI is runtime-detected via `hasSystemFeature("android.software.leanback")` in
`smali/dp0.smali`). Workspace-level notes (layout, stock APK, emulator recipes) are in
`../AGENTS.md`; builder specifics in `../builder/AGENTS.md`.

## Patch knowledge (verified against v6.7.1, versionCode 6719)

- **API surface**: `https://porn-app.com/api/` (Retrofit, `dj.smali`), endpoints
  `v9/device`, `v9/sites`, `v9/unixTime`, `v9/login`, `v9/videoheaders`… Requests carry
  `Authorization: Bearer<accessToken>` and `hash` = RSA(4096)-PKCS1-encrypted
  `HashInformation` JSON (`time`, `version`=6719, `id`=androidId, `packageName`,
  `signatures`=[cert SHA-256 b64], `loginStatus`). Embedded RSA public key in
  `x93.a()`. GSON serializes HashInformation fields directly (no @SerializedName →
  raw field names).

- **PRO check**: `LoginStatus.getPro()J` (NOT obfuscated) returns PRO-expiry unixtime seconds;
  every check in the app is `getPro() > getUnixtime()`. Patch = hook the getter to return
  `Long.MAX_VALUE`. GSON deserializes into the field directly, so server responses still parse;
  all reads go through the getter.
- **Risk**: the API validates the RSA-encrypted device hash (`x93.b()` → `HashInformation`)
  that every request carries: the signing-certificate hash must be in the server's
  whitelist, and `loginStatus.pro` in the hash must be 0 or in the past (a future
  value → HTTP 500 "Application Error - please redownload"). Hence two patches:
  spoof the signature (`SpoofSignaturePatch`) and no-op `setPro` so the hash
  reports `pro=0` while local checks go through the getter. Verified server-side:
  `sig=orig, pro=0` → 200; `pro=MAX` or wrong sig → 500 redownload error.
  If the server ever tightens hash validation further, patch the `cmp-long` sites
  instead (classes eb0, g75, jp9, p81, v5a in the decompile).
- **Spoof fingerprint gotcha (cost 3 CI iterations)**: the matched instruction for
  `methodCall(Base64, encodeToString)` in `x93.b` is the `invoke-static` (35c), and the
  result register lives in the `move-result-object` at `index + 1` — read
  `getInstruction<OneRegisterInstruction>(index + 1).registerA`, then
  `removeInstructions(index, 2)` + `addInstruction(index, const-string ...)`.
  Verified end-to-end in emulator 2026-09-17 (patches v1.0.4, builder tag `4`):
  startup OK, PRO features unlock with a real account.
- **API etiquette**: probing their API is risky (ban/block). Emulate the real app
  exactly (okhttp UA, `hash` + `Authorization` headers), send single spaced requests,
  and stop once the hypothesis is confirmed.
- **Ads (RemoveAdsPatch.kt)**: IMA (Google Interactive Media Ads) sources wrap players
  with VMAP ad tags. Three variants, all at the app's own domain:
  `vmap.xml` (StandardVideoPlayer `T()V` + PopupVideoPlayer), `vmapc.xml` (`ql0.smali`,
  the Cast `VastAdsRequest$a;->a` field), `vmap_swipe.xml` (`os7.smali`).
  Patch = replace each URL const-string with a **non-empty dummy URL
  `http://127.0.0.1/x`** (base64 `DUMMY_AD_URL` constant). An EMPTY string ""
  makes the IMA SDK throw `IllegalArgumentException: Either ad tag url or ads response
  must non-null and non empty` at video start (`je.k`/`tl9.V`/`p9.<init>`) —
  found 2026-09-17 in the emulator: playback crashed on every ad-enabled site.
  Dummy URL → ad error event → playback continues. Fixed in v1.2.0; verified
  end-to-end on emulator (v1.3.0 build: playback, 0 crashes, favorites persist).
- **Nav drawer (v1.3.0, `HideNavProLinksPatch.kt`)**: hides 6 login/PRO-gated mobile nav
  items (Your Account, Get Free PRO, PRO Benefits, PornDB, Global Search, PornTabs) in
  `NavDrawer.onCreate`, anchored after the app's own debug-item `MenuItem.setVisible`
  block; re-fetch the Menu (the app clobbers the register) and `findItem(...).setVisible(false)`.
  **Disabled by default** (`default = false`) per user request — enable with `--enable`.
  Keep Player Playlist visible (still needs login/PRO server-side, but user wants it kept).
- **RemoveNewsPromotionPatch (v1.1.0)**: news page promo stripped via JS injected in
  shared WebViewClient `Lce0;->onPageFinished` default branch (after first
  invoke-super): removes the `.accordion-item` matching /Porn Site Promotion/ and
  injects CSS `.paysiteAd{display:none!important}`. Verified on emulator.
- **URL obfuscation convention**: the app's domain never appears in plain text in any
  repo — base64-encoded (raw base64 literals in RemoveAdsPatch.kt; `b64:` prefix in
  builder config.toml, decoded in build.sh). Keep it that way in new code.
- **No billing/ad SDKs** (no AdMob, no billingclient) — "ads" are only the in-player
  VMAP breaks.

## Login state model (three states, mapped in smali 2026-09-17)

State lives ONLY in prefs (`ka1.j`): `username`, `password`, `accessToken`. No persisted
LoginStatus object — after any login the state is re-established by POST `v9/login`
(`dj.c(UserData, hash, Authorization)`); `UserData()`'s ctor auto-loads stored
username/password + android_id each time it is constructed. The response
`LoginStatus{status:int, pro:J expiry, token, unixtime}` is fanned out in memory via
LiveData to consumers (`p81.B`, `g13.V2`, `os7.n`, players' `V2`).

- **Not logged in**: prefs empty. Startup `v9/device` is ALWAYS sent with a null
  LoginStatus → `x93.b(null)` → hash reports `{pro:0, token:"", status:0, unixtime:now}`
  even on PRO devices — identity travels in the `Authorization` header, not the hash.
  Login handler (`eb0.c`): `status < 200` → error dialog (Try again / Cancel /
  Reset Password), nothing saved.
- **Logged in, free account**: `status >= 200 && pro < unixtime` → "You are logged in!"
  toast; saves accessToken/username/password; fires `sa4.e(true)`.
- **Logged in, PRO**: `status >= 200 && pro >= unixtime` → "PRO until <date> - N days
  remaining" PRO Check dialog; same save + `sa4.e(true)`. With the getPro→MAX hook
  EVERY successful login shows the PRO dialog — client-side free vs PRO is gone.

Mobile per-state behavior:

| | not logged in | free | PRO |
|---|---|---|---|
| Browse sites / video info / playback | ✅ | ✅ | ✅ |
| Client-side PRO UI (`p81` sheet items, `g13.V2` proEnabled, player playlist `sk8`) | unlocked by hook | unlocked | unlocked |
| Swipe interstitial ads (`os7.n`, every 10th swipe, `h75`) | **ACTIVE** (flag only set from a successful login response) | gone (hook) | gone |
| PornDB / favorites / history / playlists (`v9/*`) | ❌ server: "You are not logged in!" | depends on account's server-side state | ✅ |
| Global Search (`fr5` → `porn-app.com/login/inapp` WebView) | login page | ✅ | ✅ |
| TV site browsing (`wk2` → VideoActivityTV) | — | ❌ server: "not a PRO User!" | ✅ |

Details:
- All local PRO checks are `getPro() > now` (via `getUnixtime()` — or
  `System.currentTimeMillis()/1000` in `g75`, still works with MAX) → all neutralized
  by the getter hook.
- `os7.n` is the ONE client-side remnant the getter hook misses for anonymous use: it
  defaults false and is only set true from a login response (`g75`). If ever needed:
  force `n=true` in `os7.<init>` (one-line smali/patch).
- `i75` (NSFWSwipe fragment) silently re-logins at startup with stored creds; the
  swipe login path (`es7`) persists accessToken only when `status == 200`.

**TV difference**: "everything is PRO" — even browsing is a server-side PRO gate (it
checks the account's server state, not the hash's pro; past-pro hash tested → rejected).
The TV tile/login gates are client-side and already patched (see below).

**Goal**: make not-logged-in and logged-in flows seamless, bypass every client-side
PRO check (done — getter hook; only leftover above). Server-side walls (PornDB for
anonymous, TV browse for non-PRO accounts) are NOT client-patchable — they need a real
account the server blesses with PRO.

## TV notes (Television_1080p AVD, emulator-5556, leanback UI)

- **The real TV sites fragment is `wk2`** (leanback browse fragment, confirmed via
  `dumpsys activity top` Added Fragments). Patching `v48.q0` (earlier attempt) does
  NOT fix this path — v48 is a different (non-active) sites fragment.
- **TV gate chain, fully mapped and PATCHED (patches v1.2.1, verified 2026-09-17
  on emulator)**:
  1. Site tiles are only built when `SiteInfo.packagename == "standardsite"`
     (`uk2` list build — other groups become empty and are dropped).
  2. Tile clicks (`k23.h`) are gated on `wk2.u4` (logged-in flag, only set by the
     login success callback `eb0.c` → `sa4.e(true)`), and route by pkg switch:
     paid pkgs → PaysiteLoginActivity, unknown pkg → silent no-op.
  3. `wk2.b1(ZZ)V` shows the "User Account needed" dialog (`fk.D`) when pref
     `username` is empty; non-empty → `fk.q` → `fk.s` anonymous login (safe:
     fk ctor inits its username/password fields to "", no NPE).
  Fix (BypassAccountNeededPatch): `getPackagename()` → "standardsite" (tiles for
  all sites + click path) and `wk2.b1`'s `String.isEmpty` result forced to false
  (always the `fk.q` login route → callback sets `u4=true` → clicks work).
  fk.s's empty-creds pref clearing is a no-op on an anonymous device.
- **Server wall (NOT patchable client-side)**: TV site browsing (video-list fetch
  inside VideoActivityTV) is PRO-gated server-side — API returns app-level error
  "It seems you are not a PRO User!..." (string not in the app; server-sent) for
  accounts without PRO. Sending a PAST non-zero pro in the request hash does NOT
  help (tested 2026-09-17 — identical error; the hash validation accepts past pro,
  but the browse gate checks the account's server-side state). TV browsing needs a
  real account with PRO (user's old account was deleted server-side; needs a fresh
  signup + free PRO earned in the mobile app).
- Builder release APKs can't install on x86 TV emulator (`INSTALL_FAILED_NO_MATCHING_ABIS`
  — builder's utils.sh strips lib/x86* even for arch=all; fine for real arm TVs).
  For TV emulator testing, patch locally with the morphe CLI (keeps all ABIs) —
  see `../AGENTS.md` "How to build".
- TV D-pad: keyevent 19/20/66; AVD is 1920x1080 direct coords for taps.

## Build & release

```bash
# local build (gh token needs read:packages for the morphe registry)
export GITHUB_ACTOR=RjBiermann GITHUB_TOKEN=$(gh auth token)
./gradlew :patches:build -q    # → patches/build/libs/patches-*.mpp
```

- CI releases `patches-*.mpp` on every push (semantic-release, conventional commits).
- Release rules: `feat:` → minor, `fix:` → patch, `chore:` → no release.
- Generated files (`README.md` patches list, `patches-bundle.json`, `CHANGELOG.md`)
  update only on releases.
- `git pull --rebase` before push (semantic-release commits tags/files on remote).
- CI release-run FAIL on non-main branches is only the semantic-release dev-branch
  backmerge (no dev branch exists) — the .mpp asset is published regardless.

## Test

Patch a local APK and verify (recipe in workspace root AGENTS.md). Morphe Desktop CLI
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
  — works in Actions with GITHUB_TOKEN; locally needs a PAT with `read:packages`.
- Patch API: `bytecodePatch(name, description, default=true)`,
  `Fingerprint(definingClass, name, returnType)`, `method.addInstruction(index, "<smali>")`;
  no `InterfaceReference` class in this dexlib2 fork — use `MethodReference` +
  check `definingClass`/`name`.
