package dev.propulsionteam.propulsionsimulated.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster.RcsOrientation;
import dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster.RcsThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionFluids;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import dev.simulated_team.simulated.ponder.SmoothMovementUtils;
import dev.simulated_team.simulated.ponder.instructions.CustomAnimateWorldSectionInstruction;
import dev.propulsionteam.propulsionsimulated.ponder.instructions.FollowTextInstruction;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class RcsThrusterScenes {
    private RcsThrusterScenes() {
    }

    public static void singleRcs(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("single_rcs_thruster", "Single RCS Thruster");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos leverPos = util.grid().at(1, 1, 1);
        BlockPos linkPos = util.grid().at(2, 1, 1);
        BlockPos thrusterPos = util.grid().at(2, 2, 2);
        Selection controls = util.select().fromTo(1, 1, 1, 2, 1, 1);
        Selection ship = util.select().position(2, 2, 2).add(util.select().position(2, 3, 2));
        Selection thruster = util.select().position(thrusterPos);

        scene.world().setBlock(leverPos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "analog_lever")).defaultBlockState()
            .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), false);
        setPower(scene, thruster, 0f);
        setControlsSignal(scene, util, leverPos, linkPos, 0);
        scene.world().modifyBlock(linkPos, state -> state.hasProperty(RedstoneLinkBlock.RECEIVER)
            ? state.setValue(RedstoneLinkBlock.RECEIVER, false) : state, false);

        ElementLink<WorldSectionElement> shipLink = scene.world().showIndependentSection(ship, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("single_rcs.intro")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(controls, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("single_rcs.power")
            .pointAt(util.vector().centerOf(linkPos))
            .placeNearTarget();
        scene.idle(100);

        scene.addInstruction(new FollowTextInstruction(shipLink, 900));

        signalOn(scene, util, leverPos, linkPos, thrusterPos, thruster, 1f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(shipLink, new Vec3(0, 1.5, 0), 170,
            SmoothMovementUtils.quadraticRise()));
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("single_rcs.burst", Math.round(PropulsionConfig.SINGLE_RCS_THRUST.get()))
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(95);
        scene.overlay().showText(70)
            .sharedText("single_rcs.push")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(75);
        signalOff(scene, util, leverPos, linkPos, thruster);
        fallBack(scene, shipLink, 1.5);

        signalOn(scene, util, leverPos, linkPos, thrusterPos, thruster, 0.4f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(shipLink, new Vec3(0, 0.6, 0), 80,
            SmoothMovementUtils.quadraticRise()));
        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("single_rcs.steer")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(85);
        signalOff(scene, util, leverPos, linkPos, thruster);
        fallBack(scene, shipLink, 0.6);
        scene.idle(20);
        scene.markAsFinished();
    }

    public static void singleRcsHydrazine(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("single_rcs_thruster_hydrazine", "Boosting the Single RCS Thruster with Hydrazine");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos leverPos = util.grid().at(1, 1, 1);
        BlockPos linkPos = util.grid().at(2, 1, 1);
        BlockPos thrusterPos = util.grid().at(2, 2, 2);
        BlockPos tankPos = util.grid().at(2, 4, 2);
        Selection controls = util.select().fromTo(1, 1, 1, 2, 1, 1);
        Selection ship = util.select().position(2, 2, 2).add(util.select().position(2, 3, 2));
        Selection thruster = util.select().position(thrusterPos);
        double thrust = PropulsionConfig.SINGLE_RCS_THRUST.get();

        scene.world().setBlock(leverPos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "analog_lever")).defaultBlockState()
            .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), false);
        setPower(scene, thruster, 0f);
        setBoost(scene, thrusterPos, false);
        setControlsSignal(scene, util, leverPos, linkPos, 0);
        scene.world().modifyBlock(linkPos, state -> state.hasProperty(RedstoneLinkBlock.RECEIVER)
            ? state.setValue(RedstoneLinkBlock.RECEIVER, false) : state, false);

        ElementLink<WorldSectionElement> shipLink = scene.world().showIndependentSection(ship, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(controls, Direction.DOWN);
        scene.addInstruction(new FollowTextInstruction(shipLink, 2000));
        scene.idle(15);
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("rcs_hydrazine.intro")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(100);

        signalOn(scene, util, leverPos, linkPos, thrusterPos, thruster, 1f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(shipLink, new Vec3(0, 1, 0), 60,
            SmoothMovementUtils.quadraticRise()));
        scene.overlay().showText(70)
            .attachKeyFrame()
            .sharedText("rcs_hydrazine.normal", Math.round(thrust))
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(65);
        signalOff(scene, util, leverPos, linkPos, thruster);
        fallBack(scene, shipLink, 1);

        refuel(scene, util, thrusterPos, tankPos);

        setBoost(scene, thrusterPos, true);
        signalOn(scene, util, leverPos, linkPos, thrusterPos, thruster, 1f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(shipLink, new Vec3(0, 2, 0), 60,
            SmoothMovementUtils.quadraticRise()));
        scene.overlay().showText(70)
            .attachKeyFrame()
            .sharedText("rcs_hydrazine.boosted", Math.round(thrust * PropulsionConfig.RCS_HYDRAZINE_THRUST_MULTIPLIER.get()))
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(65);
        signalOff(scene, util, leverPos, linkPos, thruster);
        setBoost(scene, thrusterPos, false);
        fallBack(scene, shipLink, 2);

        usage(scene, util, thrusterPos);
    }

    public static void quadRcsHydrazine(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("rcs_thruster_hydrazine", "Boosting the Full RCS Thruster with Hydrazine");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos thrusterPos = util.grid().at(2, 2, 2);
        BlockPos tankPos = util.grid().at(2, 4, 2);
        Selection ship = util.select().position(2, 2, 2).add(util.select().position(2, 3, 2));
        double thrust = PropulsionConfig.RCS_THRUST.get();
        resetNozzles(scene, thrusterPos);
        setBoost(scene, thrusterPos, false);

        ElementLink<WorldSectionElement> shipLink = scene.world().showIndependentSection(ship, Direction.DOWN);
        scene.addInstruction(new FollowTextInstruction(shipLink, 2000));
        scene.idle(15);
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("rcs_hydrazine.intro")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("rcs_hydrazine.normal_nozzle", Math.round(thrust))
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(10);
        moveAndReturn(scene, thrusterPos, shipLink, new Vec3(MOVE_DISTANCE, 0, 0), new Direction[] {Direction.EAST});

        refuel(scene, util, thrusterPos, tankPos);

        setBoost(scene, thrusterPos, true);
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("rcs_hydrazine.boosted_nozzle", Math.round(thrust * PropulsionConfig.RCS_HYDRAZINE_THRUST_MULTIPLIER.get()))
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(10);
        moveAndReturn(scene, thrusterPos, shipLink, new Vec3(MOVE_DISTANCE * 2, 0, 0), new Direction[] {Direction.EAST});
        setBoost(scene, thrusterPos, false);

        usage(scene, util, thrusterPos);
    }

    private static void refuel(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos thrusterPos, BlockPos tankPos) {
        BlockPos pumpPos = util.grid().at(2, 3, 2);
        Selection supply = util.select().fromTo(1, 3, 2, 1, 4, 2).add(util.select().position(tankPos));
        Selection drive = util.select().fromTo(1, 3, 2, 1, 4, 2);
        Selection pump = util.select().position(pumpPos);
        scene.world().modifyBlockEntity(tankPos, FluidTankBlockEntity.class, be -> be.getTankInventory()
            .fill(new FluidStack(PropulsionFluids.HYDRAZINE.get(), 4000), FluidAction.EXECUTE));
        scene.world().setBlock(pumpPos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_pump")).defaultBlockState()
            .setValue(PumpBlock.FACING, Direction.DOWN), true);
        scene.idle(5);
        scene.world().showSection(supply, Direction.DOWN);
        scene.idle(15);
        scene.world().setKineticSpeed(drive, 32);
        scene.world().setKineticSpeed(pump, -32);
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("rcs_hydrazine.fill")
            .pointAt(util.vector().centerOf(tankPos))
            .placeNearTarget();
        scene.idle(40);
        scene.world().modifyBlockEntity(tankPos, FluidTankBlockEntity.class, be -> be.getTankInventory()
            .drain(1000, FluidAction.EXECUTE));
        scene.world().modifyBlockEntity(thrusterPos, RcsThrusterBlockEntity.class, be -> be.tank.getPrimaryHandler()
            .fill(new FluidStack(PropulsionFluids.HYDRAZINE.get(), 1000), FluidAction.EXECUTE));
        scene.effects().indicateSuccess(thrusterPos);
        scene.idle(60);
        scene.world().setKineticSpeed(drive, 0);
        scene.world().setKineticSpeed(pump, 0);
        scene.world().hideSection(supply, Direction.UP);
        scene.idle(15);
        scene.world().setBlock(pumpPos, PropulsionBlocks.PLATINUM_CASING.get().defaultBlockState(), true);
        scene.idle(15);
    }

    private static void usage(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos thrusterPos) {
        scene.overlay().showText(110)
            .attachKeyFrame()
            .sharedText("rcs_hydrazine.usage")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(120);
        scene.markAsFinished();
    }

    private static void setBoost(CreateSceneBuilder scene, BlockPos thrusterPos, boolean boost) {
        scene.world().modifyBlockEntity(thrusterPos, RcsThrusterBlockEntity.class, be -> be.applyBoostForScene(boost));
    }

    private static void signalOn(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos leverPos, BlockPos linkPos,
        BlockPos thrusterPos, Selection thruster, float power) {
        int signal = Math.round(Math.clamp(power, 0f, 1f) * 15f);
        setControlsSignal(scene, util, leverPos, linkPos, signal);
        scene.effects().indicateRedstone(leverPos);
        scene.idle(5);
        scene.effects().indicateRedstone(linkPos);
        scene.overlay().showLine(PonderPalette.GREEN, util.vector().centerOf(linkPos),
            util.vector().centerOf(thrusterPos), 14);
        scene.idle(8);
        setPower(scene, thruster, signal / 15f);
    }

    private static void signalOff(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos leverPos, BlockPos linkPos, Selection thruster) {
        setPower(scene, thruster, 0f);
        setControlsSignal(scene, util, leverPos, linkPos, 0);
    }

    private static void fallBack(CreateSceneBuilder scene, ElementLink<WorldSectionElement> shipLink, double height) {
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(shipLink, new Vec3(0, -height, 0), 30,
            SmoothMovementUtils.quadraticRise()));
        scene.idle(35);
    }

    private static void setPower(CreateSceneBuilder scene, Selection thruster, float power) {
        scene.world().modifyBlockEntity(thruster.iterator().next(), RcsThrusterBlockEntity.class,
            be -> be.applyPowerForScene(power));
    }

    private static void setControlsSignal(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos leverPos, BlockPos linkPos, int signal) {
        scene.world().modifyBlockEntityNBT(util.select().position(leverPos), AnalogLeverBlockEntity.class,
            nbt -> nbt.putInt("State", signal));
        scene.world().modifyBlock(linkPos, state -> state.hasProperty(BlockStateProperties.POWERED)
            ? state.setValue(BlockStateProperties.POWERED, signal > 0) : state, false);
    }

    private static final Direction QUAD_NORMAL = Direction.DOWN;
    private static final double MOVE_DISTANCE = 1.0;
    private static final int PHASE_TICKS = 20;

    public static void quadRcs(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("rcs_thruster", "Full RCS Thruster");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos thrusterPos = util.grid().at(2, 2, 2);
        Selection ship = util.select().position(2, 2, 2).add(util.select().position(2, 3, 2));
        resetNozzles(scene, thrusterPos);

        ElementLink<WorldSectionElement> shipLink = scene.world().showIndependentSection(ship, Direction.DOWN);
        scene.addInstruction(new FollowTextInstruction(shipLink, 2000));
        scene.idle(15);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("rcs.intro")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(100)
            .attachKeyFrame()
            .sharedText("rcs.power")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            scene.effects().indicateRedstone(thrusterPos.relative(side));
            scene.idle(8);
        }
        scene.idle(70);

        scene.overlay().showText(90)
            .sharedText("rcs.thrust", Math.round(PropulsionConfig.RCS_THRUST.get()))
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(110)
            .attachKeyFrame()
            .sharedText("rcs.push")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(10);

        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (Direction direction : directions) {
            moveAndReturn(scene, thrusterPos, shipLink,
                new Vec3(direction.getStepX(), 0, direction.getStepZ()).scale(MOVE_DISTANCE),
                new Direction[] {direction});
        }

        scene.overlay().showText(100)
            .attachKeyFrame()
            .sharedText("rcs.diagonal")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(10);
        moveAndReturn(scene, thrusterPos, shipLink, new Vec3(MOVE_DISTANCE, 0, -MOVE_DISTANCE).scale(0.8),
            new Direction[] {Direction.NORTH, Direction.EAST});
        scene.idle(30);
        scene.markAsFinished();
    }

    private static void moveAndReturn(CreateSceneBuilder scene, BlockPos thrusterPos,
        ElementLink<WorldSectionElement> shipLink, Vec3 offset, Direction[] pushDirections) {
        Vec3 half = offset.scale(0.5);
        Vec3 halfBack = offset.scale(-0.5);
        Direction[] brake = opposite(pushDirections);

        fire(scene, thrusterPos, pushDirections, 1f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(shipLink, half, PHASE_TICKS,
            SmoothMovementUtils.quadraticRise()));
        scene.idle(PHASE_TICKS);

        fire(scene, thrusterPos, pushDirections, 0f);
        fire(scene, thrusterPos, brake, 1f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(shipLink, half, PHASE_TICKS,
            SmoothMovementUtils.quadraticRiseOut()));
        scene.idle(PHASE_TICKS);
        fire(scene, thrusterPos, brake, 0f);
        scene.idle(15);

        fire(scene, thrusterPos, brake, 1f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(shipLink, halfBack, PHASE_TICKS,
            SmoothMovementUtils.quadraticRise()));
        scene.idle(PHASE_TICKS);

        fire(scene, thrusterPos, brake, 0f);
        fire(scene, thrusterPos, pushDirections, 1f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(shipLink, halfBack, PHASE_TICKS,
            SmoothMovementUtils.quadraticRiseOut()));
        scene.idle(PHASE_TICKS);
        fire(scene, thrusterPos, pushDirections, 0f);
        scene.idle(15);
    }

    private static Direction[] opposite(Direction[] directions) {
        Direction[] result = new Direction[directions.length];
        for (int i = 0; i < directions.length; i++) {
            result[i] = directions[i].getOpposite();
        }
        return result;
    }

    private static void fire(CreateSceneBuilder scene, BlockPos thrusterPos, Direction[] pushDirections, float power) {
        for (Direction push : pushDirections) {
            int nozzle = nozzleForPush(push);
            scene.world().modifyBlockEntity(thrusterPos, RcsThrusterBlockEntity.class,
                be -> be.applyNozzlePowerForScene(nozzle, power));
        }
    }

    private static void resetNozzles(CreateSceneBuilder scene, BlockPos thrusterPos) {
        for (int nozzle = 0; nozzle < 4; nozzle++) {
            int index = nozzle;
            scene.world().modifyBlockEntity(thrusterPos, RcsThrusterBlockEntity.class,
                be -> be.applyNozzlePowerForScene(index, 0f));
        }
    }

    private static int nozzleForPush(Direction push) {
        for (int nozzle = 0; nozzle < 4; nozzle++) {
            Direction exhaust = RcsOrientation.direction(RcsOrientation.exhaust(false, nozzle), QUAD_NORMAL);
            if (exhaust == push.getOpposite()) {
                return nozzle;
            }
        }
        return 0;
    }
}
