package dev.propulsionteam.propulsionsimulated.ponder;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import dev.propulsionteam.propulsionsimulated.content.hydropump.HydropumpBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionFluids;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public final class HydropumpScenes {
    private static final int GENERATOR_FE = 100_000;
    private static final int PUMP_FE = 1_000;
    private static final int DRAIN_STEP_TICKS = 4;
    private static final int TIME_SCALE = 2;

    private HydropumpScenes() {
    }

    public static void hydropump(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("hydropump", "Hydropump");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos generatorPos = util.grid().at(0, 1, 2);
        BlockPos pumpPos = util.grid().at(2, 1, 2);
        BlockPos tankPos = util.grid().at(4, 1, 2);

        Selection generator = util.select().position(generatorPos);
        Selection pump = util.select().position(pumpPos);
        Selection tank = util.select().fromTo(4, 1, 2, 4, 3, 2);
        Selection pipes = util.select().position(1, 1, 2).add(util.select().position(3, 1, 2));
        Selection cables = util.select().fromTo(0, 2, 2, 2, 2, 2);

        setPumpPower(scene, pump, 0);
        setGeneratorEnergy(scene, generator, 0);
        setPumpSpeed(scene, pumpPos, 1000);
        fillTank(scene, tankPos, 0.9f);

        scene.world().showSection(tank, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(pipes.copy().add(pump), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(generator, Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("hydropump.intro")
            .pointAt(util.vector().centerOf(pumpPos))
            .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(cables, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
            .sharedText("hydropump.needs_power")
            .pointAt(util.vector().topOf(util.grid().at(2, 2, 2)))
            .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(70)
            .attachKeyFrame()
            .sharedText("hydropump.generator")
            .pointAt(util.vector().centerOf(generatorPos))
            .placeNearTarget();
        scene.idle(30);
        scene.overlay().showControls(util.vector().blockSurface(generatorPos, Direction.WEST), Pointing.RIGHT, 30)
            .rightClick()
            .withItem(new ItemStack(PropulsionItems.CORAL_BUCKET.get()));
        scene.idle(35);

        scene.world().modifyBlockEntity(generatorPos, CoralGeneratorBlockEntity.class, be -> {
            var handler = be.getFluidHandler(null);
            if (handler != null) {
                handler.fill(new FluidStack(PropulsionFluids.CORAL.get(), 1000), FluidAction.EXECUTE);
            }
        });
        setGeneratorEnergy(scene, generator, GENERATOR_FE);
        scene.effects().indicateSuccess(generatorPos);
        scene.idle(25);

        for (int x = 0; x <= 2; x++) {
            scene.effects().indicateRedstone(util.grid().at(x, 2, 2));
            scene.idle(5);
        }
        setPumpPower(scene, pump, PUMP_FE);
        scene.world().setKineticSpeed(pump, 32);
        scene.world().propagatePipeChange(pumpPos);
        scene.effects().indicateSuccess(pumpPos);
        scene.idle(15);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("hydropump.pumping")
            .pointAt(util.vector().centerOf(util.grid().at(3, 1, 2)))
            .placeNearTarget();
        drainTank(scene, tankPos, 1000, 90);
        scene.idle(5);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("hydropump.speed")
            .pointAt(util.vector().blockSurface(pumpPos, Direction.NORTH))
            .placeNearTarget();
        scene.overlay().showScrollInput(util.vector().blockSurface(pumpPos, Direction.NORTH), Direction.NORTH, 90);
        drainTank(scene, tankPos, 1000, 30);

        setPumpSpeed(scene, pumpPos, 250);
        scene.world().propagatePipeChange(pumpPos);
        scene.overlay().showText(80)
            .sharedText("hydropump.slow")
            .pointAt(util.vector().centerOf(tankPos.above()))
            .placeNearTarget();
        drainTank(scene, tankPos, 250, 90);

        setPumpSpeed(scene, pumpPos, 2000);
        scene.world().propagatePipeChange(pumpPos);
        scene.overlay().showText(80)
            .sharedText("hydropump.fast")
            .pointAt(util.vector().centerOf(tankPos.above()))
            .placeNearTarget();
        drainTank(scene, tankPos, 2000, 90);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("hydropump.any_target")
            .pointAt(util.vector().centerOf(util.grid().at(1, 1, 2)))
            .placeNearTarget();
        scene.idle(100);

        setPumpPower(scene, pump, 0);
        setGeneratorEnergy(scene, generator, 0);
        scene.world().setKineticSpeed(pump, 0);
        scene.world().propagatePipeChange(pumpPos);
        scene.effects().indicateRedstone(pumpPos);
        scene.overlay().showText(80)
            .sharedText("hydropump.no_power")
            .pointAt(util.vector().centerOf(pumpPos))
            .placeNearTarget();
        scene.idle(90);
        scene.markAsFinished();
    }

    static void setPumpPower(CreateSceneBuilder scene, Selection pump, int fe) {
        scene.world().modifyBlockEntityNBT(pump, HydropumpBlockEntity.class, nbt -> nbt.putInt("Energy", fe));
    }

    private static void setGeneratorEnergy(CreateSceneBuilder scene, Selection generator, int fe) {
        scene.world().modifyBlockEntityNBT(generator, CoralGeneratorBlockEntity.class,
            nbt -> nbt.putInt("EnergyStored", fe));
    }

    static void setPumpSpeed(CreateSceneBuilder scene, BlockPos pumpPos, int speed) {
        scene.world().modifyBlockEntity(pumpPos, HydropumpBlockEntity.class, be -> be.setPumpingSpeed(speed));
    }

    static void fillTank(CreateSceneBuilder scene, BlockPos tankPos, float fraction) {
        scene.world().modifyBlockEntity(tankPos, FluidTankBlockEntity.class, be -> {
            FluidTankBlockEntity controller = be.getControllerBE();
            if (controller == null) {
                return;
            }
            int amount = Math.round(controller.getTankInventory().getCapacity() * fraction);
            controller.getTankInventory().setFluid(new FluidStack(PropulsionFluids.CORAL.get(), amount));
        });
    }

    private static void drainTank(CreateSceneBuilder scene, BlockPos tankPos, int mbPerSecond, int ticks) {
        int perStep = Math.max(1, mbPerSecond * DRAIN_STEP_TICKS * TIME_SCALE / 20);
        for (int elapsed = 0; elapsed < ticks; elapsed += DRAIN_STEP_TICKS) {
            scene.world().modifyBlockEntity(tankPos, FluidTankBlockEntity.class, be -> {
                FluidTankBlockEntity controller = be.getControllerBE();
                if (controller != null) {
                    controller.getTankInventory().drain(perStep, FluidAction.EXECUTE);
                }
            });
            scene.idle(DRAIN_STEP_TICKS);
        }
    }
}
