package me.cortex.voxy.client;

import java.util.List;
import java.util.Map;

// STUB for 1.20.1 backport: 1.21+ DebugScreen API (DebugScreenEntryList) does not exist in 1.20.1.
// TODO: Integrate with 1.20.1 DebugScreenOverlay via mixin if debug info is needed.
public class DebugEntries {
    public static void init() {
        // No-op in 1.20.1 backport
    }

    public static void onRebuild(Map<?, ?> allStatuses, List<?> currentlyEnabled) {
        // No-op in 1.20.1 backport
    }
}
