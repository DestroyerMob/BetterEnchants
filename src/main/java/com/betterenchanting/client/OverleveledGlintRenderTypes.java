package com.betterenchanting.client;

import com.betterenchanting.BetterEnchanting;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;

public final class OverleveledGlintRenderTypes {
    private static final ResourceLocation ITEM_GLINT = BetterEnchanting.id("textures/misc/overleveled_glint_item.png");
    private static final ResourceLocation ARMOR_GLINT = BetterEnchanting.id("textures/misc/overleveled_glint_armor.png");

    private static final RenderType ARMOR_ENTITY_GLINT = RenderType.create(
            "betterenchanting_overleveled_armor_entity_glint",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ARMOR_GLINT, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(RenderStateShard.ENTITY_GLINT_TEXTURING)
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .createCompositeState(false)
    );
    private static final RenderType GLINT_TRANSLUCENT = RenderType.create(
            "betterenchanting_overleveled_glint_translucent",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ITEM_GLINT, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(RenderStateShard.GLINT_TEXTURING)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .createCompositeState(false)
    );
    private static final RenderType GLINT = RenderType.create(
            "betterenchanting_overleveled_glint",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_GLINT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ITEM_GLINT, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(RenderStateShard.GLINT_TEXTURING)
                    .createCompositeState(false)
    );
    private static final RenderType ENTITY_GLINT = RenderType.create(
            "betterenchanting_overleveled_entity_glint",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_GLINT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ITEM_GLINT, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .setTexturingState(RenderStateShard.ENTITY_GLINT_TEXTURING)
                    .createCompositeState(false)
    );
    private static final RenderType ENTITY_GLINT_DIRECT = RenderType.create(
            "betterenchanting_overleveled_entity_glint_direct",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ITEM_GLINT, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(RenderStateShard.ENTITY_GLINT_TEXTURING)
                    .createCompositeState(false)
    );

    private OverleveledGlintRenderTypes() {
    }

    public static RenderType armorEntityGlint() {
        return ARMOR_ENTITY_GLINT;
    }

    public static RenderType glintTranslucent() {
        return GLINT_TRANSLUCENT;
    }

    public static RenderType glint() {
        return GLINT;
    }

    public static RenderType entityGlint() {
        return ENTITY_GLINT;
    }

    public static RenderType entityGlintDirect() {
        return ENTITY_GLINT_DIRECT;
    }

    public static VertexConsumer getArmorFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean withGlint) {
        return withGlint
                ? VertexMultiConsumer.create(bufferSource.getBuffer(armorEntityGlint()), bufferSource.getBuffer(renderType))
                : bufferSource.getBuffer(renderType);
    }

    public static VertexConsumer getCompassFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, PoseStack.Pose pose) {
        return VertexMultiConsumer.create(
                new SheetedDecalTextureGenerator(bufferSource.getBuffer(glint()), pose, 0.0078125F),
                bufferSource.getBuffer(renderType)
        );
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean withGlint) {
        if (!withGlint) {
            return bufferSource.getBuffer(renderType);
        }

        RenderType glintRenderType = Minecraft.useShaderTransparency() && renderType == Sheets.translucentItemSheet()
                ? glintTranslucent()
                : isItem ? glint() : entityGlint();
        return VertexMultiConsumer.create(bufferSource.getBuffer(glintRenderType), bufferSource.getBuffer(renderType));
    }

    public static VertexConsumer getFoilBufferDirect(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean withGlint) {
        return withGlint
                ? VertexMultiConsumer.create(
                bufferSource.getBuffer(isItem ? glint() : entityGlintDirect()),
                bufferSource.getBuffer(renderType)
        )
                : bufferSource.getBuffer(renderType);
    }
}
