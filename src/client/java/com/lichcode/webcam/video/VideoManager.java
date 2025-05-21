package com.lichcode.webcam.video;

import com.lichcode.webcam.Video.PlayerVideoPacketCodec;
import com.lichcode.webcam.WebcamMod;
import com.lichcode.webcam.PlayerFeeds;
import com.lichcode.webcam.Video.PlayerVideo;
import com.lichcode.webcam.VideoFramePayload;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.io.IOException;
import java.util.Date;

public class VideoManager  {
    public static boolean running = false;
    public static PlayerVideo videoFeed;

    public static void startCameraLoop() {
        running = true;
        videoFeed = new PlayerVideo(200, 200, MinecraftClient.getInstance().player.getUuidAsString());
        new Thread(() -> {
            VideoCamara.init();
            WebcamMod.LOGGER.info("Camera loop started");

            // Start behind to run first loop
            Date nextUpdate = new Date();
            while(running) {
                // Target 30fps sent to the server, I don't know if this actually helps
                Date now = new Date();
                if (now.toInstant().isAfter(nextUpdate.toInstant())) {
                    // 50 millis into the future
                    nextUpdate.setTime(now.getTime() + 100);
                    loop();
                }
            }
            WebcamMod.LOGGER.info("Camera loop stopped");
            VideoCamara.release();
        }).start();
    }

    public static void loop() {
        try {
            VideoCamara.get(videoFeed);
            if (ClientPlayNetworking.canSend(VideoFramePayload.VIDEO_FRAME_PAYLOAD_ID)) {
                // Update my own player feed for rendering my own face (when you press F5)
                PlayerFeeds.update(videoFeed);
                // Send video to server
                PacketByteBuf buf = PacketByteBufs.create();
                PlayerVideoPacketCodec.PACKET_CODEC.encode(buf, videoFeed);
                buf.resetReaderIndex();
                ClientPlayNetworking.send(VideoFramePayload.VIDEO_FRAME_PAYLOAD_ID, buf);
            } else {
                WebcamMod.LOGGER.warn("Could not send video frame, network handler is null???");
            }
        } catch (IOException e) {
            WebcamMod.LOGGER.error("Could not get image from webcam", e);
        }
    }

    public static void stopThread() {
        running = false;
    }
}
