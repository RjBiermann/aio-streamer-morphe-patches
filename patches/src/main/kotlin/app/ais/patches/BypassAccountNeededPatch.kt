package app.ais.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/**
 * On TV, the sites screen (`wk2`) shows site tiles only for
 * `SiteInfo.packagename == "standardsite"` (all others become an empty group
 * that is dropped), and the tile click handler (`k23.h`) falls through to a
 * no-op for any packagename it doesn't know. Without a user account free
 * sites therefore show nothing, and the screen is gated behind the
 * "User Account needed" dialog (`wk2.b1` → `fk.D`).
 *
 * Redirecting `getPackagename()` to "standardsite" makes every site a
 * standard site: tiles appear for all sites and every tile click routes
 * through the free path (`VideoActivityTV` with the `SITETAG` extra). The
 * paid "paysites" row (`sitetag == "paysites"`) still opens its login
 * activity, and other screens (phone sites screen, extrasite picker) are
 * unaffected because they never branch on unknown packagenames.
 */
object SitePackagenameFingerprint : Fingerprint(
    definingClass = "Lcom/streamdev/aiostreamer/datatypes/sites/SiteInfo;",
    name = "getPackagename",
    returnType = "Ljava/lang/String;"
)

@Suppress("unused")
val bypassAccountNeededPatch = bytecodePatch(
    name = "Use free sites on TV without account",
    description = "Bypasses the 'User Account needed' gate so free sites are listed and playable on the TV UI without a user account.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_APP)

    execute {
        SitePackagenameFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "standardsite"
                return-object v0
            """
        )
    }
}
