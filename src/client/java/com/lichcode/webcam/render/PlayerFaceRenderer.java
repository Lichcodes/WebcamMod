package com.lichcode.webcam.render;

import com.lichcode.webcam.PlayerFeeds;
import com.lichcode.webcam.render.image.RenderableImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33.*;

public class PlayerFaceRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>  {

    public PlayerFaceRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (clientPlayNetworkHandler == null) {
            return;
        }

        PlayerListEntry playerListEntry = clientPlayNetworkHandler.getPlayerListEntry(entity.getUuid());;
        if (playerListEntry == null) {
            return;
        }

        String playerUUID = playerListEntry.getProfile().getId().toString();
        // Get the renderable image that represents the current video frame
        // if it is null, then we haven't received any video from them so we don't attempt to render
        RenderableImage image = PlayerFeeds.get(playerUUID);
        if (image == null) {
            return;
        }

        matrices.push();

        ModelPart head = getContextModel().head;
        head.rotate(matrices);

        matrices.translate(0, 0, -0.30);
        matrices.scale(0.25f, 0.5f, 1f);

        MatrixStack.Entry entry = matrices.peek();
        RenderSystem.getProjectionMatrix();
        Matrix4f position = new Matrix4f(entry.getPositionMatrix());
        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);

        buffer.vertex(position, 1, -1, 0).texture(0, 0).next();
        buffer.vertex(position, 1, 0, 0).texture(0, 1).next();
        buffer.vertex(position, -1, 0, 0).texture(1, 1).next();

        buffer.vertex(position, -1, 0, 0).texture(1, 1).next();
        buffer.vertex(position, 1, -1, 0).texture(0, 0).next();
        buffer.vertex(position, -1, -1, 0).texture(1, 0).next();

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        image.init();
        RenderSystem.setShaderTexture(0, image.id);
        // Set defaults because minecraft might change this during rendering
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4); // Default is 4
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        glPixelStorei(GL_UNPACK_IMAGE_HEIGHT, 0);
        glPixelStorei(GL_UNPACK_SKIP_IMAGES, 0);

        // Upload new image to texture from buffer
        glBindTexture(GL_TEXTURE_2D, image.id);
        image.buffer.bind();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image.width, image.height, GL_RGB, GL_UNSIGNED_BYTE, 0);
        image.buffer.unbind();

        image.buffer.writeAndSwap(image.data().duplicate());
        glBindTexture(GL_TEXTURE_2D, image.id);

        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        tessellator.draw();
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glBindTexture(GL_TEXTURE_2D, 0);

        matrices.pop();
    }
}
