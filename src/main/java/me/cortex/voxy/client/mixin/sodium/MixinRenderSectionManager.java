package me.cortex.voxy.client.mixin.sodium;

import me.cortex.voxy.client.ICheekyClientChunkCache;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IVoxyRenderSystemHolder;
import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.client.core.util.IrisUtil;
import me.cortex.voxy.common.world.service.VoxelIngestService;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionFlags;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1.20.1 backport: Rewritten for Sodium 0.5.x (was 0.9.x).
// - Constructor: (ClientLevel, int, CommandList) instead of (ClientLevel, int, SortBehavior)
// - renderOutOfGraph/readRenderListFromTree don't exist -> hook update() instead
// - updateSectionInfo returns void; isInvisible() gone -> use HAS_BLOCK_GEOMETRY
@Mixin(value = RenderSectionManager.class, remap = false)
public class MixinRenderSectionManager {
    @Unique
    private static final boolean BOBBY_INSTALLED = FabricLoader.getInstance().isModLoaded("bobby");

    @Shadow @Final private ClientLevel world;

    @Unique private long cachedChunkPos = -1;
    @Unique private int cachedChunkStatus;
    @Unique private int bottomSectionY;

    // 0.5.x: (ClientLevel, int, CommandList)
    @Inject(method = "<init>", at = @At("TAIL"))
    private void voxy$initBottomY(ClientLevel level, int renderDistance,
                                   CommandList commandList, CallbackInfo ci) {
        this.bottomSectionY = this.world.getMinY() >> 4;
    }

    // Replacement for renderOutOfGraph + readRenderListFromTree hooks (methods don't exist in 0.5.x):
    // reset visible-section stream once per frame before rebuild.
    @Inject(method = "update", at = @At("HEAD"))
    private void voxy$resetVisibleStream(Camera camera, Viewport viewport,
                                          int frame, boolean spectator,
                                          CallbackInfo ci) {
        var vrs = IVoxyRenderSystemHolder.getNullable();
        if (vrs != null && !IrisUtil.irisShadowActive()
                && vrs.visbleSectionStream != null) {
            vrs.visbleSectionStream.reset();
        }
    }

    @Inject(method = "onChunkRemoved", at = @At("HEAD"))
    private void voxy$injectIngest(int x, int z, CallbackInfo ci) {
        if (VoxyConfig.CONFIG.ingestEnabled && !BOBBY_INSTALLED) {
            var cccm = (ICheekyClientChunkCache) this.world.getChunkSource();
            if (cccm != null) {
                var chunk = cccm.voxy$cheekyGetChunk(x, z);
                if (chunk != null) {
                    VoxelIngestService.tryAutoIngestChunk(chunk);
                }
            }
        }
    }

    @Inject(method = "onChunkAdded", at = @At("HEAD"))
    private void voxy$ingestOnAdd(int x, int z, CallbackInfo ci) {
        if (this.world != null && VoxyConfig.CONFIG.ingestEnabled) {
            var cccm = this.world.getChunkSource();
            if (cccm != null) {
                var chunk = cccm.getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk != null) {
                    VoxelIngestService.tryAutoIngestChunk(chunk);
                }
            }
        }
    }

    // 0.5.x: updateSectionInfo(RenderSection, BuiltSectionInfo) returns void.
    // Inject at TAIL; section.setInfo() already called inside.
    @Inject(method = "updateSectionInfo", at = @At("TAIL"))
    private void voxy$updateOnUpload(RenderSection section, BuiltSectionInfo info,
                                      CallbackInfo ci) {
        // Replacement for isInvisible(): only ingest if section has block geometry.
        if ((section.getFlags() & RenderSectionFlags.HAS_BLOCK_GEOMETRY) == 0) {
            return;
        }
        VoxyRenderSystem vrs = IVoxyRenderSystemHolder.getNullable();
        if (vrs == null || !VoxyConfig.CONFIG.ingestEnabled) {
            return;
        }
        int x = section.getChunkX(), y = section.getChunkY(), z = section.getChunkZ();

        // AccessorChunkTracker still valid: chunkStatus field exists in 0.5.x.
        // 1.20.1: call interface method directly to avoid intermediary signature issue
        var tracker = ((AccessorChunkTracker) ((ChunkTrackerHolder) this.world).sodium$getTracker())
            .getChunkStatus();
        long key = ChunkPos.asLong(x, z);
        if (key != this.cachedChunkPos) {
            this.cachedChunkPos = key;
            this.cachedChunkStatus = tracker.getOrDefault(key, 0);
        }
        if (this.cachedChunkStatus == 3) { // chunk + neighbors ready
            var cccm = this.world.getChunkSource();
            var chunk = cccm.getChunk(x, z, ChunkStatus.FULL, false);
            if (chunk != null) {
                var sec = chunk.getSection(y - this.bottomSectionY);
                var lp = this.world.getLightEngine();
                var csp = SectionPos.of(x, y, z);
                var blp = lp.getLayerListener(LightLayer.BLOCK).getDataLayerData(csp);
                var slp = lp.getLayerListener(LightLayer.SKY).getDataLayerData(csp);
                VoxelIngestService.rawIngest(vrs.getEngine(), sec, x, y, z,
                    blp == null ? null : blp.copy(),
                    slp == null ? null : slp.copy());
            }
        }
    }
}
