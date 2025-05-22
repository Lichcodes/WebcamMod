package com.lichcode.webcam;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.data.DataGenerator;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.core.jmx.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebcamMod implements ModInitializer {
	public static final String MOD_ID = "webcam";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
		ServerPlayNetworking.registerGlobalReceiver(VideoFramePayload.VIDEO_FRAME_PAYLOAD_ID, (server, sender, handler, buf, responseSender) -> {
            // Only send the video to players within 100 blocks
            for (ServerPlayerEntity player : PlayerLookup.around(sender.getServerWorld(), sender.getPos(), 100)) {
                // Do not send the video to the sender, they just sent it to the server duh.
                if (player.getUuid() == sender.getUuid()) {
                    continue;
                }

                // Send image to other players
				PacketByteBuf toSend = PacketByteBufs.copy(buf);
                ServerPlayNetworking.send(player, VideoFramePayload.VIDEO_FRAME_PAYLOAD_ID, toSend);
            }
        });

		ServerPlayNetworking.registerGlobalReceiver(DisableCameraPayload.DISABLE_CAMERA_PAYLOAD_ID, ((server, sender, handler, buf, responseSender) -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				PacketByteBuf toSend = PacketByteBufs.copy(buf);
				ServerPlayNetworking.send(player, DisableCameraPayload.DISABLE_CAMERA_PAYLOAD_ID, toSend);
			}
		}));
	}
}