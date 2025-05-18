package com.lichcode.webcam.render;

import com.lichcode.webcam.PlayerFeeds;
import com.lichcode.webcam.Video.PlayerVideo;
import com.lichcode.webcam.config.WebcamConfig;
import com.lichcode.webcam.render.image.RenderableImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33.*;

public class PlayerFaceRenderer extends FeatureRenderer<PlayerEntity, PlayerEntityModel<PlayerEntity>> {
    
    @SuppressWarnings("unchecked")
    public PlayerFaceRenderer(PlayerEntityRenderer playerRenderer) {
        super((FeatureRendererContext<PlayerEntity, PlayerEntityModel<PlayerEntity>>)(Object)playerRenderer);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntity player, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (clientPlayNetworkHandler == null) {
            return;
        }
        PlayerListEntry playerListEntry = clientPlayNetworkHandler.getPlayerListEntry(player.getUuid());
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

        // 获取玩家视频信息
        PlayerVideo video = PlayerFeeds.getPlayerVideo(playerUUID);
        if (video == null) {
            return;
        }

        // 保存当前的RenderSystem状态并设置深度测试
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL_LEQUAL);
        
        matrices.push();

        ModelPart head = getContextModel().head;
        head.rotate(matrices);

        // 将摄像头放在脸部前面，稍微靠前
        // 关键修复：从-0.30调整到-0.31，防止与披风产生Z-fighting
        matrices.translate(0, 0, -0.31);

        // 对所有模式使用相同的缩放比例
        matrices.scale(0.25f, 0.5f, 1f);

        // 准备渲染数据
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();

        // 使用图像纹理
        image.init();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, image.id);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        
        // 上传新图像到纹理
        glBindTexture(GL_TEXTURE_2D, image.id);
        image.buffer.bind();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image.width, image.height, GL_RGB, GL_UNSIGNED_BYTE, 0);
        image.buffer.unbind();
        image.buffer.writeAndSwap(image.data().duplicate());

        // 关键修复：使用深度测试防止穿模，但只对本渲染进行深度测试
        RenderSystem.disableCull();  // 禁用面剔除
        RenderSystem.depthMask(true); // 允许写入深度缓冲区
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 使用Tessellator直接渲染，这在1.21.1中是最可靠的方式
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);
        
        buffer.vertex(positionMatrix, 1, -1, 0).texture(0, 0);
        buffer.vertex(positionMatrix, 1, 0, 0).texture(0, 1);
        buffer.vertex(positionMatrix, -1, 0, 0).texture(1, 1);
        
        buffer.vertex(positionMatrix, -1, 0, 0).texture(1, 1);
        buffer.vertex(positionMatrix, -1, -1, 0).texture(1, 0);
        buffer.vertex(positionMatrix, 1, -1, 0).texture(0, 0);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // 恢复OpenGL状态
        glBindTexture(GL_TEXTURE_2D, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthFunc(GL_LEQUAL);
        
        matrices.pop();
    }
}
