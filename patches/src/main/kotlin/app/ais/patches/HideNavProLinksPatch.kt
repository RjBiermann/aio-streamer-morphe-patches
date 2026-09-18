package app.ais.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * The mobile nav drawer has several entries that are useless without a
 * (working, PRO) server account: "Your Account" (login), "Get Free PRO",
 * "PRO Benefits", "PornDB", "Global Search" and "PornTabs". The server
 * rejects these features for non-whitelisted builds and non-PRO accounts
 * ("You are not logged in!" / redownload error), so they are hidden from
 * the drawer. "Player Playlist" is client-side and stays.
 *
 * The app already hides its debug entries (Test Suite / Errors / Get Link)
 * in `NavDrawer.onCreate` with `menu.findItem(id).setVisible(false)`.
 * The same pattern is appended right after that block. The existing code
 * leaves the Menu reference clobbered (`move-result-object v0`), so the
 * menu is re-fetched from the NavigationView (`p1`) first; `v1` (scratch)
 * and `v8` (false) are reused.
 */
private val HIDDEN_NAV_IDS = intArrayOf(
    0x7f0b0324, // nav_premium    "Your Account"
    0x7f0b032b, // nav_tokens     "Get Free PRO"
    0x7f0b0316, // nav_buypro     "PRO Benefits"
    0x7f0b0323, // nav_porndb     "PornDB"
    0x7f0b0326, // nav_search_pro "Global Search"
    0x7f0b0329, // nav_tabs       "PornTabs"
)

object MobileNavFingerprint : Fingerprint(
    definingClass = "Lcom/streamdev/aiostreamer/mobile/ui/NavDrawer;",
    name = "onCreate",
    returnType = "V"
)

@Suppress("unused")
val hideNavProLinksPatch = bytecodePatch(
    name = "Hide account/PRO nav links",
    description = "Hides the account and PRO-only entries (Your Account, Get Free PRO, PRO Benefits, PornDB, Global Search, PornTabs) from the mobile navigation drawer.",
    default = false // disable on request; re-enable with --enable patch flag
    ) {
    compatibleWith(Constants.COMPATIBILITY_APP)

    execute {
        val method = MobileNavFingerprint.method
        // The app hides its debug entries with three MenuItem->setVisible(Z)
        // invoke-interface calls; the third (last) one is where our block
        // gets appended. invoke-interface refs are plain MethodReferences here.
        val lastIndex = method.implementation!!.instructions.indexOfLast {
            val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
            ref != null && ref.definingClass == "Landroid/view/MenuItem;" && ref.name == "setVisible"
        }
        var index = lastIndex
        method.addInstruction(
            ++index,
            "invoke-virtual {p1}, Lcom/google/android/material/navigation/NavigationView;->getMenu()Landroid/view/Menu;"
        )
        method.addInstruction(++index, "move-result-object v0")
        for (id in HIDDEN_NAV_IDS) {
            method.addInstruction(++index, "const v1, $id")
            method.addInstruction(++index, "invoke-interface {v0, v1}, Landroid/view/Menu;->findItem(I)Landroid/view/MenuItem;")
            method.addInstruction(++index, "move-result-object v1")
            method.addInstruction(++index, "invoke-interface {v1, v8}, Landroid/view/MenuItem;->setVisible(Z)Landroid/view/MenuItem;")
        }
    }
}
