package com.Tropfen_33.Duall_Wheel_Mount.content;

import dev.simulated_team.simulated.multiloader.inventory.MultiSlotContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory for the Dual Wheel Mount.
 *
 * Slot 0 = left wheel
 * Slot 1 = right wheel
 *
 * A wheel is identified in exactly the same way as the original
 * Offroad Wheel Mount: by the Offroad TIRE data component.
 */
public class DualWheelMountInventory extends MultiSlotContainer {

    private final DualWheelMountBlockEntity blockEntity;

    public boolean suppressUpdate;

    public DualWheelMountInventory(DualWheelMountBlockEntity blockEntity) {
        super(2);
        this.blockEntity = blockEntity;
        this.suppressUpdate = false;
    }

    @Override
    public void setChanged() {
        if (!suppressUpdate) {
            blockEntity.onStackChanged();
            blockEntity.notifyUpdate();
        }
    }

    public ItemStack getLeftWheel() {
        return getItem(0);
    }

    public ItemStack getRightWheel() {
        return getItem(1);
    }

    public void setLeftWheel(ItemStack stack) {
        setItem(0, stack);
    }

    public void setRightWheel(ItemStack stack) {
        setItem(1, stack);
    }
}
