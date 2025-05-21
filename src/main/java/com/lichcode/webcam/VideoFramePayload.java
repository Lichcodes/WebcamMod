package com.lichcode.webcam;

import com.lichcode.webcam.Video.PlayerVideo;
import net.minecraft.util.Identifier;

import static com.lichcode.webcam.WebcamMod.MOD_ID;

public record VideoFramePayload(PlayerVideo video) {
    public static final Identifier VIDEO_FRAME_PAYLOAD_ID = Identifier.of(MOD_ID, "video_frame");
}
