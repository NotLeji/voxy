package me.cortex.voxy.client.core.util;

import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.client.core.rendering.Viewport;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.fabricmc.loader.api.FabricLoader;

// 1.20.1 backport: Iris integration stubbed. Full Iris 1.7.x support TBD.
// Returns safe defaults (no shader active) so core Voxy works without Iris.
public class IrisUtil {

    public record CapturedViewportParameters(ChunkRenderMatrices matrices, FogParameters parameters, int width, int height, double x, double y, double z) {
        public Viewport<?> apply(VoxyRenderSystem vrs) {
            return vrs.setupViewport(this.matrices.projection(), this.matrices.modelView(), this.parameters, this.width, this.height, this.x, this.y, this.z);
        }
    }

    public static boolean USED_IRIS_VIEWPORT;
    public static CapturedViewportParameters CAPTURED_VIEWPORT_PARAMETERS;

    public static final boolean IRIS_INSTALLED = FabricLoader.getInstance().isModLoaded("iris");
    public static final boolean SHADER_SUPPORT = false; // Disabled for 1.20.1 backport

    public static boolean irisShadowActive() {
        return false; // Stubbed
    }

    public static void clearIrisSamplers() {
        // Stubbed
    }

    public static void reload() {
        // Stubbed
    }

    public static boolean irisShaderPackEnabled() {
        return false; // Stubbed
    }

    public static boolean irisShadersEnabledInConfig() {
        return false; // Stubbed
    }

    public static void disableIrisShaders() {
        // Stubbed
    }
}
