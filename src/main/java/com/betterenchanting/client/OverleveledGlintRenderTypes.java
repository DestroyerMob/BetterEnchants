package com.betterenchanting.client;

import com.betterenchanting.BetterEnchanting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class OverleveledGlintRenderTypes {
    private static final ResourceLocation ITEM_GLINT = BetterEnchanting.id("textures/misc/overleveled_glint_item.png");
    private static final ResourceLocation ARMOR_GLINT = BetterEnchanting.id("textures/misc/overleveled_glint_armor.png");
    private static final String MOBS_TOOL_FORGING = "mobstoolforging";
    private static final float LAYERED_TOOL_GLINT_BRIGHTNESS = 0.45F;
    private static final RenderStateShard.TransparencyStateShard LAYERED_TOOL_GLINT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
            "betterenchanting_layered_tool_glint_transparency",
            () -> {
                RenderStateShard.GLINT_TRANSPARENCY.setupRenderState();
                RenderSystem.setShaderColor(LAYERED_TOOL_GLINT_BRIGHTNESS, LAYERED_TOOL_GLINT_BRIGHTNESS, LAYERED_TOOL_GLINT_BRIGHTNESS, 1.0F);
            },
            () -> {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderStateShard.GLINT_TRANSPARENCY.clearRenderState();
            }
    );

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
    private static final RenderType LAYERED_TOOL_GLINT_TRANSLUCENT = RenderType.create(
            "betterenchanting_layered_tool_overleveled_glint_translucent",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ITEM_GLINT, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(LAYERED_TOOL_GLINT_TRANSPARENCY)
                    .setTexturingState(RenderStateShard.GLINT_TEXTURING)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .createCompositeState(false)
    );
    private static final RenderType LAYERED_TOOL_GLINT = RenderType.create(
            "betterenchanting_layered_tool_overleveled_glint",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_GLINT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ITEM_GLINT, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(LAYERED_TOOL_GLINT_TRANSPARENCY)
                    .setTexturingState(RenderStateShard.GLINT_TEXTURING)
                    .createCompositeState(false)
    );
    private static final RenderType LAYERED_TOOL_ENTITY_GLINT = RenderType.create(
            "betterenchanting_layered_tool_overleveled_entity_glint",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_GLINT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ITEM_GLINT, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(LAYERED_TOOL_GLINT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .setTexturingState(RenderStateShard.ENTITY_GLINT_TEXTURING)
                    .createCompositeState(false)
    );
    private static final RenderType LAYERED_TOOL_ENTITY_GLINT_DIRECT = RenderType.create(
            "betterenchanting_layered_tool_overleveled_entity_glint_direct",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ITEM_GLINT, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(LAYERED_TOOL_GLINT_TRANSPARENCY)
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

    public static RenderType layeredToolGlintTranslucent() {
        return LAYERED_TOOL_GLINT_TRANSLUCENT;
    }

    public static RenderType layeredToolGlint() {
        return LAYERED_TOOL_GLINT;
    }

    public static RenderType layeredToolEntityGlint() {
        return LAYERED_TOOL_ENTITY_GLINT;
    }

    public static RenderType layeredToolEntityGlintDirect() {
        return LAYERED_TOOL_ENTITY_GLINT_DIRECT;
    }

    public static VertexConsumer getArmorFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean withGlint) {
        return withGlint
                ? VertexMultiConsumer.create(bufferSource.getBuffer(armorEntityGlint()), bufferSource.getBuffer(renderType))
                : bufferSource.getBuffer(renderType);
    }

    public static VertexConsumer getCompassFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, PoseStack.Pose pose) {
        return getCompassFoilBuffer(bufferSource, renderType, pose, ItemStack.EMPTY);
    }

    public static VertexConsumer getCompassFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, PoseStack.Pose pose, ItemStack stack) {
        return VertexMultiConsumer.create(
                new SheetedDecalTextureGenerator(bufferSource.getBuffer(isLayeredToolStack(stack) ? layeredToolGlint() : glint()), pose, 0.0078125F),
                bufferSource.getBuffer(renderType)
        );
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean withGlint) {
        return getFoilBuffer(bufferSource, renderType, isItem, withGlint, ItemStack.EMPTY);
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean withGlint, ItemStack stack) {
        if (!withGlint) {
            return bufferSource.getBuffer(renderType);
        }

        RenderType glintRenderType = Minecraft.useShaderTransparency() && renderType == Sheets.translucentItemSheet()
                ? isLayeredToolStack(stack) ? layeredToolGlintTranslucent() : glintTranslucent()
                : isItem
                ? isLayeredToolStack(stack) ? layeredToolGlint() : glint()
                : isLayeredToolStack(stack) ? layeredToolEntityGlint() : entityGlint();
        return VertexMultiConsumer.create(bufferSource.getBuffer(glintRenderType), bufferSource.getBuffer(renderType));
    }

    public static VertexConsumer getFoilBufferDirect(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean withGlint) {
        return getFoilBufferDirect(bufferSource, renderType, isItem, withGlint, ItemStack.EMPTY);
    }

    public static VertexConsumer getFoilBufferDirect(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean withGlint, ItemStack stack) {
        return withGlint
                ? VertexMultiConsumer.create(
                bufferSource.getBuffer(isItem
                        ? isLayeredToolStack(stack) ? layeredToolGlint() : glint()
                        : isLayeredToolStack(stack) ? layeredToolEntityGlintDirect() : entityGlintDirect()),
                bufferSource.getBuffer(renderType)
        )
                : bufferSource.getBuffer(renderType);
    }

    private static boolean isLayeredToolStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (DataComponentType<?> component : stack.getComponents().keySet()) {
            ResourceLocation componentId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component);
            if (componentId != null
                    && MOBS_TOOL_FORGING.equals(componentId.getNamespace())
                    && ("tool_construction".equals(componentId.getPath()) || "tool_part".equals(componentId.getPath()))) {
                return true;
            }
        }
        return false;
    }
}
