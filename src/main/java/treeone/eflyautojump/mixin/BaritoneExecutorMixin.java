package treeone.eflyautojump.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import treeone.eflyautojump.EFlyAutoJump;
import xaeroplus.util.BaritoneExecutor;

@Mixin(value = BaritoneExecutor.class, remap = false)
public class BaritoneExecutorMixin {

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "elytra", at = @At(value = "HEAD"))
    private static void injectElytra(final int x, final int z, final CallbackInfo ci) {
        EFlyAutoJump.startSpaceSpam();
    }
}