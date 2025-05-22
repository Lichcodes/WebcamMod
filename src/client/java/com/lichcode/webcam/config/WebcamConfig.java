package com.lichcode.webcam.config;

import com.lichcode.webcam.WebcamMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class WebcamConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(WebcamMod.MOD_ID + " .properties");
    private static Properties properties;

    private static boolean cameraEnabled = true;
    private static String selectedCamera = "";

    public static void init() {
        properties = new Properties();

        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                properties.load(in);
                loadFromProperties();
            } catch (IOException e) {
                WebcamMod.LOGGER.error("Failed to load webcam config", e);
            }
        } else {
            saveConfig();
        }
    }

    public static void saveConfig() {
        properties.setProperty("camera_enabled", String.valueOf(cameraEnabled));
        properties.setProperty("selected_camera", selectedCamera);

        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            properties.store(out, "WebcamMod Configuration");
        } catch (IOException e) {
            WebcamMod.LOGGER.error("Failed to save webcam config", e);
        }
    }

    private static void loadFromProperties() {
        String enabledStr = properties.getProperty("camera_enabled");
        cameraEnabled = Boolean.parseBoolean(enabledStr);
        selectedCamera = properties.getProperty("selected_camera", selectedCamera);
    }

    public static boolean isCameraEnabled() {
        return cameraEnabled;
    }

    public static void setCameraEnabled(boolean cameraEnabled) {
        WebcamConfig.cameraEnabled = cameraEnabled;
        saveConfig();
    }

    public static String getSelectedCamera() {
        return selectedCamera;
    }

    public static void setSelectedCamera(String selectedCamera) {
        WebcamConfig.selectedCamera = selectedCamera;
        saveConfig();
    }
}
