package com.betterenchanting.client;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.world.inventory.EnhancedEnchantingMenu;
import com.betterenchanting.world.level.block.EnchantingTableStorage;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.phys.Vec3;

public final class EnchantingTableItemDisplay {
    private EnchantingTableItemDisplay() {
    }

    public static void render(
            EnchantingTableBlockEntity table,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (!(table instanceof EnchantingTableStorage storage)) {
            return;
        }
        SimpleContainer inventory = storage.betterenchanting$getEnchantingInventory();
        float time = table.time + partialTick;
        ItemStack target = inventory.getItem(EnhancedEnchantingMenu.TARGET_SLOT);
        List<MachineDisplayState.Display> displays = new ArrayList<>();

        EnchantingTableStatParticles.emit(
                table,
                target,
                inventory.getItem(EnhancedEnchantingMenu.REAGENT_SLOT)
        );

        float targetY = (float) (1.30D + FloatingItemVisuals.bob(time, 0.0D, 0.04D));
        renderStack(
                table,
                target,
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                0.5F,
                targetY,
                0.5F,
                FloatingItemVisuals.slowRotation(time, 0.0D),
                0.72F,
                0
        );
        publishDisplay(table, target, EnhancedEnchantingMenu.TARGET_SLOT,
                new Vec3(0.5D, targetY, 0.5D), 0.34D, displays);

        ItemStack reagent = inventory.getItem(EnhancedEnchantingMenu.REAGENT_SLOT);
        float reagentY = (float) (1.89D + FloatingItemVisuals.bob(time, 1.2D, 0.035D));
        renderStack(
                table,
                reagent,
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                0.5F,
                reagentY,
                0.5F,
                FloatingItemVisuals.slowRotation(time, 120.0D),
                0.50F,
                1
        );
        publishDisplay(table, reagent, EnhancedEnchantingMenu.REAGENT_SLOT,
                new Vec3(0.5D, reagentY, 0.5D), 0.27D, displays);

        for (int index = 0; index < EnhancedEnchantingMenu.MODIFIER_SLOT_COUNT; index++) {
            float angle = time * 0.012F + index * ((float) Math.PI * 2.0F / EnhancedEnchantingMenu.MODIFIER_SLOT_COUNT);
            float radius = 0.62F;
            float x = 0.5F + Mth.cos(angle) * radius;
            float y = (float) (1.31D + FloatingItemVisuals.bob(time, index * 1.7D, 0.05D));
            float z = 0.5F + Mth.sin(angle) * radius;
            ItemStack modifier = inventory.getItem(EnhancedEnchantingMenu.FIRST_MODIFIER_SLOT + index);
            renderStack(
                    table,
                    modifier,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay,
                    x,
                    y,
                    z,
                    FloatingItemVisuals.slowRotation(time, index * 120.0D),
                    0.42F,
                    2 + index
            );
            publishDisplay(table, modifier, EnhancedEnchantingMenu.FIRST_MODIFIER_SLOT + index,
                    new Vec3(x, y, z), 0.24D, displays);
        }

        if (BetterEnchantingConfig.usesInteractiveEnchanting() && table.getLevel() instanceof ClientLevel level) {
            MachineDisplayState.publish(level, table.getBlockPos(), displays);
        }
    }

    private static void publishDisplay(EnchantingTableBlockEntity table, ItemStack stack, int slot,
                                       Vec3 localPosition, double pickRadius,
                                       List<MachineDisplayState.Display> displays) {
        if (stack.isEmpty()) {
            return;
        }
        displays.add(new MachineDisplayState.Display(
                table.getBlockPos(),
                slot,
                localPosition.add(Vec3.atLowerCornerOf(table.getBlockPos())),
                stack,
                pickRadius
        ));
    }

    private static void renderStack(
            EnchantingTableBlockEntity table,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            float x,
            float y,
            float z,
            float rotationDegrees,
            float scale,
            int seedOffset
    ) {
        FloatingItemVisuals.render(
                table,
                stack,
                poseStack,
                buffers,
                packedOverlay,
                new Vec3(x, y, z),
                scale,
                rotationDegrees,
                seedOffset
        );
    }
}
