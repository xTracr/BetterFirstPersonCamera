package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public class ModelViewScreen extends Screen {
    private static final String KEY_SCREEN = "screen.xtracr_" + RealCamera.MODID + "_modelView_";
    private static final String KEY_WIDGET = "screen.widget.xtracr_" + RealCamera.MODID + "_modelView_";
    private static final String KEY_TOOLTIP = "screen.tooltip.xtracr_" + RealCamera.MODID + "_modelView_";
    private static final Map<String, Text> selectingText = Map.of("forward", Text.translatable(KEY_WIDGET + "forwardMode").styled(s -> s.withColor(Formatting.GREEN)),
            "upward", Text.translatable(KEY_WIDGET + "upwardMode").styled(s -> s.withColor(Formatting.RED)),
            "pos", Text.translatable(KEY_WIDGET + "posMode").styled(s -> s.withColor(Formatting.BLUE)));
    protected int xSize = 420, ySize = 220, widgetWidth = (xSize - ySize) / 4 - 10, widgetHeight = 17;
    protected int x, y;
    private boolean shouldPause = false, showCube = false;
    private int entitySize = 80, layers = 0;
    private double entityX, entityY;
    private float yaw, pitch, xRot, yRot;
    private String focusedTextureId, selecting = "forward";
    private Pair<Float, Float> focusedUV;
    private FloatFieldWidget forwardUField, forwardVField, upwardUField, upwardVField, posUField, posVField;
    private TextFieldWidget textureIdField, nameField;
    private DoubleValueSlider yawSlider, pitchSlider;

    public ModelViewScreen() {
        super(Text.translatable(KEY_SCREEN + "title"));
    }

    @Override
    protected void init() {
        super.init();
        x = (width - xSize) / 2;
        y = (height - ySize) / 2;
        initLeftWidgets();
        initRightWidgets();
    }

    private void initLeftWidgets() {
        GridWidget gridWidget = new GridWidget();
        gridWidget.getMainPositioner().margin(5, 4, 0, 0);
        GridWidget.Adder adder = gridWidget.createAdder(2);
        adder.add(createButton("reset",widgetWidth * 2 + 5, button -> reset()), 2);
        adder.add(yawSlider = new DoubleValueSlider(widgetWidth * 2 + 5, widgetHeight, 0.5D,
                -60.0D, 60.0D, d -> Text.translatable(KEY_WIDGET + "yaw", MathUtil.round(d, 2)), d -> yaw = (float) d), 2);
        adder.add(pitchSlider = new DoubleValueSlider(widgetWidth * 2 + 5, widgetHeight, 0.5D,
                -90.0D, 90.0D, d -> Text.translatable(KEY_WIDGET + "pitch", MathUtil.round(d, 2)), d -> pitch = (float) d), 2);
        adder.add(createButton(Text.translatable(KEY_WIDGET + "selectMode", selectingText.get(selecting)), widgetWidth * 2 + 5, button -> {
                    if (selecting.equals("forward")) selecting = "upward";
                    else if (selecting.equals("upward")) selecting = "pos";
                    else selecting = "forward";
                    button.setMessage(Text.translatable(KEY_WIDGET + "selectMode", selectingText.get(selecting)));
                }), 2).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "selectMode")));
        adder.add(forwardUField = createFloatField(widgetWidth, forwardUField));
        adder.add(forwardVField = createFloatField(widgetWidth, forwardVField));
        adder.add(upwardUField = createFloatField(widgetWidth, upwardUField));
        adder.add(upwardVField = createFloatField(widgetWidth, upwardVField));
        adder.add(posUField = createFloatField(widgetWidth, posUField));
        adder.add(posVField = createFloatField(widgetWidth, posVField));
        adder.add(textureIdField = createTextField(widgetWidth * 2 + 5, textureIdField),2).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "textureId")));
        adder.add(createButton("save", widgetWidth, button -> saveConfig()));
        adder.add(createButton("load", widgetWidth, button -> loadConfig()));
        adder.add(nameField = createTextField(widgetWidth * 2 + 5, nameField),2).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "listName")));
        textureIdField.setMaxLength(1024);
        nameField.setMaxLength(20);
        gridWidget.refreshPositions();
        SimplePositioningWidget.setPos(gridWidget, x, y, x + (xSize - ySize) / 2 - 5, y + ySize, 0, 0);
        gridWidget.forEachChild(this::addDrawableChild);
    }

    private void initRightWidgets() {
        GridWidget gridWidget = new GridWidget();
        gridWidget.getMainPositioner().margin(5, 4, 0, 0);
        GridWidget.Adder adder = gridWidget.createAdder(2);
        adder.add(createButton("pause", widgetWidth * 2 + 5, button -> shouldPause = !shouldPause), 2);
        adder.add(createButton("showCube", widgetWidth * 2 + 5, button -> showCube = !showCube), 2);
        gridWidget.refreshPositions();
        SimplePositioningWidget.setPos(gridWidget, x + (xSize + ySize) / 2 + 5, y, x + xSize, y +ySize, 0, 0);
        gridWidget.forEachChild(this::addDrawableChild);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);// 1.20.1 only
        super.render(context, mouseX, mouseY, delta);
        drawEntity(context, x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, mouseX, mouseY, this.client.player);
    }

    @Override
    public void renderBackground(DrawContext context) {
        super.renderBackground(context);
        context.fill(x, y, x + (xSize - ySize) / 2 - 5, y + ySize, 0xFF555555);
        context.fill(x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, 0xFF222222);
        context.fill(x + (xSize + ySize) / 2 + 5, y, x + xSize, y + ySize, 0xFF555555);
    }

    protected void drawEntity(DrawContext context, int x1, int y1, int x2, int y2, int mouseX, int mouseY, LivingEntity entity) {
        float centerX = (float)(x1 + x2) / 2.0f;
        float centerY = (float)(y1 + y2) / 2.0f;
        context.enableScissor(x1, y1, x2, y2);
        Quaternionf quaternionf = new Quaternionf().rotateX((float) Math.PI/6 + xRot).rotateY((float) Math.PI/6 + yRot).rotateZ((float) Math.PI);
        float entityBodyYaw = entity.bodyYaw;
        float entityYaw = entity.getYaw();
        float entityPitch = entity.getPitch();
        float entityPrevHeadYaw = entity.prevHeadYaw;
        float entityHeadYaw = entity.headYaw;
        entity.bodyYaw = 180.0f;
        entity.setYaw(180.0f + yaw);
        entity.setPitch(pitch);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();
        Vector3f vector3f = new Vector3f((float) entityX, (float) entityY, -2.0f);
        drawEntity(context, centerX, centerY, mouseX, mouseY, vector3f, quaternionf, entity);
        entity.bodyYaw = entityBodyYaw;
        entity.setYaw(entityYaw);
        entity.setPitch(entityPitch);
        entity.prevHeadYaw = entityPrevHeadYaw;
        entity.headYaw = entityHeadYaw;
        context.disableScissor();
    }

    protected void drawEntity(DrawContext context, float x, float y, int mouseX, int mouseY, Vector3f offset, Quaternionf quaternionf, LivingEntity entity) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().multiplyPositionMatrix(new Matrix4f().scaling(entitySize, entitySize, -entitySize));
        context.getMatrices().translate(offset.x(), offset.y(), offset.z());
        context.getMatrices().multiply(quaternionf);
        DiffuseLighting.method_34742();
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadows(false);
        ModelAnalyser analyser = new ModelAnalyser();
        entityRenderDispatcher.render(entity, 0, -entity.getHeight() / 2.0f, 0, 0.0f, 1.0f, context.getMatrices(), analyser, 0xF000F0);
        analyser.buildLastRecord();
        analyser.drawByAnother(context.getVertexConsumers(), renderLayer -> true, (renderLayer, vertices) -> true); // TODO
        context.draw();
        analyser.setCurrent(renderLayer -> renderLayer.toString().contains(textureIdField.getText()), 0);
        int focusedIndex = analyser.getFocusedIndex(mouseX, mouseY, layers);
        focusedUV = analyser.getCenterUV(focusedIndex);
        focusedTextureId = analyser.focusedTextureId();
        analyser.drawQuad(context, posUField.getValue(), posVField.getValue(), 0x6F3333CC);
        if (focusedIndex != -1) {
            if (showCube) analyser.drawPolyhedron(context, focusedIndex, 0x5FFFFFFF);
            else analyser.drawQuad(context, focusedIndex, 0x7FFFFFFF, true);
        }
        analyser.drawNormal(context, forwardUField.getValue(), forwardVField.getValue(), entitySize / 2, 0xFF00CC00);
        analyser.drawNormal(context, upwardUField.getValue(), upwardVField.getValue(), entitySize / 2, 0xFFCC0000);
        entityRenderDispatcher.setRenderShadows(true);
        context.getMatrices().pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    private void reset() {
        entitySize = 80;
        yawSlider.setValue(0);
        pitchSlider.setValue(0);
        entityX = entityY = 0;
        xRot = yRot = 0;
        layers = 0;
    }

    private void saveConfig() {
        String name = nameField.getText();
        if (name == null) return;
        ConfigFile.modConfig.binding.targetMap.put(name, new ModConfig.Binding.Target(textureIdField.getText(),
                forwardUField.getValue(), forwardVField.getValue(), upwardUField.getValue(), upwardVField.getValue(),
                posUField.getValue(), posVField.getValue()));
        ConfigFile.save();
    }

    private void loadConfig() {
        String name = nameField.getText();
        ModConfig.Binding.Target target = ConfigFile.modConfig.binding.targetMap.get(name);
        if (target == null) return;
        try {
            textureIdField.setText(target.textureId());
            forwardUField.setValue(target.forwardU());
            forwardVField.setValue(target.forwardV());
            upwardUField.setValue(target.upwardU());
            upwardVField.setValue(target.upwardV());
            posUField.setValue(target.posU());
            posVField.setValue(target.posV());
        } catch (Exception ignored) {
        }
    }

    private ButtonWidget createButton(String name, int width, ButtonWidget.PressAction onPress) {
        return createButton(Text.translatable(KEY_WIDGET + name), width, onPress);
    }

    private ButtonWidget createButton(Text message, int width, ButtonWidget.PressAction onPress) {
        return ButtonWidget.builder(message, onPress).size(width, widgetHeight).build();
    }

    private FloatFieldWidget createFloatField(int width, @Nullable FloatFieldWidget copyFrom) {
        return new FloatFieldWidget(textRenderer, 0, 0, width, widgetHeight, copyFrom, null);
    }

    private TextFieldWidget createTextField(int width, @Nullable TextFieldWidget copyFrom) {
        return new TextFieldWidget(textRenderer, 0, 0, width, widgetHeight, copyFrom, null);
    }

    protected boolean mouseInViewArea(double mouseX, double mouseY) {
        return mouseX >= x + (double) (xSize - ySize) / 2 && mouseX <= x + (double) (xSize + ySize) / 2 && mouseY >= y && mouseY <= y + ySize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseInViewArea(mouseX, mouseY) && focusedUV != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT &&
                InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
            if (selecting.equals("forward")) {
                forwardUField.setValue(focusedUV.getLeft());
                forwardVField.setValue(focusedUV.getRight());
            } else if (selecting.equals("upward")) {
                upwardUField.setValue(focusedUV.getLeft());
                upwardVField.setValue(focusedUV.getRight());
            } else {
                posUField.setValue(focusedUV.getLeft());
                posVField.setValue(focusedUV.getRight());
            }
            textureIdField.setText(focusedTextureId);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
                xRot = MathHelper.wrapDegrees(xRot + (float) deltaY / 90f);
                yRot = MathHelper.wrapDegrees(yRot - (float) deltaX / 90f);
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                entityX = entityX + deltaX / entitySize;
                entityY = entityY + deltaY / entitySize;
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
                layers = Math.max(0, layers + (int) amount);
            } else {
                entitySize = MathHelper.clamp(entitySize + (int) amount * entitySize / 16, 16, 1024);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean shouldPause() {
        return shouldPause;
    }
}
