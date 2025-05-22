package com.lichcode.webcam.screen;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.lichcode.webcam.WebcamMod;
import com.lichcode.webcam.WebcamModClient;
import com.lichcode.webcam.config.WebcamConfig;
import com.lichcode.webcam.video.VideoCamara;
import com.lichcode.webcam.video.VideoManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class SettingsScreen extends Screen {
    public static final int ELEMENT_HEIGHT = 20;
    public static final int ELEMENT_SPACING = 10;
    public float zoom = 1;
    private WebcamEntryList webcamEntryList;

    public SettingsScreen() {
        super(Text.of(WebcamMod.MOD_ID + " Settings"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawEntity(context, this.width/4, 0, this.width/4*3, this.height, 100 * zoom, 0.0625F, mouseX, mouseY, this.client.player);
        if (!this.webcamEntryList.canSwitch) {
            context.drawTooltip(this.textRenderer, Text.of("Opening webcam..."), this.width/4*2, 50);
        }
        if (!this.webcamEntryList.cameraEnabled) {
            context.drawTooltip(this.textRenderer, Text.of("Camera disabled"), this.width/4*2, 50);
        }
    }

    @Override
    public void init() {
        initCloseButton();
        initWebcamList();
        initCameraEnabled();
    }

    private void initCloseButton() {
        Text closeButtonTitle = Text.of("Close");
        int closeButtonWidth = 100;
        int closeButtonX = width - closeButtonWidth - ELEMENT_SPACING;
        int closeButtonY = height - ELEMENT_HEIGHT - ELEMENT_SPACING;
        ButtonWidget closeButton = ButtonWidget.builder(
                closeButtonTitle,
                button -> client.setScreen(null)
        ).dimensions(closeButtonX, closeButtonY, closeButtonWidth, ELEMENT_HEIGHT).build();

        addDrawableChild(closeButton);
    }

    private void initCameraEnabled() {
        SimpleOption<Boolean> cameraEnabledOption = SimpleOption.ofBoolean("Camara Enabled", WebcamConfig.isCameraEnabled(), enabled -> {
            WebcamConfig.setCameraEnabled(enabled);
            webcamEntryList.cameraEnabled = enabled;
            if (!enabled) {
                VideoManager.stopThread();
                WebcamModClient.sendDisableCameraPayload();
            } else {
                VideoManager.startCameraLoop();
            }
        });
        int width = this.width/4;
        OptionListWidget list = new OptionListWidget(this.client, width, 18, 0, this.height - 32, 25);
        list.addSingleOptionEntry(cameraEnabledOption);
        addDrawableChild(list);
    }

    private void initWebcamList() {
        List<String> webcams = VideoCamara.getWebcamList();

        int listWidth = this.width/4;
        int closeButtonY = this.height - ELEMENT_SPACING;
        WebcamEntryList listWidget = new WebcamEntryList(this.client, listWidth, closeButtonY, 32, this.height, 18);
        listWidget.setRenderHeader(true, 18);
        String currentWebcam = VideoCamara.getCurrentWebcam();
        for (String webcamName : webcams) {
            int index = listWidget.addEntry(webcamName);
            if (currentWebcam != null && currentWebcam.equals(webcamName)) {
                listWidget.setSelected(listWidget.getEntry(index));
            }
        }

        listWidget.onSelected((String name) -> {
            try {
                VideoCamara.setWebcamByName(name);
            } catch (WebcamException exception) {
                System.out.println(exception.getMessage());
            }
            return null;
        });

        listWidget.cameraEnabled = WebcamConfig.isCameraEnabled();
        this.webcamEntryList = listWidget;
        addDrawableChild(listWidget);
    }

    public static void drawEntity(DrawContext context, int x1, int y1, int x2, int y2, float size, float f, float mouseX, float mouseY, LivingEntity entity) {
        float g = (float)(x1 + x2) / 2.0F;
        float h = (float)(y1 + y2) / 2.0F;
        context.enableScissor(x1, y1, x2, y2);
        float i = (float)Math.atan((double)((g - mouseX) / 40.0F));
        float j = (float)Math.atan((double)((h - mouseY) / 40.0F));
        Quaternionf quaternionf = (new Quaternionf()).rotateZ((float)Math.PI);
        Quaternionf quaternionf2 = (new Quaternionf()).rotateX(j * 20.0F * ((float)Math.PI / 180F));
        quaternionf.mul(quaternionf2);
        float k = entity.bodyYaw;
        float l = entity.getYaw();
        float m = entity.getPitch();
        float n = entity.prevHeadYaw;
        float o = entity.headYaw;
        entity.bodyYaw = 180.0F + i * 20.0F;
        entity.setYaw(180.0F + i * 40.0F);
        entity.setPitch(-j * 20.0F);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();
        float p = entity.getScaleFactor();
        Vector3f vector3f = new Vector3f(0.0F, entity.getHeight() - 0.6f + f * p, 0.0F);
        float q = (float)size / p;
        drawEntity(context, g, h, q, vector3f, quaternionf, quaternionf2, entity);
        entity.bodyYaw = k;
        entity.setYaw(l);
        entity.setPitch(m);
        entity.prevHeadYaw = n;
        entity.headYaw = o;
        context.disableScissor();
    }

    public static void drawEntity(DrawContext context, float x, float y, float size, Vector3f vector3f, Quaternionf quaternionf, @Nullable Quaternionf quaternionf2, LivingEntity entity) {
        context.getMatrices().push();
        context.getMatrices().translate((double)x, (double)y, (double)50.0F);
        context.getMatrices().scale(size, size, -size);
        context.getMatrices().translate(vector3f.x, vector3f.y, vector3f.z - 5);
        context.getMatrices().multiply(quaternionf);
        context.draw();
        DiffuseLighting.method_34742();
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        if (quaternionf2 != null) {
            entityRenderDispatcher.setRotation(quaternionf2.conjugate(new Quaternionf()).rotateY((float)Math.PI));
        }

        entityRenderDispatcher.setRenderShadows(false);
        RenderSystem.runAsFancy(() -> entityRenderDispatcher.render(entity, (double)0.0F, (double)0.0F, (double)0.0F, 0.0F, 1.0F, context.getMatrices(), context.getVertexConsumers(), 15728880));
        context.draw();
        entityRenderDispatcher.setRenderShadows(true);
        context.getMatrices().pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX > this.width/4) {
            zoom += amount/10;
            if (zoom < 0.1) {
                zoom = 0.1f;
            } else if (zoom > 15) {
                zoom = 15;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Environment(EnvType.CLIENT)
    public class WebcamEntryList extends EntryListWidget<WebcamEntryList.WebcamEntry> {
        public boolean canSwitch = true;
        public boolean cameraEnabled = true;
        private Function<String,?> selectedCallback;

        public WebcamEntryList(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        public void setRenderHeader(boolean renderHeader, int headerHeight) {
            super.setRenderHeader(renderHeader, headerHeight);
        }

        public void onSelected(Function<String, ?> selectedCallback) {
            this.selectedCallback = selectedCallback;
        }

        @Override
        protected void renderHeader(DrawContext context, int x, int y) {
            context.drawCenteredTextWithShadow(SettingsScreen.this.textRenderer, Text.of("Select Webcam"), this.width/2, y, 0xFFFF00);
            context.draw();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            WebcamEntry entry = getEntryAtPosition(mouseX, mouseY);
            if (entry != null) {
                context.drawTooltip(SettingsScreen.this.textRenderer, Text.of(entry.text), mouseX, mouseY);
            }
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
        }

        public int addEntry(String text) {
            WebcamEntry webcamEntry = new WebcamEntry();
            webcamEntry.text = text;
            return super.addEntry(webcamEntry);
        }

        public WebcamEntry getEntry(int i) {
            return super.getEntry(i);
        }

        @Environment(EnvType.CLIENT)
        public class WebcamEntry extends EntryListWidget.Entry<WebcamEntry> {
            public String text;
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int centerX = WebcamEntryList.this.width/2;
                int textY = y + entryHeight / 2;
                String textToDraw = this.text;
                if (this.text.length() > 15) {
                   textToDraw = this.text.substring(0, Math.min(this.text.length(), 15)) + "...";
                }

                Text text = Text.of(textToDraw);
                int color = -1;
                if (!WebcamEntryList.this.canSwitch || !WebcamEntryList.this.cameraEnabled)  {
                    color = 0x5B5B54;
                }
                context.drawCenteredTextWithShadow(SettingsScreen.this.textRenderer, text, centerX, textY - 9 / 2, color);
            }


            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (WebcamEntryList.this.canSwitch && WebcamEntryList.this.cameraEnabled && this != WebcamEntryList.this.getSelectedOrNull()) {
                    WebcamEntryList.this.canSwitch = false;
                    WebcamEntryList.this.setSelected(this);
                    new Thread(() -> {
                        WebcamEntryList.this.selectedCallback.apply(this.text);
                        WebcamEntryList.this.canSwitch = true;
                    }).start();
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

    }
}
