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
