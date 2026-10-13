package me.cortex.voxy.client.mixin.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import me.cortex.voxy.client.VoxyClient;
import me.cortex.voxy.client.core.IVoxyRenderSystemHolder;
import me.cortex.voxy.client.core.rendering.Viewport;
import me.cortex.voxy.client.core.util.FogParameters;
import me.cortex.voxy.client.core.util.IrisUtil;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1.20.1 backport: Rewritten for Sodium 0.5.x (was 0.9.x).
// 0.5.x render() has 5 params (no FogParameters/GpuSampler/GpuBuffer); no renderPass.getTarget().
@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class MixinDefaultChunkRenderer extends ShaderChunkRenderer {

    public MixinDefaultChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void voxy$cancelSodiumRender(ChunkRenderMatrices matrices,
                                         CommandList commandList,
                                         ChunkRenderListIterable renderLists,
                                         TerrainRenderPass renderPass,
                                         CameraTransform camera,
                                         CallbackInfo ci) {
        if (VoxyClient.disableSodiumChunkRender()) {
            super.begin(renderPass);
            this.voxy$doRender(matrices, renderPass, camera);
            super.end(renderPass);
            ci.cancel();
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                     target = "Lme/jellysquid/mods/sodium/client/render/chunk/ShaderChunkRenderer;end(Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)V",
                     shift = At.Shift.BEFORE))
    private void voxy$injectVoxyRender(ChunkRenderMatrices matrices,
                                       CommandList commandList,
                                       ChunkRenderListIterable renderLists,
                                       TerrainRenderPass renderPass,
                                       CameraTransform camera,
                                       CallbackInfo ci) {
        this.voxy$doRender(matrices, renderPass, camera);
    }

    @Unique
    private void voxy$doRender(ChunkRenderMatrices matrices,
                               TerrainRenderPass renderPass,
                               CameraTransform camera) {
        if (renderPass == DefaultTerrainRenderPasses.CUTOUT) {
            var renderer = IVoxyRenderSystemHolder.getNullable();
            if (renderer != null) {
                // 0.5.x: no renderPass.getTarget(); Sodium renders to vanilla main framebuffer.
                RenderTarget target = Minecraft.getInstance().getMainRenderTarget();

                // Fog from vanilla RenderSystem (1.20.1), not from Sodium.
                float[] fogColor = RenderSystem.getShaderFogColor();
                FogParameters fog = new FogParameters(
                    RenderSystem.getShaderFogStart(),
                    RenderSystem.getShaderFogEnd(),
                    fogColor[0], fogColor[1], fogColor[2], fogColor[3]
                );

                Viewport<?> viewport;
                if (IrisUtil.USED_IRIS_VIEWPORT) {
                    viewport = renderer.getViewport();
                    IrisUtil.USED_IRIS_VIEWPORT = false;
                } else {
                    viewport = renderer.setupViewport(
                        matrices.projection(), matrices.modelView(), fog,
                        target.width, target.height,
                        camera.x, camera.y, camera.z);
                }
                // 1.20.1: texture IDs directly, no GlTextureView cast.
                renderer.renderOpaque(viewport,
                    target.getDepthTextureId(),
                    target.getColorTextureId());
            }
        }
    }
}
