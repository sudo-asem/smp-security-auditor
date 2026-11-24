package dev.smpaudit.mixin;

import dev.smpaudit.AuditExporter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleCommands", at = @At("TAIL"))
    private void smpaudit$afterCommandTreeSync(ClientboundCommandsPacket packet, CallbackInfo ci) {
        AuditExporter.capture(((ClientPacketListenerAccessor) (Object) this).smpaudit$getCommands());
    }
}
