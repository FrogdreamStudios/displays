package com.inotsleep.dreamdisplays.client_1_21_8.render;

import com.inotsleep.dreamdisplays.client.DisplayManager;
import com.inotsleep.dreamdisplays.client_1_21_8.DreamDisplaysClientCommon;
import com.inotsleep.dreamdisplays.client_1_21_8.util.ClientUtils;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.UUID;

public class TextureObject implements Closeable {
    private final int width, height;
    private final ResourceLocation id;
    private final DynamicTexture texture;
    private final RenderType renderType;
    private final CommandEncoder encoder;

    private BufferedImage stagingImage;
    private Graphics2D stagingG;
    private IntBuffer directBuffer;

    private boolean written = false;

    public TextureObject(int width, int height, UUID displayID) {
        this.width = width;
        this.height = height;

        this.id = ResourceLocation.fromNamespaceAndPath(DreamDisplaysClientCommon.MOD_ID, displayID + "-" + width + "x" + height);
        this.texture = new DynamicTexture(id.getPath(), width, height, true);
        this.encoder = RenderSystem.getDevice().createCommandEncoder();
        this.renderType = RenderType.create(
                id.getPath(),
                4194304,
                true,
                false,
                RenderPipelines.SOLID,
                RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(id, false)).createCompositeState(RenderType.OutlineProperty.NONE)
            );

        Minecraft.getInstance().getTextureManager().register(id, texture);

        allocateStaging();
    }

    private void allocateStaging() {
        this.stagingImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.stagingG = stagingImage.createGraphics();
        this.stagingG.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        this.stagingG.setComposite(AlphaComposite.SrcOver);

        int pixelCount = width * height;
        ByteBuffer bb = ByteBuffer.allocateDirect(pixelCount * Integer.BYTES).order(ByteOrder.nativeOrder());
        this.directBuffer = bb.asIntBuffer();
    }

    public static TextureObject validateObject(TextureObject old, int width, int height, UUID displayID) {
        if (old != null && old.width == width && old.height == height) return old;

        if (old != null) DisplayManager.removeData(TextureObject.class, displayID);
        TextureObject newObj = new TextureObject(width, height, displayID);
        DisplayManager.setData(TextureObject.class, displayID, newObj);
        return newObj;
    }

    public void render(PoseStack stack) {
        if (!false) {
            ClientUtils.renderColor(stack, 0, 0, 0, RenderType.solid());
        } else {
            ClientUtils.renderGpuTexture(stack, texture.getTextureView(), renderType);
        }
    }

    public void writeToTexture(BufferedImage image) {
        if (image == null) return;

        double scale = Math.max((double) width  / image.getWidth(),
                (double) height / image.getHeight());
        int sw = (int) Math.round(image.getWidth()  * scale);
        int sh = (int) Math.round(image.getHeight() * scale);
        int x = (width  - sw) / 2;
        int y = (height - sh) / 2;

        stagingG.drawImage(image, x, y, sw, sh, null);

        int[] pixels = ((DataBufferInt) stagingImage.getRaster().getDataBuffer()).getData();
        directBuffer.clear();

        for (int argb : pixels) {
            int rgba = (argb << 8) | ((argb >>> 24) & 0xFF);
            directBuffer.put(rgba);
        }
        directBuffer.flip();

        encoder.writeToTexture(
                texture.getTexture(),
                directBuffer,
                NativeImage.Format.RGBA,
                /*mipLevel*/ 0,
                /*depth*/    0,
                /*xOff */    0,
                /*yOff */    0,
                width,
                height);

        written = true;
    }

    @Override
    public void close() {
        try {
            texture.close();
        } catch (Exception ignored) {}
        stagingG.dispose();
    }
}

