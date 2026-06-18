package com.betterenchanting.world;

import com.betterenchanting.compat.SilentGearCompat;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ItemTagsCommand {
    private ItemTagsCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("itemtags")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reroll")
                        .executes(context -> reroll(context.getSource()))));
    }

    private static int reroll(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("Hold an item in your main hand before rerolling tags."));
            return 0;
        }

        List<ResourceLocation> materialTags = SilentGearCompat.materialItemTags(stack);
        List<ResourceLocation> targetTags = EnchantmentTargetTags.resolve(stack);
        source.sendSuccess(() -> Component.literal("Rerolled Better Enchanting tags for ")
                .append(stack.getHoverName())
                .append(Component.literal(".")), false);
        source.sendSuccess(() -> tagList("Virtual material tags", materialTags), false);
        source.sendSuccess(() -> tagList("Target tags", targetTags), false);
        return Math.max(Command.SINGLE_SUCCESS, materialTags.size() + targetTags.size());
    }

    private static Component tagList(String label, List<ResourceLocation> tags) {
        MutableComponent line = Component.literal(label + ": ").withStyle(ChatFormatting.GRAY);
        if (tags.isEmpty()) {
            return line.append(Component.literal("none").withStyle(ChatFormatting.DARK_GRAY));
        }

        for (int index = 0; index < tags.size(); index++) {
            if (index > 0) {
                line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            line.append(Component.literal("#" + tags.get(index)).withStyle(ChatFormatting.AQUA));
        }
        return line;
    }
}
