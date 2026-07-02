package dev.propulsionteam.propulsionsimulated.content.platinum;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.FluidHelper.FluidExchange;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Locale;

public class PlatinumFluidVesselBlock extends Block implements IWrenchable, IBE<PlatinumFluidVesselBlockEntity> {

    public static final BooleanProperty POSITIVE = BooleanProperty.create("positive");
    public static final BooleanProperty NEGATIVE = BooleanProperty.create("negative");
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

    public PlatinumFluidVesselBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POSITIVE, true)
            .setValue(NEGATIVE, true)
            .setValue(AXIS, Direction.Axis.X)
            .setValue(SHAPE, Shape.WINDOW));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        AdvancementBehaviour.setPlacedBy(level, pos, placer);
    }

    public static boolean isVessel(BlockState state) {
        return state.getBlock() instanceof PlatinumFluidVesselBlock;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock() || moved) {
            return;
        }
        withBlockEntityDo(world, pos, PlatinumFluidVesselBlockEntity::updateConnectivity);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(POSITIVE, NEGATIVE, AXIS, SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            BlockState placedOn = context.getLevel()
                .getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
            Direction.Axis preferredAxis = placedOn.getOptionalValue(AXIS).orElse(null);
            if (preferredAxis != null) {
                return this.defaultBlockState().setValue(AXIS, preferredAxis);
            }
        }
        return this.defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        PlatinumFluidVesselBlockEntity vesselAt = ConnectivityHandler.partAt(getBlockEntityType(), world, pos);
        if (vesselAt == null) {
            return 0;
        }
        PlatinumFluidVesselBlockEntity controllerBE = vesselAt.getControllerBE();
        if (controllerBE == null || !controllerBE.hasWindow()) {
            return 0;
        }
        return vesselAt.getLuminosity();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        withBlockEntityDo(context.getLevel(), context.getClickedPos(), PlatinumFluidVesselBlockEntity::toggleWindows);
        return InteractionResult.SUCCESS;
    }

    static final VoxelShape CAMPFIRE_SMOKE_CLIP = Block.box(0, 4, 0, 16, 16, 16);

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context == CollisionContext.empty()) {
            return CAMPFIRE_SMOKE_CLIP;
        }
        return state.getShape(level, pos);
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && neighborState.getBlock() != this) {
            withBlockEntityDo(level, currentPos, PlatinumFluidVesselBlockEntity::updateBoilerTemperature);
        }
        return state;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        boolean onClient = level.isClientSide;

        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        FluidExchange exchange = null;
        PlatinumFluidVesselBlockEntity be = ConnectivityHandler.partAt(getBlockEntityType(), level, pos);
        if (be == null) {
            return ItemInteractionResult.FAIL;
        }

        IFluidHandler vesselCapability = level.getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), null);
        if (vesselCapability == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        FluidStack prevFluidInVessel = vesselCapability.getFluidInTank(0).copy();

        if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, be)) {
            exchange = FluidExchange.ITEM_TO_TANK;
        } else if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, be)) {
            exchange = FluidExchange.TANK_TO_ITEM;
        }

        if (exchange == null) {
            if (GenericItemEmptying.canItemBeEmptied(level, stack) || GenericItemFilling.canItemBeFilled(level, stack)) {
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        SoundEvent soundEvent = null;
        BlockState fluidState = null;
        FluidStack fluidInVessel = vesselCapability.getFluidInTank(0);

        if (exchange == FluidExchange.ITEM_TO_TANK) {
            Fluid fluid = fluidInVessel.getFluid();
            fluidState = fluid.defaultFluidState().createLegacyBlock();
            soundEvent = FluidHelper.getEmptySound(fluidInVessel);
        }

        if (exchange == FluidExchange.TANK_TO_ITEM) {
            Fluid fluid = prevFluidInVessel.getFluid();
            fluidState = fluid.defaultFluidState().createLegacyBlock();
            soundEvent = FluidHelper.getFillSound(prevFluidInVessel);
        }

        if (soundEvent != null && !onClient) {
            float pitch = Mth.clamp(1 - (1f * fluidInVessel.getAmount() / (PlatinumFluidVesselBlockEntity.getCapacityMultiplier() * 16 * 2f)), 0, 1);
            pitch /= 1.5f;
            pitch += .5f;
            pitch += (level.random.nextFloat() - .5f) / 4f;
            level.playSound(null, pos, soundEvent, SoundSource.BLOCKS, .5f, pitch);
        }

        if (!FluidStack.isSameFluidSameComponents(fluidInVessel, prevFluidInVessel)) {
            PlatinumFluidVesselBlockEntity controllerBE = be.getControllerBE();
            if (controllerBE != null) {
                if (fluidState != null && onClient) {
                    BlockParticleOption blockParticleData = new BlockParticleOption(ParticleTypes.BLOCK, fluidState);
                    float fluidLevel = (float) fluidInVessel.getAmount() / vesselCapability.getTankCapacity(0);

                    boolean reversed = fluidInVessel.getFluid().getFluidType().isLighterThanAir();
                    if (reversed) {
                        fluidLevel = 1 - fluidLevel;
                    }

                    Vec3 vec = hitResult.getLocation();
                    vec = new Vec3(vec.x, controllerBE.getBlockPos().getY() + fluidLevel * (controllerBE.getHeight() - .5f) + .25f, vec.z);
                    Vec3 motion = player.position().subtract(vec).scale(1 / 20f);
                    vec = vec.add(motion);
                    level.addParticle(blockParticleData, vec.x, vec.y, vec.z, motion.x, motion.y, motion.z);
                    return ItemInteractionResult.SUCCESS;
                }

                controllerBE.sendDataImmediately();
                controllerBE.setChanged();
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof PlatinumFluidVesselBlockEntity vesselBE)) {
                return;
            }
            world.removeBlockEntity(pos);
            ConnectivityHandler.splitMulti(vesselBE);
        }
    }

    @Override
    public Class<PlatinumFluidVesselBlockEntity> getBlockEntityClass() {
        return PlatinumFluidVesselBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PlatinumFluidVesselBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.PLATINUM_FLUID_VESSEL_BLOCK_ENTITY.get();
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        Direction.Axis mirrorAxis = mirror == Mirror.FRONT_BACK ? Direction.Axis.X : Direction.Axis.Z;
        Direction.Axis axis = state.getValue(AXIS);
        if (axis == mirrorAxis) {
            return state.setValue(POSITIVE, state.getValue(NEGATIVE))
                .setValue(NEGATIVE, state.getValue(POSITIVE));
        }
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        for (int i = 0; i < rotation.ordinal(); i++) {
            state = rotateOnce(state);
        }
        return state;
    }

    private BlockState rotateOnce(BlockState state) {
        Direction.Axis axis = state.getValue(AXIS);
        if (axis == Direction.Axis.X) {
            return state.setValue(AXIS, Direction.Axis.Z);
        }
        if (axis == Direction.Axis.Z) {
            return state.setValue(AXIS, Direction.Axis.X)
                .setValue(POSITIVE, state.getValue(NEGATIVE))
                .setValue(NEGATIVE, state.getValue(POSITIVE));
        }
        return state;
    }

    public enum Shape implements StringRepresentable {
        PLAIN,
        WINDOW,
        WINDOW_TOP,
        WINDOW_MIDDLE,
        WINDOW_BOTTOM,
        WINDOW_SINGLE,
        WINDOW_TOP_SINGLE,
        WINDOW_MIDDLE_SINGLE,
        WINDOW_BOTTOM_SINGLE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Shape nonSingleVariant() {
            return switch (this) {
                case WINDOW_SINGLE -> WINDOW;
                case WINDOW_TOP_SINGLE -> WINDOW_TOP;
                case WINDOW_MIDDLE_SINGLE -> WINDOW_MIDDLE;
                case WINDOW_BOTTOM_SINGLE -> WINDOW_BOTTOM;
                default -> this;
            };
        }
    }

    public enum WindowType implements StringRepresentable {
        SIDE_WIDE,
        SIDE_NARROW_ENDS,
        SIDE_NARROW_THIRDS,
        SIDE_HORIZONTAL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final SoundType SILENCED_METAL =
        new DeferredSoundType(0.1F, 1.5F, () -> SoundEvents.METAL_BREAK, () -> SoundEvents.METAL_STEP,
            () -> SoundEvents.METAL_PLACE, () -> SoundEvents.METAL_HIT, () -> SoundEvents.METAL_FALL);

    @Override
    public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, Entity entity) {
        SoundType soundType = super.getSoundType(state, world, pos, entity);
        if (entity != null && entity.getPersistentData().contains("SilenceVesselSound")) {
            return SILENCED_METAL;
        }
        return soundType;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
        return getBlockEntityOptional(world, pos).map(PlatinumFluidVesselBlockEntity::getControllerBE)
            .map(be -> ComparatorUtil.fractionToRedstoneLevel(be.getFillState()))
            .orElse(0);
    }
}
