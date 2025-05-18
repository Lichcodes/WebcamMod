package com.lichcode.webcam.render.buffer;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33.*;

public class DoublePBO {
    private int writePBO;
    private int readPBO;
    private int size;

    public DoublePBO(int size) {
        this.writePBO = glGenBuffers();
        this.readPBO = glGenBuffers();
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }

    public void writeAndSwap(ByteBuffer data) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, writePBO);
        glBufferData(GL_PIXEL_UNPACK_BUFFER, size, GL_DYNAMIC_DRAW);
        ByteBuffer writeBuffer = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY);
        if (writeBuffer != null) {
            writeBuffer.put(data);
            glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
        }
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        this.swap();
        data.flip();
    }

    public void bind() {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, readPBO);
    }

    public void unbind() {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    }

    public void swap() {
        int temp = readPBO;
        readPBO = writePBO;
        writePBO = temp;
    }
}
