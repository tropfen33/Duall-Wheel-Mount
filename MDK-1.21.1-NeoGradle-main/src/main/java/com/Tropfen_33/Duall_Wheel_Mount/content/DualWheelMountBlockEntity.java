package com.Tropfen_33.Duall_Wheel_Mount.content;

import com.Tropfen_33.Duall_Wheel_Mount.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.simulated_team.simulated.multiloader.inventory.MultiSlotContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Dual Wheel Mount.
 *
 * Phase 2:
 * - stores two wheel ItemStacks
 * - uses the same Offroad TIRE component used by the original Wheel Mount
 * - keeps the Create kinetic base
 *
 * The Sable physics actor will be added after the two-wheel inventory
 * and attachment logic are verified.
 */
public class DualWheelMountBlockEntity extends KineticBlockEntity implements Clearable {

    private static final String INVENTORY_TAG = "Wheels";

    private final DualWheelMountInventory inventory;

    public DualWheelMountBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DUAL_WHEEL_MOUNT.get(), pos, state);
        this.inventory = new DualWheelMountInventory(this);
    }

    public ItemStack getLeftWheel() {
        return inventory.getLeftWheel();
    }

    public ItemStack getRightWheel() {
        return inventory.getRightWheel();
    }

    public boolean hasLeftWheel() {
        return !getLeftWheel().isEmpty()
                && getLeftWheel().get(OffroadDataComponents.TIRE) != null;
    }

    public boolean hasRightWheel() {
        return !getRightWheel().isEmpty()
                && getRightWheel().get(OffroadDataComponents.TIRE) != null;
    }

    public DualWheelMountInventory getInventory() {
        return inventory;
    }

    public MultiSlotContainer getMultiSlotInventory() {
        return inventory;
    }

    public void onStackChanged() {
        invalidateRenderBoundingBox();
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
        onStackChanged();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.put(INVENTORY_TAG, inventory.write(registries));
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        inventory.suppressUpdate = true;

        if (tag.contains(INVENTORY_TAG)) {
            inventory.read(registries, tag.getCompound(INVENTORY_TAG));
        }

        inventory.suppressUpdate = false;
        onStackChanged();

        super.read(tag, registries, clientPacket);
    }
}
