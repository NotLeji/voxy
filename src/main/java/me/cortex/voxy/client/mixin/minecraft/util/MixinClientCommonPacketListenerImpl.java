package me.cortex.voxy.client.mixin.minecraft.util;

import me.cortex.voxy.client.LoadException;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1.20.1 backport: ClientCommonPacketListenerImpl was added in 1.20.2+; use ClientPacketListener
@Mixin(ClientPacketListener.class)
public class MixinClientCommonPacketListenerImpl {
    @Inject(method = "onPacketError", at = @At("HEAD"), cancellable = true)
    private void handleDisconnectAsCrash(Packet<?> packet, Exception exception, CallbackInfo ci) {
        if (packet instanceof ClientboundLoginPacket) {
            ci.cancel();
            throw new LoadException("Force crashing due to exception during on game join", exception);
        }
    }
}
