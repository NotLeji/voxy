package me.cortex.voxy.client.mixin.sodium;

import me.cortex.voxy.client.core.IVoxyRenderSystemHolder;
import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.client.core.util.IrisUtil;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionFlags;
import me.jellysquid.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1.20.1 backport: Rewritten for Sodium 0.5.x (was 0.9.x).
// - FallbackVisibleChunkCollector doesn't exist in 0.5.x (deleted).
// - Old target RenderRegionManager.getForChunk doesn't exist.
// - New hook: VisibleChunkCollector.visit(RenderSection, boolean) called by OcclusionCuller.
@Mixin(value = VisibleChunkCollector.class, remap = false)
public class MixinVisibleChunkCollector {

    // Called by OcclusionCuller for each section that passes culling.
    // Replaces MixinFallbackVisibleChunkCollector (class doesn't exist in 0.5.x).
    @Inject(method = "visit", at = @At("TAIL"))
    private void voxy$gatherVisibleSection(RenderSection section, boolean visible,
                                            CallbackInfo ci) {
        VoxyRenderSystem vrs;
        if (visible
                && !IrisUtil.irisShadowActive()
                && (vrs = IVoxyRenderSystemHolder.getNullable()) != null
                && vrs.visbleSectionStream != null
                && voxy$isBuiltWithGeometry(section)) {
            vrs.visbleSectionStream.put(SectionPos.asLong(
                section.getChunkX(), section.getChunkY(), section.getChunkZ()));
        }
    }

    // Replacement for (flags & MASK_IS_BUILT) != 0.
    // MASK_IS_BUILT doesn't exist in 0.5.x; use isBuilt() + HAS_BLOCK_GEOMETRY.
    private static boolean voxy$isBuiltWithGeometry(RenderSection section) {
        if (section == null || !section.isBuilt()) {
            return false;
        }
        return (section.getFlags() & RenderSectionFlags.HAS_BLOCK_GEOMETRY) != 0;
    }
}
