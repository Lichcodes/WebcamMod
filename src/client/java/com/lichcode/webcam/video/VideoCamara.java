package com.lichcode.webcam.video;

import com.lichcode.webcam.WebcamMod;
import com.lichcode.webcam.Video.PlayerVideo;
import com.github.sarxos.webcam.*;
import com.lichcode.webcam.config.WebcamConfig;
import org.jetbrains.annotations.Nullable;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;


public class VideoCamara {
    private static Webcam webcam;

    public static void init() {
        for(Webcam wc : Webcam.getWebcams()) {
            try {
                wc.open();
                webcam = wc;
                WebcamMod.LOGGER.info("Using webcam: {}", wc.getName());
                return;
            } catch (WebcamException e) {
                WebcamMod.LOGGER.info("Webcam {} not usable, trying next one.", wc.getName());
            }
        }

        throw new WebcamLockException("All webcams in use!");
    }

    public static void release() {
        webcam.close();
    }

    public static List<String> getWebcamList() {
        return Webcam.getWebcams().stream().map((wc) -> wc.getName()).toList();
    }

    public static void setWebcamByName(String name) {
        Webcam wc = Webcam.getWebcamByName(name);
        if (wc == null) {
            throw new WebcamException("Webcam not found");
        }
        webcam.close();
        try {
            wc.open();
        } catch (WebcamException e) {
            throw e;
        }

        webcam = wc;
    }

    public static String getCurrentWebcam() {
        if (webcam == null) {
            return null;
        }

        return webcam.getName();
    }

    public static void get(PlayerVideo playerVideo) throws IOException {
        BufferedImage image = webcam.getImage();
        
        // 根据显示模式处理图像
        WebcamConfig.DisplayMode mode = WebcamConfig.getDisplayMode();
        
        switch (mode) {
            case STRETCH_FILL:
                // 拉伸填充模式：直接拉伸为正方形
                image = resizeStretch(image, playerVideo.width, playerVideo.width);
                break;
                
            case CROP_BOX:
                // 选框裁剪模式：基于选框位置和大小裁剪
                image = cropByBox(image, playerVideo.width);
                break;
        }

        // 压缩图像使用JPEG格式，70%质量以平衡文件大小和质量
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = writers.next();
        JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
        jpegParams.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
        // 设置压缩质量为70%以减少网络传输负载
        jpegParams.setCompressionQuality(0.7f);
        writer.setOutput(ios);
        IIOImage outputImage = new IIOImage(image, null, null);

        writer.write(null, outputImage, jpegParams);
        writer.dispose();
        ios.close();
        // Save compressed image to object
        playerVideo.setFrame(baos.toByteArray());
    }

    // 拉伸填充模式：强制拉伸为正方形
    private static BufferedImage resizeStretch(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }
    
    // 选框裁剪模式：基于选框位置和大小裁剪
    private static BufferedImage cropByBox(BufferedImage original, int targetSize) {
        // 获取最新的裁剪参数，确保使用当前设置
        float centerX = WebcamConfig.getCropBoxX();
        float centerY = WebcamConfig.getCropBoxY();
        float boxSize = WebcamConfig.getCropBoxSize();
        
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        
        // 确保参数在有效范围内
        centerX = Math.max(0, Math.min(1, centerX));
        centerY = Math.max(0, Math.min(1, centerY));
        boxSize = Math.max(0.1f, Math.min(1, boxSize));
        
        // 确定基准尺寸，使用原始图像中较短的边
        int referenceDimension = Math.min(originalWidth, originalHeight);
        
        // 计算实际裁剪区域大小 (越小的boxSize意味着选择的区域越小，实际显示效果看起来越放大)
        int cropSize = (int)(referenceDimension * boxSize);
        
        // 确保cropSize至少为1像素
        cropSize = Math.max(1, cropSize);
        
        // 计算裁剪起始点，以确保裁剪区域为正方形
        int cropX = (int)(originalWidth * centerX - cropSize / 2.0f);
        int cropY = (int)(originalHeight * centerY - cropSize / 2.0f);
        
        // 确保裁剪区域在图像范围内
        cropX = Math.max(0, Math.min(originalWidth - cropSize, cropX));
        cropY = Math.max(0, Math.min(originalHeight - cropSize, cropY));
        
        // 如果计算出的裁剪区域超出边界，需要调整大小
        if (cropX + cropSize > originalWidth) {
            cropSize = originalWidth - cropX;
        }
        if (cropY + cropSize > originalHeight) {
            cropSize = originalHeight - cropY;
        }
        
        // 再次确保cropSize至少为1像素
        cropSize = Math.max(1, cropSize);
        
        try {
            // 裁剪图像
            BufferedImage cropped = original.getSubimage(cropX, cropY, cropSize, cropSize);
            
            // 调整到目标大小
            BufferedImage resized = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(cropped, 0, 0, targetSize, targetSize, null);
            g.dispose();
            
            return resized;
        } catch (Exception e) {
            // 如果裁剪失败（例如参数无效），则回退到拉伸模式
            WebcamMod.LOGGER.error("裁剪失败，回退到拉伸模式", e);
            return resizeStretch(original, targetSize, targetSize);
        }
    }

    // 为了兼容性保留原方法
    public static BufferedImage resize(BufferedImage original, int width, int height) {
        return resizeStretch(original, width, height);
    }
}
