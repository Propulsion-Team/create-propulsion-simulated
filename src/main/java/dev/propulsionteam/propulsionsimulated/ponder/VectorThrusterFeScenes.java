package dev.propulsionteam.propulsionsimulated.ponder;

import java.util.List;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.creative_vector_thruster.CreativeVectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster.LiquidVectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionFluids;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Ponder for the Forge-Energy vector thruster (see {@code ponder_vector_thruster_normal.nbt}).
 * <p>
 * Sections: setup and reveal → intro + FE storage + generator → moving / links / copy → thrust on → thrust off.
 */
public final class VectorThrusterFeScenes {
    private VectorThrusterFeScenes() {
    }

    public static void vectorThrusterFe(SceneBuilder builder, SceneBuildingUtil util) {
        vectorThruster(builder, util, VectorSceneType.FE);
    }

    public static void creativeVectorThruster(SceneBuilder builder, SceneBuildingUtil util) {
        vectorThruster(builder, util, VectorSceneType.CREATIVE);
    }

    public static void liquidVectorThruster(SceneBuilder builder, SceneBuildingUtil util) {
        vectorThruster(builder, util, VectorSceneType.LIQUID);
    }

    private static void vectorThruster(SceneBuilder builder, SceneBuildingUtil util, VectorSceneType sceneType) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(sceneType.sceneId, sceneType.title);
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(12);

        BlockPos motorPos = util.grid().at(4, 1, 3);
        Selection machineLayer = util.select()
            .position(3, 1, 3)
            .add(util.select().fromTo(1, 1, 2, 1, 1, 5))
            .add(util.select().fromTo(2, 1, 2, 2, 1, 5));
        if (sceneType.hasPumpLine()) {
            machineLayer = machineLayer
                .add(util.select().position(4, 1, 3))
                .add(util.select().position(5, 1, 3));
        }
        Selection thrusterSel = util.select().position(3, 2, 3);
        BlockPos thrusterPos = util.grid().at(3, 2, 3);
        // Analog levers beside the thruster (ponder_vector_thruster_normal.nbt, column x = 1).
        BlockPos leverWestPos = util.grid().at(1, 1, 2);
        BlockPos leverEastPos = util.grid().at(1, 1, 3);
        BlockPos leverDownPos = util.grid().at(1, 1, 4);
        BlockPos leverUpPos = util.grid().at(1, 1, 5);
        LeverControl leverWest = new LeverControl(util.select().position(leverWestPos), leverWestPos);
        LeverControl leverEast = new LeverControl(util.select().position(leverEastPos), leverEastPos);
        LeverControl leverDown = new LeverControl(util.select().position(leverDownPos), leverDownPos);
        LeverControl leverUp = new LeverControl(util.select().position(leverUpPos), leverUpPos);
        Selection linkBlocks = util.select().fromTo(2, 1, 2, 2, 1, 5);
        Selection kineticNet = util.select().position(4, 1, 3);
        resetVectorControls(scene, linkBlocks, leverWest, leverEast, leverDown, leverUp);

