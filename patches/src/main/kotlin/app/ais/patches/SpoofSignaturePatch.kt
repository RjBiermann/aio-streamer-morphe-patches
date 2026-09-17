package app.ais.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/**
 * Every API request authenticates with a `Bearer` hash built from a JSON blob
 * (via GSON, which serializes FIELDS, not getters) that includes the SHA-256
 * digest of the app's signing certificate. The server rejects unknown signing
 * certificates with an "application error — please redownload" dialog.
 *
 * Hook `HashInformation.setSignatures()V` (the choke point that stores the
 * certificate list before serialization) so the stored list always contains
 * the original app signature, regardless of what key patched builds are
 * signed with.
 */
// base64(SHA-256 of the stock APK's signing certificate), NO_WRAP — the value
// the app would report unmodified.
private const val ORIGINAL_SIGNATURE = "VQMyUhZdmnnwK5RVCbeGqu0HN020MEDUM44crQyL1zw="
object SignatureFingerprint : Fingerprint(
    definingClass = "Lcom/streamdev/aiostreamer/datatypes/login/HashInformation;",
    name = "setSignatures",
    parameters = listOf("Ljava/util/List;")
)

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof app signature",
    description = "Reports the original app signature to the API so patched builds are not rejected.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_APP)

    execute {
        SignatureFingerprint.method.addInstructions(
            0,
            """
                const-string v1, "$ORIGINAL_SIGNATURE"
                filled-new-array {v1}, [Ljava/lang/String;
                move-result-object v1
                invoke-static {v1}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;
                move-result-object v1
            """
        )
    }
}
