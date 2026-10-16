package com.Tropfen_33.Duall_Wheel_Mount.content;

import com.Tropfen_33.Duall_Wheel_Mount.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DualWheelMountBlock extends HorizontalKineticBlock
        implements IBE<DualWheelMountBlockEntity> {

    public DualWheelMountBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<DualWheelMountBlockEntity> getBlockEntityClass() {
        return DualWheelMountBlockEntity.class;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<? extends DualWheelMountBlockEntity>
    getBlockEntityType() {
        return ModBlockEntities.DUAL_WHEEL_MOUNT.get();
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level, BlockPos pos, BlockState state, Direction direction) {
        return direction == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {

        if (!(level.getBlockEntity(pos) instanceof DualWheelMountBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        ItemStack held = player.getItemInHand(hand);

        Direction facing = state.getValue(HORIZONTAL_FACING);
        Direction leftSide = facing.getCounterClockWise();
        Direction rightSide = facing.getClockWise();
        Direction clickedFace = hitResult.getDirection();

        int targetSlot = clickedFace == leftSide ? 0
                : clickedFace == rightSide ? 1
                : -1;

        // Empty hand: remove a wheel from the side that was clicked.
        if (held.isEmpty()) {
            if (targetSlot == -1) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.literal("Click the left or right side to remove a wheel."),
                            true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!level.isClientSide) {
                ItemStack removed = targetSlot == 0
                        ? be.getLeftWheel().copy()
                        : be.getRightWheel().copy();

                if (!removed.isEmpty()) {
                    if (targetSlot == 0) {
                        be.getInventory().setLeftWheel(ItemStack.EMPTY);
                    } else {
                        be.getInventory().setRightWheel(ItemStack.EMPTY);
                    }

                    if (!player.getInventory().add(removed)) {
                        player.drop(removed, false);
                    }

                    be.setChanged();
                } else {
                    String side = targetSlot == 0 ? "left" : "right";
                    player.displayClientMessage(
                            Component.literal("There is no " + side + " wheel installed."),
                            true);
                }
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Only tire items can be installed.
        if (held.get(OffroadDataComponents.TIRE) == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // A tire clicked on front/back/top/bottom gets a fitting message.
        if (targetSlot == -1) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("This side is not used for wheel installation."),
                        true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            boolean occupied = targetSlot == 0
                    ? !be.getLeftWheel().isEmpty()
                    : !be.getRightWheel().isEmpty();

            String side = targetSlot == 0 ? "left" : "right";

            if (occupied) {
                player.displayClientMessage(
                        Component.literal("The " + side + " wheel position is already occupied."),
                        true);
                return ItemInteractionResult.sidedSuccess(false);
            }

            ItemStack wheel = held.copyWithCount(1);

            if (targetSlot == 0) {
                be.getInventory().setLeftWheel(wheel);
            } else {
                be.getInventory().setRightWheel(wheel);
            }

            held.shrink(1);
            be.setChanged();

            player.displayClientMessage(
                    Component.literal("Inserted the " + side + " wheel."),
                    true);

            return ItemInteractionResult.CONSUME;
        }

        return ItemInteractionResult.sidedSuccess(true);
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving) {

        if (state.hasBlockEntity() && !state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof DualWheelMountBlockEntity be) {
                be.clearContent();
            }
            level.removeBlockEntity(pos);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
