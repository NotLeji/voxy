package me.cortex.voxy.client.core.model;

// 1.20.1 backport: Replacement for 1.21.2+ ChunkSectionLayer.
// Used as a simple 3-value enum for model baking logic.
public enum BakeLayer {
    SOLID,
    CUTOUT,
    TRANSLUCENT
}
