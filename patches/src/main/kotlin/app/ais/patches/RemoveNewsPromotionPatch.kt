package app.ais.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * The startup news page (a WebView) shows a third-party "Porn Site
 * Promotion" banner (accordion item with an ad iframe). The page is loaded
 * with the shared WebViewClient `ce0`; its default `onPageFinished` branch
 * (used by the news WebView) simply calls super.
 *
 * A small JS snippet is injected after every page load: it removes any
 * accordion item whose header mentions the promotion, and hides any leftover
 * `.paysiteAd` blocks as a fallback. Harmless on other WebViews sharing this
 * client, since they have no matching elements.
 */
private const val STRIP_PROMOTION_JS =
    "(function(){var b=document.querySelectorAll('.accordion-item');" +
        "for(var i=0;i<b.length;i++){var h=b[i].querySelector('.accordion-header');" +
        "if(h&&/Porn Site Promotion/i.test(h.textContent)){b[i].parentNode.removeChild(b[i]);}}" +
        "var s=document.createElement('style');s.textContent='.paysiteAd{display:none!important}';" +
        "document.head.appendChild(s);})();"

object NewsWebClientFingerprint : Fingerprint(
    definingClass = "Lce0;",
    name = "onPageFinished",
    returnType = "V"
)

@Suppress("unused")
val removeNewsPromotionPatch = bytecodePatch(
    name = "Remove news promotions",
    description = "Removes the third-party paysite promotion banner from the startup news page.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_APP)

    execute {
        val method = NewsWebClientFingerprint.method
        // The first invoke-super is the default (non-switch) branch, immediately
        // followed by return-void. Inject the JS call between them.
        val superIndex = method.implementation.instructions.indexOfFirst {
            (it.reference as? MethodReference)?.definingClass == "Landroid/webkit/WebViewClient;"
        }
        method.addInstruction(superIndex + 1, "const-string v0, \"$STRIP_PROMOTION_JS\"")
        method.addInstruction(superIndex + 2, "const/4 v1, 0x0")
        method.addInstruction(
            superIndex + 3,
            "invoke-virtual {p1, v0, v1}, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V"
        )
    }
}
