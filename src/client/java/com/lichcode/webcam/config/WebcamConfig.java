package com.lichcode.webcam.config;

import com.lichcode.webcam.WebcamMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class WebcamConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(WebcamMod.MOD_ID + ".properties");
    private static Properties properties;

    // 显示模式枚举
    public enum DisplayMode {
        STRETCH_FILL,  // 拉伸填充模式
        CROP_BOX       // 选框裁剪模式
    }

    // 默认配置
    private static DisplayMode displayMode = DisplayMode.STRETCH_FILL;
    private static float cropBoxX = 0.5f;  // 选框X中心位置 (0-1)
    private static float cropBoxY = 0.5f;  // 选框Y中心位置 (0-1)
    private static float cropBoxSize = 1.0f;  // 选框大小 (0.1-1)

    // 初始化配置
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

    // 保存配置
    public static void saveConfig() {
        saveToProperties();
        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            properties.store(out, "WebcamMod Configuration");
        } catch (IOException e) {
            WebcamMod.LOGGER.error("Failed to save webcam config", e);
        }
    }

    // 将当前配置保存到Properties对象
    private static void saveToProperties() {
        properties.setProperty("display_mode", displayMode.name());
        properties.setProperty("crop_box_x", String.valueOf(cropBoxX));
        properties.setProperty("crop_box_y", String.valueOf(cropBoxY));
        properties.setProperty("crop_box_size", String.valueOf(cropBoxSize));
    }

    // 从Properties对象加载配置
    private static void loadFromProperties() {
        String modeStr = properties.getProperty("display_mode", DisplayMode.STRETCH_FILL.name());
        try {
            displayMode = DisplayMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            WebcamMod.LOGGER.warn("Invalid display mode in config: {}", modeStr);
            displayMode = DisplayMode.STRETCH_FILL;
        }

        cropBoxX = parseFloat(properties.getProperty("crop_box_x"), 0.5f);
        cropBoxY = parseFloat(properties.getProperty("crop_box_y"), 0.5f);
        cropBoxSize = parseFloat(properties.getProperty("crop_box_size"), 1.0f);
        
        // 确保值在有效范围内
        cropBoxX = Math.max(0, Math.min(1, cropBoxX));
        cropBoxY = Math.max(0, Math.min(1, cropBoxY));
        cropBoxSize = Math.max(0.1f, Math.min(1, cropBoxSize));
    }

    private static float parseFloat(String value, float defaultValue) {
        try {
            return value != null ? Float.parseFloat(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Getter 和 Setter
    public static DisplayMode getDisplayMode() {
        return displayMode;
    }

    public static void setDisplayMode(DisplayMode mode) {
        displayMode = mode;
        saveConfig();
    }

    public static float getCropBoxX() {
        return cropBoxX;
    }

    public static void setCropBoxX(float x) {
        cropBoxX = Math.max(0, Math.min(1, x));
        saveConfig();
    }

    public static float getCropBoxY() {
        return cropBoxY;
    }

    public static void setCropBoxY(float y) {
        cropBoxY = Math.max(0, Math.min(1, y));
        saveConfig();
    }

    public static float getCropBoxSize() {
        return cropBoxSize;
    }

    public static void setCropBoxSize(float size) {
        cropBoxSize = Math.max(0.1f, Math.min(1, size));
        saveConfig();
    }
} 