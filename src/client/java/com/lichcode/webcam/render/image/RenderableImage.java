package com.lichcode.webcam.render.image;

import com.lichcode.webcam.Video.PlayerVideo;
import com.lichcode.webcam.render.buffer.DoublePBO;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33.*;

public class RenderableImage {
    public ByteBuffer data;
    public int width;
    public int height;
    public int id;
    public DoublePBO buffer;

    public void init() {
        if (id == 0) {
            id = glGenTextures();
            buffer = new DoublePBO(this.width * this.height * 3);
        } else if (buffer.getSize() < this.width * this.height * 3) {
            // 如果图像大小变化，需要更新buffer的大小
            buffer = new DoublePBO(this.width * this.height * 3);
        }

        glBindTexture(GL_TEXTURE_2D, this.id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, this.width, this.height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void fill(PlayerVideo video) {
        this.width = video.width;
        this.height = video.height;
        this.data = video.asByteBuffer();
    }

    public ByteBuffer data() {
        return this.data;
    }
}