package me.cortex.voxy.client.core;

import me.cortex.voxy.client.core.gl.shader.Shader;

import static org.lwjgl.opengl.GL11C.*;

// 1.20.1 backport: Simplified. 1.20.1 uses OpenGL -1..1 depth (not 0..1), standard GL_LESS depth.
// Iris integration stubbed.
public record RenderProperties(boolean isZero2One, boolean isReverseZ, boolean useBlockAtlasUVs) {

    public <T extends Shader.Builder<J>, J extends Shader> T apply(T builder) {
        return (T) builder.defineIf("USE_ZERO_ONE_DEPTH", this.isZero2One)
                .defineIf("USE_REVERSE_Z", this.isReverseZ);
    }

    public int closerEqualDepthCompare() {
        return this.isReverseZ?GL_GEQUAL:GL_LEQUAL;
    }

    public int closerDepthCompare() {
        return this.isReverseZ?GL_GREATER:GL_LESS;
    }

    public int furtherDepthCompare() {
        return this.isReverseZ?GL_LESS:GL_GREATER;
    }

    public float clearDepth() {
        return this.isReverseZ?0.0f:1.0f;
    }

    public float inverseClearDepth() {
        return this.isReverseZ?1.0f:0.0f;
    }

    public static RenderProperties getRenderProperties() {
        // 1.20.1: OpenGL depth range is -1..1 (isZero2One=false), standard depth func (isReverseZ=false)
        return new RenderProperties(false, false, false);
    }
}
