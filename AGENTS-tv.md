# AGENTS-tv.md — TV notes (Television_1080p AVD, emulator-5556, leanback UI)

Entry: `AGENTS.md`. Login/server walls: `AGENTS-login.md`.

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
- **Server wall — SOLVED (patches v1.3.0, TvDataIsTvPatch, verified 2026-09-18 on
  emulator: browse + video playback work anonymously, TV and phone)**: the
  "It seems you are not a PRO User!..." HTTP 403 is NOT account state — it is
  keyed on request CONTENT. The server PRO-gates:
  1. `isTV=true` query param on `v9/sites/{tag}/data|related|tags|stream|extra`
     → force `isTV=false` in the 3 request lambdas (fp4/r31/wj7, hook the
     `move-result` after every `dp0.a` call: fp4 2 sites, r31 2, wj7 1).
  2. `pornTabs=true` in the `SiteInfoRequest` body (only the TV site screen
     `ze4.f` sends it) on `v9/sites/{tag}/info|link|categories` → force
     `const/4 p3, 0x0` at ctor start (mobile already sends false).
  The TV also passes `filter=null` (phone always passes one) — a filter-less
  body gets "You are not logged in!" on /link even with pornTabs fixed →
  default a `StandardFilter()` at ctor start when p2 is null. With pornTabs
  false the responses omit some VideoInformation lists (e.g. pornstars) →
  null-safe `getPornstars()` (returns EMPTY_LIST) and null-safe
  `SiteInfoRequest.toString()` (append(Object) instead of
  StandardFilter.toString()) — both NPE'd/killed threads on TV.
  Verified phone regression: pornTabs=false forced globally is harmless on
  mobile (browse + playback pass). Paysite PROMO tiles (BRAZZERS/EVIL EROTIC/
  MAMACITAZ etc. in every listing) still 403 "not logged in" on the player's
  `POST v9/sites/{tag}/link` + `POST v9/video/{id}/info` (dj.I) — real Pornhub
  videos play; promo tiles are studio ads, not a patch bug.
  Instrumentation technique (SIR ctor): ctor has `.locals 0`; insert logging
  just before the final `return-void` where all param registers are dead.
- Builder release APKs can't install on x86 TV emulator (`INSTALL_FAILED_NO_MATCHING_ABIS`
  — builder's utils.sh strips lib/x86* even for arch=all; fine for real arm TVs).
  For TV emulator testing, patch locally with the morphe CLI (keeps all ABIs) —
  see `../AGENTS.md` "How to build".
- TV D-pad: keyevent 19/20/66; AVD is 1920x1080 direct coords for taps.
