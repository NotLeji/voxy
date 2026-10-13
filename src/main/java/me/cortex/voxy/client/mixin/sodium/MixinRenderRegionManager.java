package me.cortex.voxy.client.mixin.sodium;

import me.cortex.voxy.client.core.IVoxyRenderSystemHolder;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = RenderRegionManager.class, remap = false)
public class MixinRenderRegionManager {
    @ModifyArg(method = "uploadResults(Lme/jellysquid/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;Lme/jellysquid/mods/sodium/client/render/chunk/UniformBufferManager;)V", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/region/RenderRegionManager$PendingSectionMeshUpload;<init>(Lme/jellysquid/mods/sodium/client/render/chunk/RenderSection;ILme/jellysquid/mods/sodium/client/render/chunk/data/BuiltSectionMeshParts;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lme/jellysquid/mods/sodium/client/gpu/arena/PendingUpload;)V"), remap = false, index = 1)
    private int voxy$cancelFade(int original) {
        if (original == -1) return original;
        var vrs = IVoxyRenderSystemHolder.getNullable();
        if (vrs==null) {
            return original;
        } else {
            return -999999;
        }
    }
}