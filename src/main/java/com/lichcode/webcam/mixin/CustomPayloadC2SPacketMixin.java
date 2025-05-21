package com.lichcode.webcam.mixin;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(CustomPayloadC2SPacket.class)
public class CustomPayloadC2SPacketMixin {

    @ModifyConstant(
            method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V",
            constant = @Constant(intValue = 32767)
    )
    private int modifyMaxPayloadSize(int originalSize) {
        return 120500;
    }
}
