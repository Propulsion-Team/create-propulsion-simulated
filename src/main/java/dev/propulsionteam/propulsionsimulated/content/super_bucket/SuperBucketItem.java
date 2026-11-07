package dev.propulsionteam.propulsionsimulated.content.super_bucket;

import dev.propulsionteam.propulsionsimulated.assemblerstick.item.CreateStyleTooltipItem;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class SuperBucketItem extends CreateStyleTooltipItem {
    public SuperBucketItem(Properties properties) {
        super(properties);
    }

    private static FluidStack getSelectedFluid(ItemStack stack) {
        return stack.getOrDefault(PropulsionDataComponents.SUPER_BUCKET_FLUID.get(), FluidStack.EMPTY);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !getSelectedFluid(stack).isEmpty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        FluidStack selected = getSelectedFluid(stack);
        if (!selected.isEmpty()) {
            tooltip.add(Component.translatable("item.createpropulsion.super_bucket.selected_fluid", selected.getHoverName())
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        // Run before block interaction so tanks are filled/copied instead of opening their GUI.
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), context.isInside());
        // The normal block ray ignores fluids. Prefer any fluid between the player and that block.
        BlockHitResult fluidHit = getPlayerPOVHitResult(context.getLevel(), player, ClipContext.Fluid.ANY);
        if (fluidHit.getType() == HitResult.Type.BLOCK
                && !context.getLevel().getFluidState(fluidHit.getBlockPos()).isEmpty()) {
            hit = fluidHit;
        }
        return interact(context.getLevel(), player, context.getHand(), stack, hit);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResult result = interact(level, player, hand, stack,
                getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY));
        return new InteractionResultHolder<>(result, stack);
    }

    private static InteractionResult clear(Level level, ItemStack stack) {
        if (!level.isClientSide) {
            stack.remove(PropulsionDataComponents.SUPER_BUCKET_FLUID.get());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult interact(Level level, Player player, InteractionHand hand,
                                               ItemStack stack, BlockHitResult hit) {
        FluidStack selected = getSelectedFluid(stack);
        if (player.isShiftKeyDown() && !selected.isEmpty()) {
            // Clearing a selection must never also empty the targeted container.
            return clear(level, stack);
        }
        BlockPos clicked = hit.getBlockPos();
        if (!level.mayInteract(player, clicked) || !player.mayUseItemAt(clicked, hit.getDirection(), stack)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = level.getBlockState(clicked);
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, clicked, hit.getDirection());
        if (handler == null) {
            handler = level.getCapability(Capabilities.FluidHandler.BLOCK, clicked, null);
        }
        if (player.isShiftKeyDown()) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                if (handler != null) {
                    SuperBucketFluidHandling.clearContainer(handler);
                } else if (state.is(Blocks.WATER_CAULDRON) || state.is(Blocks.LAVA_CAULDRON)) {
                    level.setBlockAndUpdate(clicked, Blocks.CAULDRON.defaultBlockState());
                    level.gameEvent(player, GameEvent.FLUID_PICKUP, clicked);
                }
            }
            return InteractionResult.CONSUME;
        }
        if (selected.isEmpty()) {
            FluidStack copied = handler == null ? FluidStack.EMPTY : SuperBucketFluidHandling.copyFluid(handler);
            if (copied.isEmpty()) {
                FluidState fluidState = level.getFluidState(clicked);
                if (!fluidState.isEmpty()) {
                    Fluid fluid = fluidState.getType();
                    if (fluid instanceof FlowingFluid flowing) {
                        fluid = flowing.getSource();
                    }
                    copied = new FluidStack(fluid, FluidType.BUCKET_VOLUME);
                } else if (state.is(Blocks.WATER_CAULDRON)) {
                    copied = new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME);
                } else if (state.is(Blocks.LAVA_CAULDRON)) {
                    copied = new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME);
                }
            }
            if (!copied.isEmpty()) {
                stack.set(PropulsionDataComponents.SUPER_BUCKET_FLUID.get(), copied);
            }
            return InteractionResult.CONSUME;
        }

        if (handler != null) {
            SuperBucketFluidHandling.fillContainer(handler, selected);
            // Never spill beside a full, locked, or incompatible container.
            return InteractionResult.CONSUME;
        }

        if (state.getBlock() instanceof AbstractCauldronBlock) {
            BlockState filled = null;
            if (selected.is(Fluids.WATER) && (state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON))) {
                filled = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
            } else if (selected.is(Fluids.LAVA) && (state.is(Blocks.CAULDRON) || state.is(Blocks.LAVA_CAULDRON))) {
                filled = Blocks.LAVA_CAULDRON.defaultBlockState();
            }
            if (filled != null && !filled.equals(state)) {
                level.setBlockAndUpdate(clicked, filled);
                level.gameEvent(player, GameEvent.FLUID_PLACE, clicked);
            }
            return InteractionResult.CONSUME;
        }

        boolean canContain = state.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, level, clicked, state, selected.getFluid());
        BlockPos target = hit.getType() == HitResult.Type.MISS || canContain || state.canBeReplaced(selected.getFluid())
                ? clicked : clicked.relative(hit.getDirection());
        if (!level.mayInteract(player, target) || !player.mayUseItemAt(target, hit.getDirection(), stack)) {
            return InteractionResult.FAIL;
        }
        FluidStack placed = selected.copyWithAmount(FluidType.BUCKET_VOLUME);
        FluidTank source = new FluidTank(FluidType.BUCKET_VOLUME);
        source.setFluid(placed);
        FluidUtil.tryPlaceFluid(player, level, hand, target, source, placed);
        return InteractionResult.CONSUME;
    }
}
