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
        String selectedCamera = WebcamConfig.getSelectedCamera();
        if (!selectedCamera.isEmpty()) {
            try {
                Webcam wc = Webcam.getWebcamByName(selectedCamera);
                if (wc != null) {
                    wc.open();
                    webcam = wc;
                    return;
                }

                WebcamMod.LOGGER.info("Can not find selected camera {}, trying others.", selectedCamera);
            } catch (WebcamException e) {
                WebcamMod.LOGGER.info("Can not open selected camera {}, trying others.", selectedCamera);
            }
        }


        for(Webcam wc : Webcam.getWebcams()) {
            try {
                wc.open();
                webcam = wc;
                WebcamMod.LOGGER.info("Using webcam: {}", wc.getName());
                WebcamConfig.setSelectedCamera(webcam.getName());
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
        WebcamConfig.setSelectedCamera(webcam.getName());
    }

    public static String getCurrentWebcam() {
        if (webcam == null) {
            return null;
        }

        return webcam.getName();
    }

    public static void get(PlayerVideo playerVideo) throws IOException {
        BufferedImage image = webcam.getImage();
        // Resize image to defined size
        // TODO: maybe add x/y offset so user can define size
        image = resize(image, playerVideo.width, playerVideo.height);

        // Compress image using JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = writers.next();
        JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
        jpegParams.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
        jpegParams.setCompressionQuality(1);
        writer.setOutput(ios);
        IIOImage outputImage = new IIOImage(image, null, null);

        writer.write(null, outputImage, jpegParams);
        writer.dispose();
        ios.close();
        // Save compressed image to object
        playerVideo.setFrame(baos.toByteArray());
    }

    public static BufferedImage resize(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();

        return resized;
    }
}
