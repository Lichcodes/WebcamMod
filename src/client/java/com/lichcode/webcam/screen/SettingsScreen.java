package com.lichcode.webcam.screen;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.lichcode.webcam.WebcamMod;
import com.lichcode.webcam.config.WebcamConfig;
import com.lichcode.webcam.video.VideoCamara;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;

public class SettingsScreen extends Screen {
    public static final int ELEMENT_HEIGHT = 20;
    public static final int ELEMENT_SPACING = 10;
    public float zoom = 1;
    private WebcamEntryList webcamEntryList;
    
    // 显示模式选择按钮
    private CyclingButtonWidget<WebcamConfig.DisplayMode> displayModeButton;
    
    // 选框裁剪模式参数滑块
    private SliderWidget cropBoxXSlider;
    private SliderWidget cropBoxYSlider;
    private SliderWidget cropBoxSizeSlider;

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
    }
    
    private String getDisplayModeDescription(WebcamConfig.DisplayMode mode) {
        switch (mode) {
            case STRETCH_FILL:
                return "拉伸填充模式：摄像头画面被强制拉伸为正方形";
            case CROP_BOX:
                return "选框裁剪模式：选择画面部分区域进行显示";
            default:
                return "";
        }
    }

    @Override
    public void init() {
        initCloseButton();
        initWebcamList();
        initDisplayModeControls();
        updateCropBoxControlsVisibility();
    }

    private void initCloseButton() {
        Text closeButtonTitle = Text.of("关闭");
        int closeButtonWidth = 180; // 与其他按钮保持一致的宽度
        int closeButtonX = this.width - closeButtonWidth - ELEMENT_SPACING; // 与其他按钮对齐
        int closeButtonY = ELEMENT_SPACING * 2; // 放在最顶部
        ButtonWidget closeButton = ButtonWidget.builder(
                closeButtonTitle,
                button -> client.setScreen(null)
        ).dimensions(closeButtonX, closeButtonY, closeButtonWidth, ELEMENT_HEIGHT).build();

        addDrawableChild(closeButton);
    }

    private void initWebcamList() {
        List<String> webcams = VideoCamara.getWebcamList();

        int listWidth = this.width/4;
        int closeButtonY = this.height - ELEMENT_SPACING;
        WebcamEntryList listWidget = new WebcamEntryList(this.client, listWidth, closeButtonY, ELEMENT_SPACING, 18);
        String currentWebcam = VideoCamara.getCurrentWebcam();
        for (String webcamName : webcams) {
            WebcamEntryList.WebcamEntry entry = listWidget.addEntry(webcamName);
            if (currentWebcam != null && currentWebcam.equals(webcamName)) {
                listWidget.setSelected(entry);
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

        this.webcamEntryList = listWidget;
        addDrawableChild(listWidget);
    }
    
    private void initDisplayModeControls() {
        // 控件大小和位置配置
        int buttonWidth = 180; // 减小宽度以适应右侧
        int rightSideX = this.width - buttonWidth - ELEMENT_SPACING; // 右侧起始X坐标
        int startY = ELEMENT_SPACING * 2 + ELEMENT_HEIGHT + ELEMENT_SPACING; // 从关闭按钮下方开始布局
        
        // 显示模式切换按钮
        displayModeButton = CyclingButtonWidget.<WebcamConfig.DisplayMode>builder(mode -> {
                String label = switch (mode) {
                    case STRETCH_FILL -> "拉伸填充模式";
                    case CROP_BOX -> "选框裁剪模式";
                };
                return Text.of("显示模式: " + label);
            })
            .values(WebcamConfig.DisplayMode.values())
            .initially(WebcamConfig.getDisplayMode())
            .build(rightSideX, startY, buttonWidth, ELEMENT_HEIGHT, Text.of("显示模式"), 
                (button, mode) -> {
                    WebcamConfig.setDisplayMode(mode);
                    updateCropBoxControlsVisibility();
                });
        
        addDrawableChild(displayModeButton);
        
        // 右侧控件间距为单倍
        int controlSpacing = ELEMENT_SPACING + ELEMENT_HEIGHT;
        
        // 选框X位置滑块 - 位于显示模式按钮下方
        cropBoxXSlider = new ModSliderWidget(
                rightSideX, 
                startY + controlSpacing, 
                buttonWidth, 
                ELEMENT_HEIGHT, 
                Text.of("选框X位置: " + String.format("%.2f", WebcamConfig.getCropBoxX())),
                WebcamConfig.getCropBoxX()) {
            
            @Override
            protected void updateMessage() {
                setMessage(Text.of("选框X位置: " + String.format("%.2f", this.value)));
            }
            
            @Override
            protected void applyValue() {
                WebcamConfig.setCropBoxX((float) this.value);
                // 实时更新裁剪预览
                updateCropPreview();
            }
            
            @Override
            public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
                super.onDrag(mouseX, mouseY, deltaX, deltaY);
                // 拖动时实时更新
                updateCropPreview();
            }
        };
        
        // 选框Y位置滑块 - 位于X位置滑块下方
        cropBoxYSlider = new ModSliderWidget(
                rightSideX, 
                startY + 2 * controlSpacing, 
                buttonWidth, 
                ELEMENT_HEIGHT, 
                Text.of("选框Y位置: " + String.format("%.2f", WebcamConfig.getCropBoxY())),
                WebcamConfig.getCropBoxY()) {
            
            @Override
            protected void updateMessage() {
                setMessage(Text.of("选框Y位置: " + String.format("%.2f", this.value)));
            }
            
            @Override
            protected void applyValue() {
                WebcamConfig.setCropBoxY((float) this.value);
                // 实时更新裁剪预览
                updateCropPreview();
            }
            
            @Override
            public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
                super.onDrag(mouseX, mouseY, deltaX, deltaY);
                // 拖动时实时更新
                updateCropPreview();
            }
        };
        
        // 选框大小滑块 - 位于Y位置滑块下方
        cropBoxSizeSlider = new ModSliderWidget(
                rightSideX, 
                startY + 3 * controlSpacing, 
                buttonWidth, 
                ELEMENT_HEIGHT, 
                Text.of("选框大小: " + String.format("%.2f", WebcamConfig.getCropBoxSize())),
                WebcamConfig.getCropBoxSize(), 0.1, 1.0) {
            
            @Override
            protected void updateMessage() {
                setMessage(Text.of("选框大小: " + String.format("%.2f", this.value)));
            }
            
            @Override
            protected void applyValue() {
                WebcamConfig.setCropBoxSize((float) this.value);
                // 实时更新裁剪预览
                updateCropPreview();
            }
            
            @Override
            public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
                super.onDrag(mouseX, mouseY, deltaX, deltaY);
                // 拖动时实时更新
                updateCropPreview();
            }
        };
        
        addDrawableChild(cropBoxXSlider);
        addDrawableChild(cropBoxYSlider);
        addDrawableChild(cropBoxSizeSlider);
    }
    
    private void updateCropBoxControlsVisibility() {
        boolean isVisible = WebcamConfig.getDisplayMode() == WebcamConfig.DisplayMode.CROP_BOX;
        cropBoxXSlider.active = isVisible;
        cropBoxYSlider.active = isVisible;
        cropBoxSizeSlider.active = isVisible;
        cropBoxXSlider.visible = isVisible;
        cropBoxYSlider.visible = isVisible;
        cropBoxSizeSlider.visible = isVisible;
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
        float p = entity.getScale();
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
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        entityRenderDispatcher.render(entity, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, context.getMatrices(), immediate, 15728880);
        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);
        context.getMatrices().pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX > this.width/4) {
            zoom += verticalAmount/10;
            if (zoom < 0.1) {
                zoom = 0.1f;
            } else if (zoom > 15) {
                zoom = 15;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    // 自定义滑块类，用于裁剪参数调整
    public abstract class ModSliderWidget extends SliderWidget {
        private final double min;
        private final double max;
        
        public ModSliderWidget(int x, int y, int width, int height, Text text, double value) {
            this(x, y, width, height, text, value, 0.0, 1.0);
        }
        
        public ModSliderWidget(int x, int y, int width, int height, Text text, double value, double min, double max) {
            super(x, y, width, height, text, value);
            this.min = min;
            this.max = max;
        }
        
        @Override
        protected void updateMessage() {
            setMessage(Text.of("Value: " + value));
        }
        
        @Override
        protected void applyValue() {}
        
        // 获取实际的值（在min和max之间映射）
        public double getValue() {
            return min + (max - min) * this.value;
        }
    }

    @Environment(EnvType.CLIENT)
    public class WebcamEntryList extends EntryListWidget<WebcamEntryList.WebcamEntry> {
        public boolean canSwitch = true;
        private Function<String,?> selectedCallback;

        public WebcamEntryList(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        public void onSelected(Function<String, ?> selectedCallback) {
            this.selectedCallback = selectedCallback;
        }

        @Override
        protected void renderHeader(DrawContext context, int x, int y) {
            context.drawCenteredTextWithShadow(SettingsScreen.this.textRenderer, Text.of("选择摄像头"), this.width/2, y, 0xFFFF00);
            context.draw();
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderWidget(context, mouseX, mouseY, delta);
            WebcamEntry entry = getEntryAtPosition(mouseX, mouseY);
            if (entry != null) {
                context.drawTooltip(SettingsScreen.this.textRenderer, Text.of(entry.text), mouseX, mouseY);
            }
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {

        }

        public WebcamEntry addEntry(String text) {
            WebcamEntry webcamEntry = new WebcamEntry();
            webcamEntry.text = text;
            super.addEntry(webcamEntry);
            return webcamEntry;
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
                if (!WebcamEntryList.this.canSwitch)  {
                    color = 0x5B5B54;
                }
                context.drawCenteredTextWithShadow(SettingsScreen.this.textRenderer, text, centerX, textY - 9 / 2, color);
            }


            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (WebcamEntryList.this.canSwitch && this != WebcamEntryList.this.getSelectedOrNull()) {
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

    // 新增：更新裁剪预览的方法
    private void updateCropPreview() {
        // 仅在选框裁剪模式下处理
        if (WebcamConfig.getDisplayMode() != WebcamConfig.DisplayMode.CROP_BOX) {
            return;
        }
        
        // 通过临时传入当前的裁剪参数，触发图像处理更新
        try {
            // 这会触发VideoCamara类使用最新的裁剪参数来处理图像
            // 下一帧渲染时就会显示更新后的效果
            MinecraftClient.getInstance().send(() -> {
                WebcamConfig.saveConfig(); // 确保新参数被保存
            });
        } catch (Exception e) {
            // 忽略可能的异常
        }
    }
}
