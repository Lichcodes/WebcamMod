package com.lichcode.webcam.Video;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PlayerVideo {
    public String playerUUID;
    public byte[] frame;
    public int width;
    public int height;

    public PlayerVideo(int width, int height, String playerUUID) {
        this.width = width;
        this.height = height;
        this.playerUUID = playerUUID;
    }

    public void setFrame(byte[] frame) {
        this.frame = frame;
    }


    public ByteBuffer asByteBuffer() {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new ByteArrayInputStream(this.frame));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 更新宽高信息为实际图像的宽高
        this.width = image.getWidth();
        this.height = image.getHeight();

        // Create a ByteBuffer large enough for RGB
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 3);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                // Extract RGB components (no alpha)
                byte r = (byte) ((pixel >> 16) & 0xFF);
                byte g = (byte) ((pixel >> 8) & 0xFF);
                byte b = (byte) (pixel & 0xFF);

                buffer.put(r);
                buffer.put(g);
                buffer.put(b);
            }
        }

        buffer.flip(); // Prepare for reading by OpenGL
        return buffer;
    }

}
