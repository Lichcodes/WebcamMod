package com.lichcode.webcam;

import com.lichcode.webcam.Video.PlayerVideo;
import com.lichcode.webcam.Video.PlayerVideoPacketCodec;
import com.lichcode.webcam.config.WebcamConfig;
import com.lichcode.webcam.render.PlayerFaceRenderer;

import com.lichcode.webcam.screen.SettingsScreen;
import com.lichcode.webcam.video.VideoManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.*;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import static com.lichcode.webcam.WebcamMod.MOD_ID;


public class WebcamModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		WebcamConfig.init();
		registerSettingsCommand();

		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof PlayerEntityRenderer) {
				registrationHelper.register(new PlayerFaceRenderer((PlayerEntityRenderer) entityRenderer));
			}
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (WebcamConfig.isCameraEnabled()) {
				VideoManager.startCameraLoop();
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> {
			VideoManager.stopThread();
		}));

        ClientPlayNetworking.registerGlobalReceiver(VideoFramePayload.VIDEO_FRAME_PAYLOAD_ID, (minecraftClient, clientPlayNetworkHandler, packetByteBuf, packetSender) -> {
			packetByteBuf.resetReaderIndex();
			PlayerVideo playerVideo = PlayerVideoPacketCodec.PACKET_CODEC.decode(packetByteBuf);
			PlayerFeeds.update(playerVideo);
        });

		ClientPlayNetworking.registerGlobalReceiver(DisableCameraPayload.DISABLE_CAMERA_PAYLOAD_ID, (minecraftClient, clientPlayNetworkHandler, packetByteBuf, packetSender) -> {
			packetByteBuf.resetReaderIndex();
			String playerUUID = packetByteBuf.readString();
			PlayerFeeds.remove(playerUUID);
        });
	}


	private void registerSettingsCommand() {
		ClientCommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess) -> {
			commandDispatcher.register(ClientCommandManager.literal("webcam-settings").executes(context ->  {
				MinecraftClient client = context.getSource().getClient();

				client.send(() -> client.setScreen(new SettingsScreen()));
				return 1;
			}));
		}));
	}

	public static void sendDisableCameraPayload() {
		PacketByteBuf buf = PacketByteBufs.create();
		String uuid = MinecraftClient.getInstance().player.getUuid().toString();
		buf.writeString(uuid);
		buf.resetReaderIndex();
		ClientPlayNetworking.send(DisableCameraPayload.DISABLE_CAMERA_PAYLOAD_ID, buf);
	}
}
