package dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster;

import com.simibubi.create.foundation.utility.CreateLang;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.*;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

public class SolidFuelThrusterBlockEntity extends AbstractThrusterBlockEntity implements Clearable {
    private static final float SUPERHEATED_THRUST_MULTIPLIER = 2.0f;
    private static final int BURN_SYNC_INTERVAL_TICKS = 20;

    public final SolidFuelThrusterItemHandler inventory = new SolidFuelThrusterItemHandler(this);

    private int burnTime = 0;
    private int totalBurnTicks = 0;
    private boolean superHeated = false;
    private boolean hatchOpen = false;
    private boolean wasPoweredLastTick = false;

    public SolidFuelThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.SOLID_FUEL_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    public SolidFuelThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public PropulsionConfig.ThrusterPlumeType getPlumeRenderType() {
        return PropulsionConfig.getSolidFuelThrusterPlumeType();
    }

    @Override
    public AABB getRenderBoundingBox() {
        return MeshedThrusterFlameUtils.inflateRenderBoundingBox(this, super.getRenderBoundingBox());
    }


    @Override
    public void tick() {
        if (level != null && !level.isClientSide) {
            tickFuel();
            if (hatchOpen) {
                tryPullFuelFromBehind();
            }
        }
        super.tick();
    }

    private void tickFuel() {
        boolean powered = isPowered();

        if (burnTime > 0 && powered && !SolidFuelThrusterFuelHelper.isInfiniteBurnTime(burnTime)) {
            burnTime--;
        }

        if (burnTime > 0 && powered != wasPoweredLastTick) {
            syncBurnStateToClient();
        } else if (burnTime > 0 && powered && level != null
                && level.getGameTime() % BURN_SYNC_INTERVAL_TICKS == 0) {
            syncBurnStateToClient();
        }
        wasPoweredLastTick = powered;

        if (burnTime <= 0) {
            ItemStack fuel = getFuelStack();
            if (!fuel.isEmpty() && !SolidFuelThrusterFuelHelper.isInfiniteFuel(fuel)) {
                setFuelStack(ItemStack.EMPTY);
            }
            superHeated = false;
            totalBurnTicks = 0;
            tryStartBurning();
        }
    }

    private void syncBurnStateToClient() {
        setChanged();
        notifyUpdate();
    }

    private void tryStartBurning() {
        if (burnTime > 0) {
            return;
        }
        ItemStack fuel = getFuelStack();
        if (fuel.isEmpty() || !canAcceptFuel(fuel)) {
            return;
        }
        if (SolidFuelThrusterFuelHelper.isInfiniteFuel(fuel)) {
            burnTime = SolidFuelThrusterFuelHelper.INFINITE_THRESHOLD;
        } else {
            totalBurnTicks = SolidFuelThrusterFuelHelper.resolveTotalBurnTicks(fuel);
            burnTime = totalBurnTicks;
        }
        totalBurnTicks = burnTime;
        superHeated = SolidFuelThrusterFuelHelper.isSuperheatedFuel(fuel);
        markFuelChanged();
        dirtyThrust();
    }

    /**
     * Remaining burn ticks (only decreases while the thruster is powered on).
     */
    public int getDisplayBurnTimeRemaining() {
        return burnTime;
    }

    void onInventoryChanged(int slot) {
        markFuelChanged();
        if (burnTime <= 0) {
            tryStartBurning();
        }
    }

