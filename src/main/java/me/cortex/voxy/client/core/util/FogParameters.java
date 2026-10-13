package me.cortex.voxy.client.core.util;

// 1.20.1 backport: Sodium 0.5.x has no client.util.FogParameters (added in 0.6+).
// Minimal replacement with the fields Voxy uses.
public record FogParameters(
        float environmentalStart,
        float environmentalEnd,
        float red,
        float green,
        float blue,
        float alpha
) {
    public static FogParameters DEFAULT = new FogParameters(0, Float.MAX_VALUE, 0, 0, 0, 1);
}
