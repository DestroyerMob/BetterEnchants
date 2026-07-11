package com.betterenchanting.world.level.block.entity;

import com.betterenchanting.compat.ModularMaterialCompat;
import com.betterenchanting.compat.MobsToolForgingCompat;
import com.betterenchanting.data.EnchantmentLevelRules;
import com.betterenchanting.registry.ModBlockEntities;
import com.betterenchanting.world.inventory.AttunementPedestalMenu;
import com.betterenchanting.world.inventory.PedestalUpgradeRules;
import com.betterenchanting.world.inventory.PedestalUpgradeRules.UpgradePlan;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AttunementPedestalBlockEntity extends BaseContainerBlockEntity {
    public static final int TARGET_SLOT = 0;
    public static final int ESSENCE_SLOT = 1;
    public static final int CATALYST_SLOT = 2;
    public static final int CONTAINER_SIZE = 3;
    public static final int DATA_COUNT = 8;

    public static final int FLAG_VALID_SELECTION = 1;
    public static final int FLAG_MATCHING_ESSENCE = 1 << 1;
    public static final int FLAG_ENOUGH_ESSENCE = 1 << 2;
    public static final int FLAG_LINKED_TABLE = 1 << 3;
    public static final int FLAG_ENOUGH_POWER = 1 << 4;
    public static final int FLAG_CATALYST_REQUIRED = 1 << 5;
    public static final int FLAG_HAS_CATALYST = 1 << 6;
    public static final int FLAG_OVERLEVEL = 1 << 7;
    public static final int FLAG_MAXIMUM_REACHED = 1 << 8;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    @Nullable
    private ResourceLocation selectedEnchantment;
    private int selectedPartIndex = -1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            UpgradePlan plan = AttunementPedestalBlockEntity.this.currentPlan();
            return switch (index) {
                case 0 -> plan.currentLevel();
                case 1 -> plan.nextLevel();
                case 2 -> plan.essenceCost();
                case 3 -> plan.requiredPower();
                case 4 -> plan.availablePower();
                case 5 -> planFlags(plan);
                case 6 -> AttunementPedestalBlockEntity.this.selectedEnchantmentRegistryId();
                case 7 -> AttunementPedestalBlockEntity.this.selectedPartIndex;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AttunementPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATTUNEMENT_PEDESTAL.get(), pos, state);
    }

    public ItemStack target() {
        return this.items.get(TARGET_SLOT);
    }

    @Nullable
    public ResourceLocation selectedEnchantment() {
        return this.selectedEnchantment;
    }

    public int selectedPartIndex() {
        return this.selectedPartIndex;
    }

    public UpgradePlan previewPlan(int partIndex, ResourceLocation enchantmentId) {
        if (this.level == null) {
            return PedestalUpgradeRules.invalidPlan(enchantmentId, partIndex);
        }
        return PedestalUpgradeRules.plan(
                this.level,
                this.worldPosition,
                this.target(),
                partIndex,
                enchantmentId,
                this.items.get(ESSENCE_SLOT),
                this.items.get(CATALYST_SLOT)
        );
    }

    public UpgradePlan upgradePlan() {
        return this.currentPlan();
    }

    public boolean selectEnchantment(int partIndex, ResourceLocation enchantmentId) {
        if (this.level == null || !PedestalUpgradeRules.canSelect(this.level, this.target(), partIndex, enchantmentId)) {
            return false;
        }
        this.selectedEnchantment = enchantmentId;
        this.selectedPartIndex = partIndex;
        this.setChanged();
        return true;
    }

    public boolean tryUpgrade(Player player) {
        if (this.level == null || this.level.isClientSide || this.selectedEnchantment == null) {
            return false;
        }
        UpgradePlan plan = this.currentPlan();
        if (!plan.canUpgrade() || plan.enchantment() == null) {
            return false;
        }

        ItemStack target = this.target();
        ItemStack upgraded = upgradeTarget(this.level.registryAccess(), target, plan);
        if (upgraded.isEmpty()) {
            return false;
        }

        int levelsSpent = 0;
        player.onEnchantmentPerformed(upgraded, 0);
        if (!player.hasInfiniteMaterials()) {
            this.items.get(ESSENCE_SLOT).shrink(plan.essenceCost());
            if (plan.catalystRequired()) {
                this.items.get(CATALYST_SLOT).shrink(1);
            }
        }
        this.items.set(TARGET_SLOT, upgraded);
        this.setChanged();

        player.awardStat(Stats.ENCHANT_ITEM);
        List<EnchantmentInstance> applied = List.of(new EnchantmentInstance(plan.enchantment(), plan.nextLevel()));
        net.neoforged.neoforge.common.CommonHooks.onPlayerEnchantItem(player, upgraded, applied);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, upgraded, levelsSpent);
            emitUpgradeEffects(serverPlayer.serverLevel(), plan.overlevel());
        }
        return true;
    }

    private ItemStack upgradeTarget(RegistryAccess registries, ItemStack target, UpgradePlan plan) {
        if (plan.partIndex() >= 0) {
            return MobsToolForgingCompat.upgradeRoutedEnchantment(
                    registries,
                    target,
                    plan.partIndex(),
                    plan.enchantmentId(),
                    plan.nextLevel()
            ).orElse(ItemStack.EMPTY);
        }

        ItemStack result = target.copy();
        if (plan.overlevel()) {
            EnchantmentLevelRules.overlevel(result, plan.enchantment());
        } else {
            EnchantmentHelper.updateEnchantments(result, mutable -> mutable.set(plan.enchantment(), plan.nextLevel()));
            EnchantmentLevelRules.clampEnchantments(result);
        }
        ModularMaterialCompat.reconcileRoutedEnchantments(registries, result);
        return result;
    }

    private void emitUpgradeEffects(ServerLevel level, boolean overlevel) {
        double x = this.worldPosition.getX() + 0.5D;
        double y = this.worldPosition.getY() + 1.25D;
        double z = this.worldPosition.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.ENCHANT, x, y, z, overlevel ? 54 : 34, 0.38D, 0.32D, 0.38D, 0.09D);
        if (overlevel) {
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 18, 0.30D, 0.28D, 0.30D, 0.04D);
        }
        level.playSound(null, this.worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.1F, overlevel ? 1.35F : 1.05F);
        level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.25F, overlevel ? 1.55F : 1.25F);
    }

    private UpgradePlan currentPlan() {
        if (this.level == null || this.selectedEnchantment == null) {
            return PedestalUpgradeRules.invalidPlan(
                    this.selectedEnchantment == null ? ResourceLocation.withDefaultNamespace("unbreaking") : this.selectedEnchantment,
                    this.selectedPartIndex
            );
        }
        return PedestalUpgradeRules.plan(
                this.level,
                this.worldPosition,
                this.target(),
                this.selectedPartIndex,
                this.selectedEnchantment,
                this.items.get(ESSENCE_SLOT),
                this.items.get(CATALYST_SLOT)
        );
    }

    private int selectedEnchantmentRegistryId() {
        if (this.level == null || this.selectedEnchantment == null) {
            return -1;
        }
        return this.level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ResourceKey.create(Registries.ENCHANTMENT, this.selectedEnchantment))
                .map(holder -> this.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).asHolderIdMap().getId(holder))
                .orElse(-1);
    }

    private static int planFlags(UpgradePlan plan) {
        int flags = 0;
        flags |= plan.validSelection() ? FLAG_VALID_SELECTION : 0;
        flags |= plan.matchingEssence() ? FLAG_MATCHING_ESSENCE : 0;
        flags |= plan.enoughEssence() ? FLAG_ENOUGH_ESSENCE : 0;
        flags |= plan.linkedTable() ? FLAG_LINKED_TABLE : 0;
        flags |= plan.enoughPower() ? FLAG_ENOUGH_POWER : 0;
        flags |= plan.catalystRequired() ? FLAG_CATALYST_REQUIRED : 0;
        flags |= plan.hasCatalyst() ? FLAG_HAS_CATALYST : 0;
        flags |= plan.overlevel() ? FLAG_OVERLEVEL : 0;
        flags |= plan.maximumReached() ? FLAG_MAXIMUM_REACHED : 0;
        return flags;
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.betterenchanting.attunement_pedestal");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == TARGET_SLOT && !ItemStack.matches(this.items.get(TARGET_SLOT), stack)) {
            this.selectedEnchantment = null;
            this.selectedPartIndex = -1;
        }
        super.setItem(slot, stack);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AttunementPedestalMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.selectedEnchantment = tag.contains("SelectedEnchantment")
                ? ResourceLocation.tryParse(tag.getString("SelectedEnchantment"))
                : null;
        this.selectedPartIndex = tag.contains("SelectedPartIndex") ? tag.getInt("SelectedPartIndex") : -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        if (this.selectedEnchantment != null) {
            tag.putString("SelectedEnchantment", this.selectedEnchantment.toString());
        }
        tag.putInt("SelectedPartIndex", this.selectedPartIndex);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