    private void tryPullFuelFromBehind() {
        if (level == null || !getFuelStack().isEmpty() || burnTime > 0) {
            return;
        }
        Direction inputSide = getFuelInputSide();
        BlockPos behind = worldPosition.relative(inputSide);
        IItemHandler source = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                behind,
                inputSide.getOpposite()
        );
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack candidate = source.getStackInSlot(i);
            if (!canAcceptFuel(candidate)) {
                continue;
            }
            ItemStack extracted = source.extractItem(i, 1, false);
            if (!extracted.isEmpty()) {
                ItemStack remainder = ItemHandlerHelper.insertItem(inventory, extracted, false);
                if (!remainder.isEmpty()) {
                    source.insertItem(i, remainder, false);
                }
                break;
            }
        }
    }

    public void markFuelChanged() {
        setChanged();
        notifyUpdate();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public boolean canAcceptFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return SolidThrusterFuelManager.getProperties(stack) != null
                || SolidFuelThrusterFuelHelper.isSuperheatedFuel(stack)
                || SolidFuelThrusterFuelHelper.isCreativeBlazeCake(stack);
    }

    public ItemStack getFuelStack() {
        return inventory.getStackInSlot(SolidFuelThrusterItemHandler.FUEL_SLOT);
    }

    public void setFuelStack(ItemStack stack) {
        inventory.setStackInSlot(
                SolidFuelThrusterItemHandler.FUEL_SLOT,
                stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1)
        );
    }

    public ItemStack getQueuedFuel() {
        return burnTime > 0 ? ItemStack.EMPTY : getFuelStack();
    }

    public void setQueuedFuel(ItemStack stack) {
        if (burnTime <= 0) {
            setFuelStack(stack);
        }
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getTotalBurnTicks() {
        return totalBurnTicks;
    }

    public boolean isSuperHeated() {
        return superHeated;
    }

    public boolean isHatchOpen() {
        return hatchOpen;
    }

    public void toggleHatch() {
        hatchOpen = !hatchOpen;
        setChanged();
        notifyUpdate();
    }

    public ItemStack getBurningFuel() {
        return burnTime > 0 ? getFuelStack() : ItemStack.EMPTY;
    }

    public IItemHandler getItemHandler(Direction side) {
        if (side != null && side != getFuelInputSide()) {
            return null;
        }
        return inventory;
    }

    public Direction getFuelInputSide() {
        return getFacing();
    }

    public boolean tryInsertOrExtractFuel(Player player, InteractionHand hand) {
        if (player == null || player.isSpectator()) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);

        if (held.isEmpty()) {
            if (burnTime > 0 || getFuelStack().isEmpty()) {
                return false;
            }
            ItemStack fuel = getFuelStack();
            if (!player.getInventory().add(fuel.copy())) {
                player.drop(fuel.copy(), false);
            }
            setFuelStack(ItemStack.EMPTY);
            return true;
        }

        if (burnTime > 0) {
            return false;
        }

        ItemStack remainder = inventory.insertItem(SolidFuelThrusterItemHandler.FUEL_SLOT, held, false);
        if (remainder.getCount() == held.getCount()) {
            return false;
        }
        player.setItemInHand(hand, remainder);
        return true;
    }

    @Override
    public float getPower() {
        if (controlMode == ControlMode.PERIPHERAL) {
            return digitalInput > 0.0f ? 1.0f : 0.0f;
        }
        return redstoneInput > 0 ? 1.0f : 0.0f;
    }

    private boolean hasActiveBurn() {
        return burnTime > 0 && !getFuelStack().isEmpty();
    }

    @Override
    protected boolean isWorking() {
        return isPowered() && hasActiveBurn();
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        float thrust = 0;
        float currentPower = getPower();

        if (isWorking() && currentPower > 0) {
            ItemStack fuel = getFuelStack();
            ItemThrusterProperties properties = SolidThrusterFuelManager.getProperties(fuel);
            float obstructionEffect = calculateObstructionEffect();
            float thrustPercentage = Math.min(currentPower, obstructionEffect);

            if (thrustPercentage > 0 && properties != null) {
                float fuelEfficiency = SolidThrusterFuelManager.getEfficiency(fuel.getItem());
                float thrustMultiplier = properties.thrustMultiplier();
                if (superHeated) {
                    thrustMultiplier *= SUPERHEATED_THRUST_MULTIPLIER;
                }
                float baseThrustPn = (float) (getBaseThrust() * getThrustUnitsPerKn());
                baseThrustPn *= (float) calculateAtmosphericFactor();
                thrust = baseThrustPn * thrustPercentage * thrustMultiplier * fuelEfficiency;
            }
        }

        setThrustAndSync(thrust);
        isThrustDirty = false;
    }

    @Override
    protected double getBaseThrust() {
        return PropulsionConfig.SOLID_FUEL_THRUSTER_BASE_THRUST.get();
    }

    @Override
    protected double getRawThrustCap() {
        return PropulsionConfig.SOLID_FUEL_THRUSTER_BASE_THRUST.get();
    }

    @Override
    public double getNozzleOffsetFromCenter() {
        return PropulsionConfig.SOLID_FUEL_THRUSTER_NOZZLE_OFFSET.get();
    }

    @Override
    protected double getParticleCountMultiplier() {
        return PropulsionConfig.SOLID_FUEL_THRUSTER_PARTICLE_COUNT_MULTIPLIER.get();
    }

    @Override
    protected double getParticleVelocityMultiplier() {
        return PropulsionConfig.SOLID_FUEL_THRUSTER_PARTICLE_VELOCITY_MULTIPLIER.get();
    }

    @Override
    public boolean shouldEmitPlume() {
        if (!super.shouldEmitPlume()) {
            return false;
        }
        ItemThrusterProperties properties = SolidThrusterFuelManager.getProperties(getFuelStack());
        return properties != null && properties.particleType() != ThrusterParticleType.NONE;
    }

    @Override
    protected ParticleOptions createParticleOptions() {
        ItemThrusterProperties properties = SolidThrusterFuelManager.getProperties(getFuelStack());
        if (properties == null) {
            return super.createParticleOptions();
        }
        return properties.particleType().createParticleOptions(properties);
    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (getFuelStack().isEmpty()) {
            return CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.thruster.status.no_fuel"))
                    .style(ChatFormatting.RED);
        }
        if (!validFuel()) {
            return CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.thruster.status.wrong_fuel"))
                    .style(ChatFormatting.RED);
        }
        if (!isPowered()) {
            return CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.thruster.status.not_powered"))
                    .style(ChatFormatting.GOLD);
        }
        if (getEmptyBlocks() == 0) {
            return CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.thruster.obstructed"))
                    .style(ChatFormatting.RED);
        }
        return CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.status.working"))
                .style(ChatFormatting.GREEN);
    }

    @Override
    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addThrusterDetails(tooltip, isPlayerSneaking);
        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.solid_fuel_thruster.fuel_label"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        if (hasActiveBurn()) {
            appendBurnTimeLine(tooltip, getDisplayBurnTimeRemaining(), totalBurnTicks, superHeated);
        } else if (!getFuelStack().isEmpty()) {
            int nextBurn = SolidFuelThrusterFuelHelper.resolveTotalBurnTicks(getFuelStack());
            appendBurnTimeLine(tooltip, nextBurn, nextBurn, SolidFuelThrusterFuelHelper.isSuperheatedFuel(getFuelStack()));
        }

        if (!getFuelStack().isEmpty()) {
            CreateLang.builder()
                    .add(Component.literal("  "))
                    .add(getFuelStack().getHoverName().copy().withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip);
        }
    }

    private static void appendBurnTimeLine(List<Component> tooltip, int remaining, int total, boolean superheated) {
        boolean infinite = SolidFuelThrusterFuelHelper.isInfiniteBurnTime(remaining)
                || SolidFuelThrusterFuelHelper.isInfiniteBurnTime(total);
        LangBuilder time = CreateLang.builder().add(Component.literal("  "));
        if (infinite) {
            time.add(Component.translatable("createpropulsion.gui.goggles.solid_fuel_thruster.infinite")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            String formatted = SolidFuelThrusterFuelHelper.formatBurnTime(remaining);
            if (formatted != null) {
                time.add(Component.literal(formatted).withStyle(superheated ? ChatFormatting.GOLD : ChatFormatting.AQUA));
            }
        }
        time.add(Component.translatable("createpropulsion.gui.goggles.solid_fuel_thruster.burn_time")
                .withStyle(ChatFormatting.GRAY));
        time.forGoggles(tooltip);

        if (superheated) {
            CreateLang.builder()
                    .add(Component.literal("  "))
                    .add(Component.translatable("createpropulsion.gui.goggles.solid_fuel_thruster.superheated")
                            .withStyle(ChatFormatting.GOLD))
                    .forGoggles(tooltip);
        }
    }

    public boolean validFuel() {
        return !getFuelStack().isEmpty() && canAcceptFuel(getFuelStack());
    }

    @Override
    public FluidStack fluidStack() {
        return FluidStack.EMPTY;
    }

    @Override
    public boolean validFluid() {
        return false;
    }

    @Override
    public IFluidHandler getFluidHandler(Direction side) {
        return null;
    }

    @Override
    public void clearContent() {
        setFuelStack(ItemStack.EMPTY);
        burnTime = 0;
        totalBurnTicks = 0;
        superHeated = false;
    }

    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("BurnTime", burnTime);
        compound.putInt("TotalBurnTicks", totalBurnTicks);
        compound.putBoolean("SuperHeated", superHeated);
        compound.putBoolean("HatchOpen", hatchOpen);
        compound.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        burnTime = compound.getInt("BurnTime");
        totalBurnTicks = compound.contains("TotalBurnTicks")
                ? compound.getInt("TotalBurnTicks")
                : burnTime;
        superHeated = compound.getBoolean("SuperHeated");
        hatchOpen = compound.getBoolean("HatchOpen");
        if (compound.contains("Inventory")) {
            CompoundTag inventoryTag = compound.getCompound("Inventory");
            if (inventoryTag.contains("Items")) {
                ListTag items = inventoryTag.getList("Items", Tag.TAG_COMPOUND);
                ItemStack merged = ItemStack.EMPTY;
                if (items.size() > 1) {
                    ItemStack burning = ItemStack.parse(registries, items.getCompound(1)).orElse(ItemStack.EMPTY);
                    ItemStack queued = ItemStack.parse(registries, items.getCompound(0)).orElse(ItemStack.EMPTY);
                    merged = !burning.isEmpty() ? burning.copyWithCount(1) : queued.copyWithCount(1);
                } else if (!items.isEmpty()) {
                    merged = ItemStack.parse(registries, items.getCompound(0)).orElse(ItemStack.EMPTY);
                }
                setFuelStack(merged);
            } else {
                inventory.deserializeNBT(registries, inventoryTag);
            }
        } else {
            ItemStack legacy = ItemStack.EMPTY;
            if (compound.contains("BurningFuel")) {
                legacy = ItemStack.parse(registries, compound.getCompound("BurningFuel")).orElse(ItemStack.EMPTY);
            }
            if (legacy.isEmpty() && compound.contains("QueuedFuel")) {
                legacy = ItemStack.parse(registries, compound.getCompound("QueuedFuel")).orElse(ItemStack.EMPTY);
            }
            setFuelStack(legacy);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide && burnTime <= 0) {
            tryStartBurning();
        }
    }
}
