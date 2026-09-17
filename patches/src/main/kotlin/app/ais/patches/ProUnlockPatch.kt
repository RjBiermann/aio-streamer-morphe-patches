package app.ais.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/**
 * The server sends `pro` (unixtime seconds, PRO expiry) and `unixtime` (server time)
 * in the login response. Everywhere in the app the PRO check is `getPro() > getUnixtime()`.
 *
 * Hooking the single getter `LoginStatus.getPro()J` to return Long.MAX_VALUE
 * makes every PRO check succeed.
 */
object ProStatusFingerprint : Fingerprint(
    definingClass = "Lcom/streamdev/aiostreamer/datatypes/login/LoginStatus;",
    name = "getPro",
    returnType = "J"
)

@Suppress("unused")
val proUnlockPatch = bytecodePatch(
    name = "Unlock PRO",
    description = "Unlocks all PRO features permanently.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_APP)

    execute {
        ProStatusFingerprint.method.addInstructions(
            0,
            """
                const-wide v0, 0x7fffffffffffffffL
                return-wide v0
            """
        )
    }
}
