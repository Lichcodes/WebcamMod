package com.lichcode.webcam.video;

import com.lichcode.webcam.WebcamMod;
import com.lichcode.webcam.PlayerFeeds;
import com.lichcode.webcam.Video.PlayerVideo;
import com.lichcode.webcam.VideoFramePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.util.Date;

public class VideoManager  {
    public static boolean running = false;
    public static PlayerVideo videoFeed;

    public static void startCameraLoop() {
        running = true;
        videoFeed = new PlayerVideo(128, 128, MinecraftClient.getInstance().player.getUuidAsString());
        new Thread(() -> {
            VideoCamara.init();
            WebcamMod.LOGGER.info("Camera loop started");

            // Start behind to run first loop
            Date nextUpdate = new Date();
            while(running) {
                // 保持10fps (每100ms一帧)
                Date now = new Date();
                if (now.toInstant().isAfter(nextUpdate.toInstant())) {
                    // 100 millis into the future for 10fps
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
            if (ClientPlayNetworking.canSend(VideoFramePayload.ID)) {
                // Update my own player feed for rendering my own face (when you press F5)
                PlayerFeeds.update(videoFeed);
                // Send video to server
                ClientPlayNetworking.send(new VideoFramePayload(videoFeed));
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