        // - Section 1: reveal layout + optional pump line -------------------
        scene.idle(8);
        if (sceneType.hasPumpLine()) {
            Block pumpBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_pump"));
            BlockState pumpState = pumpBlock.defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.WEST);
            scene.world().setBlock(motorPos, pumpState, false);
            scene.idle(6);
        }
        scene.world().showSection(machineLayer, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(thrusterSel, Direction.DOWN);
        scene.idle(12);
        if (sceneType.hasPumpLine()) {
            scene.world().setKineticSpeed(kineticNet, 32.0f);
            scene.effects().rotationSpeedIndicator(motorPos);
            scene.idle(20);
        }

        // - Section 2: main intro (on thruster) ------------------------─
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText(sceneType.introKey)
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(100);
        scene.idle(25);

        showVariantSetup(scene, util, sceneType, thrusterSel, thrusterPos);

        // - Section 4: analog levers + vector tilt (ponder does not tick BEs / links - thruster synced after) ─
        scene.overlay().showText(85)
            .attachKeyFrame()
            .sharedText("vector_thruster_fe.moving.intro")
            .pointAt(util.vector().centerOf(util.grid().at(3, 2, 3)))
            .placeNearTarget();
        scene.idle(90);
        runVectorCircle(scene, sceneType, thrusterPos, leverWest, leverEast, leverDown, leverUp, 40);
        animateVectorTransition(scene, sceneType, thrusterPos, leverWest, leverEast, leverDown, leverUp,
            new Step(15, 0, 0, 15), new Step(0, 0, 0, 0), 24);
        scene.idle(25);

        // - Section 5: rotate view + highlight embedded redstone link sides ----------
        scene.overlay().showText(95)
            .attachKeyFrame()
            .sharedText("vector_thruster_fe.moving.text_3")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(45);
        scene.effects().indicateSuccess(thrusterPos.west());
        scene.idle(8);
        scene.effects().indicateSuccess(thrusterPos.north());
        scene.idle(30);
        scene.rotateCameraY(180);
        scene.idle(15);
        scene.effects().indicateSuccess(thrusterPos.east());
        scene.idle(8);
        scene.effects().indicateSuccess(thrusterPos.south());
        scene.idle(25);
        scene.rotateCameraY(-180);
        scene.idle(100);

        scene.overlay().showText(90)
            .sharedText("vector_thruster_fe.moving.text_1")
            .pointAt(util.vector().centerOf(util.grid().at(3, 2, 3)))
            .placeNearTarget();
        scene.idle(100);
        // - Section 6: thrust power (redstone) + ion plume, then power down ----------
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText(sceneType.powerKey)
            .pointAt(util.vector().centerOf(util.grid().at(3, 2, 3)))
            .placeNearTarget();
        scene.idle(95);
        powerThrusterForScene(scene, sceneType, thrusterSel, thrusterPos);
        scene.effects().indicateRedstone(util.grid().at(3, 2, 3));
        emitIonBurst(scene, util, 3, 2, 3, Direction.UP, 10);
        scene.idle(100);
        unpowerThrusterForScene(scene, sceneType, thrusterSel);
        scene.idle(35);
        scene.markAsFinished();
    }

    private enum VectorSceneType {
        FE("ponder_vector_thruster_normal", "Vector Thruster (Forge Energy)",
            "vector_thruster_fe.intro.main", "vector_thruster_fe.powering.intro", true),
        CREATIVE("ponder_creative_vector_thruster", "Creative Vector Thruster",
            "vector_thruster_creative.intro.main", "vector_thruster_creative.powering.intro", false),
        LIQUID("ponder_liquid_vector_thruster", "Liquid Vector Thruster",
            "vector_thruster_liquid.intro.main", "vector_thruster_liquid.powering.intro", true);

        private final String sceneId;
        private final String title;
        private final String introKey;
        private final String powerKey;
        private final boolean hasPumpLine;

        VectorSceneType(String sceneId, String title, String introKey, String powerKey, boolean hasPumpLine) {
            this.sceneId = sceneId;
            this.title = title;
            this.introKey = introKey;
            this.powerKey = powerKey;
            this.hasPumpLine = hasPumpLine;
        }

        private boolean hasPumpLine() {
            return hasPumpLine;
        }
    }

    private record LeverControl(Selection selection, BlockPos pos) {
    }

    private record Step(int w, int e, int d, int u) {
    }

    private static void showVariantSetup(CreateSceneBuilder scene, SceneBuildingUtil util, VectorSceneType sceneType,
        Selection thrusterSel, BlockPos thrusterPos) {
        switch (sceneType) {
            case FE -> {
                scene.overlay().showText(85)
                    .attachKeyFrame()
                    .sharedText("vector_thruster_fe.intro.fe")
                    .pointAt(util.vector().centerOf(thrusterPos))
                    .placeNearTarget();
                scene.idle(90);
                int feCap = PropulsionConfig.ION_THRUSTER_ENERGY_CAPACITY_FE.get();
                scene.overlay().showText(90)
                    .attachKeyFrame()
                    .sharedText("vector_thruster_fe.intro.fe_storage", feCap)
                    .pointAt(util.vector().centerOf(thrusterPos))
                    .placeNearTarget();
                scene.idle(95);
                scene.world().modifyBlockEntityNBT(thrusterSel, IonThrusterBlockEntity.class,
                    nbt -> nbt.putInt("EnergyStored", feCap));
                scene.idle(10);
                scene.overlay().showText(95)
                    .attachKeyFrame()
                    .sharedText("vector_thruster_fe.intro.generators")
                    .pointAt(util.vector().blockSurface(util.grid().at(3, 1, 3), Direction.DOWN))
                    .placeNearTarget();
                scene.idle(100);
            }
            case CREATIVE -> {
                scene.overlay().showText(95)
                    .attachKeyFrame()
                    .sharedText("vector_thruster_creative.intro.thrust")
                    .pointAt(util.vector().blockSurface(util.grid().at(3, 2, 3), Direction.DOWN))
                    .placeNearTarget();
                scene.overlay().showScrollInput(util.vector().blockSurface(util.grid().at(3, 1, 3), Direction.DOWN),
                    Direction.DOWN, 80);
                scene.idle(105);
            }
            case LIQUID -> {
                scene.overlay().showText(95)
                    .attachKeyFrame()
                    .sharedText("vector_thruster_liquid.intro.fuel")
                    .pointAt(util.vector().centerOf(util.grid().at(4, 1, 3)))
                    .placeNearTarget();
                scene.idle(95);
                int fuelCapacity = PropulsionConfig.getLiquidVectorThrusterFuelTankCapacityMbOrDefault();
                scene.world().modifyBlockEntity(thrusterPos, LiquidVectorThrusterBlockEntity.class,
                    be -> be.tank.getPrimaryHandler().setFluid(new FluidStack(PropulsionFluids.TURPENTINE.get(), fuelCapacity)));
                scene.overlay().showText(90)
                    .attachKeyFrame()
                    .sharedText("vector_thruster_liquid.intro.pumped")
                    .pointAt(util.vector().blockSurface(util.grid().at(3, 1, 3), Direction.UP))
                    .placeNearTarget();
                scene.idle(100);
            }
        }
    }

    private static void powerThrusterForScene(CreateSceneBuilder scene, VectorSceneType sceneType,
        Selection thrusterSel, BlockPos thrusterPos) {
        switch (sceneType) {
            case FE -> {
                int feCap = PropulsionConfig.ION_THRUSTER_ENERGY_CAPACITY_FE.get();
                scene.world().modifyBlockEntityNBT(thrusterSel, IonThrusterBlockEntity.class, nbt -> {
                    nbt.putInt("EnergyStored", feCap);
                    nbt.putInt("RedstoneInput", 15);
                });
            }
            case CREATIVE -> scene.world().modifyBlockEntityNBT(thrusterSel, CreativeVectorThrusterBlockEntity.class,
                nbt -> nbt.putInt("RedstoneInput", 15));
            case LIQUID -> {
                scene.world().modifyBlockEntity(thrusterPos, LiquidVectorThrusterBlockEntity.class, be -> {
                    int fuelCapacity = PropulsionConfig.getLiquidVectorThrusterFuelTankCapacityMbOrDefault();
                    be.tank.getPrimaryHandler().setFluid(new FluidStack(PropulsionFluids.TURPENTINE.get(), fuelCapacity));
                });
                scene.world().modifyBlockEntityNBT(thrusterSel, LiquidVectorThrusterBlockEntity.class,
                    nbt -> nbt.putInt("RedstoneInput", 15));
            }
        }
    }

    private static void unpowerThrusterForScene(CreateSceneBuilder scene, VectorSceneType sceneType, Selection thrusterSel) {
        switch (sceneType) {
            case FE -> scene.world().modifyBlockEntityNBT(thrusterSel, IonThrusterBlockEntity.class,
                nbt -> nbt.putInt("RedstoneInput", 0));
            case CREATIVE -> scene.world().modifyBlockEntityNBT(thrusterSel, CreativeVectorThrusterBlockEntity.class,
                nbt -> nbt.putInt("RedstoneInput", 0));
            case LIQUID -> scene.world().modifyBlockEntityNBT(thrusterSel, LiquidVectorThrusterBlockEntity.class,
                nbt -> nbt.putInt("RedstoneInput", 0));
        }
    }

    private static void setLevelPower(CreateSceneBuilder scene,
        LeverControl west, LeverControl east, LeverControl down, LeverControl up,
        int w, int e, int d, int u) {
        setLeverState(scene, west, w);
        setLeverState(scene, east, e);
        setLeverState(scene, down, d);
        setLeverState(scene, up, u);
    }

    private static void resetVectorControls(CreateSceneBuilder scene, Selection linkBlocks,
        LeverControl west, LeverControl east, LeverControl down, LeverControl up) {
        scene.world().modifyBlocks(linkBlocks, state -> state.hasProperty(BlockStateProperties.POWERED)
            ? state.setValue(BlockStateProperties.POWERED, false)
            : state, false);
        setLevelPower(scene, west, east, down, up, 0, 0, 0, 0);
    }

    private static void setLeverState(CreateSceneBuilder scene, LeverControl lever, int power) {
        int clampedPower = Math.clamp(power, 0, 15);
        scene.world().modifyBlockEntityNBT(lever.selection(), AnalogLeverBlockEntity.class,
            nbt -> nbt.putInt("State", clampedPower));
    }

    private static void runVectorCircle(CreateSceneBuilder scene, VectorSceneType sceneType, BlockPos thrusterPos,
        LeverControl leverWest, LeverControl leverEast, LeverControl leverDown, LeverControl leverUp, int stepTicks) {
        Step[] loop = {
            new Step(15, 0, 0, 0),
            new Step(15, 0, 15, 0),
            new Step(0, 0, 15, 0),
            new Step(0, 15, 15, 0),
            new Step(0, 15, 0, 0),
            new Step(0, 15, 0, 15),
            new Step(0, 0, 0, 15),
            new Step(15, 0, 0, 15),
        };
        Step previous = new Step(0, 0, 0, 0);
        for (Step s : loop) {
            animateVectorTransition(scene, sceneType, thrusterPos, leverWest, leverEast, leverDown, leverUp, previous, s, stepTicks);
            previous = s;
        }
    }

    private static void animateVectorTransition(CreateSceneBuilder scene, VectorSceneType sceneType, BlockPos thrusterPos,
        LeverControl leverWest, LeverControl leverEast, LeverControl leverDown, LeverControl leverUp,
        Step from, Step to, int ticks) {
        int frames = Math.max(1, ticks / 2);
        int ticksPerFrame = Math.max(1, ticks / frames);
        for (int frame = 1; frame <= frames; frame++) {
            float progress = frame / (float) frames;
            int w = lerpPower(from.w, to.w, progress);
            int e = lerpPower(from.e, to.e, progress);
            int d = lerpPower(from.d, to.d, progress);
            int u = lerpPower(from.u, to.u, progress);
            setLevelPower(scene, leverWest, leverEast, leverDown, leverUp, w, e, d, u);
            if (frame == frames || frame % 8 == 0) {
                indicateLeverPower(scene, leverWest, w);
                indicateLeverPower(scene, leverEast, e);
                indicateLeverPower(scene, leverDown, d);
                indicateLeverPower(scene, leverUp, u);
            }
            float vx = (w - e) / 15.0f;
            float vy = (d - u) / 15.0f;
            applyVectorSignalsForScene(scene, sceneType, thrusterPos, vx, vy);
            scene.idle(ticksPerFrame);
        }
    }

    private static void applyVectorSignalsForScene(CreateSceneBuilder scene, VectorSceneType sceneType,
        BlockPos thrusterPos, float vx, float vy) {
        if (sceneType == VectorSceneType.LIQUID) {
            scene.world().modifyBlockEntity(thrusterPos, LiquidVectorThrusterBlockEntity.class, be -> {
                be.applyVectorSignalsForScene(vx, vy);
                be.animateVectorForScene();
            });
            return;
        }
        scene.world().modifyBlockEntity(thrusterPos, VectorThrusterBlockEntity.class, be -> {
            be.applyVectorSignalsForScene(vx, vy);
            be.animateVectorForScene();
        });
    }

    private static void indicateLeverPower(CreateSceneBuilder scene, LeverControl lever, int power) {
        if (power > 0) {
            scene.effects().indicateRedstone(lever.pos().below());
        }
    }

    private static int lerpPower(int from, int to, float progress) {
        return Math.clamp(Math.round(from + (to - from) * progress), 0, 15);
    }

    private static void emitIonBurst(CreateSceneBuilder scene, SceneBuildingUtil util, int x, int y, int z,
        Direction exhaustDirection, int particlesPerTick) {
        scene.effects().emitParticles(util.vector().centerOf(util.grid().at(x, y, z)).add(0, 0.4, 0), (world, px, py, pz) -> {
            for (int i = 0; i < particlesPerTick; i++) {
                double lateral = 0.08;
                double ox = (world.random.nextDouble() - 0.5) * lateral;
                double oy = (world.random.nextDouble() - 0.5) * lateral;
                double oz = (world.random.nextDouble() - 0.5) * lateral;
                switch (exhaustDirection.getAxis()) {
                    case X -> ox = 0.0;
                    case Y -> oy = 0.0;
                    case Z -> oz = 0.0;
                    default -> {
                    }
                }
                double vx = exhaustDirection.getStepX() * 0.55 + (world.random.nextDouble() - 0.5) * 0.02;
                double vy = exhaustDirection.getStepY() * 0.55 + (world.random.nextDouble() - 0.5) * 0.02;
                double vz = exhaustDirection.getStepZ() * 0.55 + (world.random.nextDouble() - 0.5) * 0.02;
                world.addParticle(new IonParticleData(List.of(), null, 0.55f), px + ox, py + oy, pz + oz, vx, vy, vz);
            }
        }, 1, 70);
    }
}
