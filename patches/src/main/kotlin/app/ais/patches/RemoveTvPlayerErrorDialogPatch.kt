package app.ais.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * m72.a(Context, Throwable) is the generic "Error occured" popup shown on
 * every caught APIException/CloudflareException (TV player fragment data
 * callbacks, its refetch runnable zg2, etc.). Playback proceeds fine — the
 * failing calls are gated for anonymous accounts and non-fatal — so the
 * popup is pure noise. Keep the xj2.b error log, then return before any
 * dialog is built.
 */
@Suppress("unused")
val removeTvPlayerErrorDialogPatch = bytecodePatch(
    name = "Remove TV player error dialog",
    description = "Suppress the 'Error occured' popup on playback failures",
    default = true,
) {
    compatibleWith("com.streamdev.aiostreamer")

    execute {
        val a = object : Fingerprint(
            definingClass = "Lm72;",
            name = "a",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/Throwable;")
        ) {}
        val impl = a.method.implementation!!
        val instrs = impl.instructions
        val idx = instrs.indexOfFirst { ins ->
            ins.opcode == Opcode.INVOKE_VIRTUAL &&
                (ins as? ReferenceInstruction)?.reference.let { r ->
                    r is MethodReference && r.definingClass == "Lxj2;" && r.name == "b"
                }
        }
        check(idx > 0) { "m72.a: xj2.b log call not found" }
        a.method.addInstructions(idx + 1, "return-void")
    }
}
