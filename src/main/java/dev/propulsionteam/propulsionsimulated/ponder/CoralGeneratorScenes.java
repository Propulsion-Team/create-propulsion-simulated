package dev.propulsionteam.propulsionsimulated.ponder;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorFuelManager;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorFuelProperties;
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

public final class CoralGeneratorScenes {
    private static final int PUMP_FE = 1_000;

    private CoralGeneratorScenes() {
    }

    public static void coralGenerator(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("coral_generator", "Coral Generator");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos generatorPos = util.grid().at(2, 1, 2);
        BlockPos pumpPos = util.grid().at(3, 1, 2);
        BlockPos tankPos = util.grid().at(4, 1, 2);
        Selection generator = util.select().position(generatorPos);
        Selection pump = util.select().position(pumpPos);
        Selection tank = util.select().fromTo(4, 1, 2, 4, 3, 2);
        Selection cables = util.select().fromTo(2, 2, 2, 3, 2, 2);

        setGeneratorEnergy(scene, generator, 0);
        HydropumpScenes.setPumpPower(scene, pump, 0);
        HydropumpScenes.setPumpSpeed(scene, pumpPos, 400);
        HydropumpScenes.fillTank(scene, tankPos, 0.9f);

        scene.world().showSection(generator, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("coral_generator.intro")
            .pointAt(util.vector().centerOf(generatorPos))
            .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("coral_generator.fuel")
            .pointAt(util.vector().centerOf(generatorPos))
            .placeNearTarget();
        scene.idle(20);
        scene.overlay().showControls(util.vector().blockSurface(generatorPos, Direction.WEST), Pointing.RIGHT, 30)
            .rightClick()
            .withItem(new ItemStack(PropulsionItems.CORAL_BUCKET.get()));
        scene.idle(35);
        setGeneratorCoral(scene, generatorPos, 1000);
        scene.effects().indicateSuccess(generatorPos);
        scene.idle(55);

        CoralGeneratorFuelProperties properties = CoralGeneratorFuelManager.getProperties(PropulsionFluids.CORAL.get());
        if (properties != null && properties.fePerMb() > 0) {
            scene.overlay().showText(80)
                .sharedText("coral_generator.rate", properties.fePerMb())
                .pointAt(util.vector().centerOf(generatorPos))
                .placeNearTarget();
            scene.idle(90);
        }

        scene.world().showSection(cables, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(100)
            .attachKeyFrame()
            .sharedText("coral_generator.output")
            .pointAt(util.vector().topOf(generatorPos))
            .placeNearTarget();
        for (int step = 1; step <= 8; step++) {
            setGeneratorCoral(scene, generatorPos, 1000 - step * 100);
            setGeneratorEnergy(scene, generator, step * 20_000);
            scene.effects().indicateRedstone(util.grid().at(2, 2, 2));
            scene.idle(12);
        }
        scene.idle(10);

        scene.world().showSection(pump, Direction.DOWN);
        scene.world().showSection(tank, Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(110)
            .attachKeyFrame()
            .sharedText("coral_generator.feed")
            .pointAt(util.vector().centerOf(pumpPos))
            .placeNearTarget();
        scene.idle(10);
        scene.effects().indicateRedstone(util.grid().at(3, 2, 2));
        HydropumpScenes.setPumpPower(scene, pump, PUMP_FE);
        scene.world().setKineticSpeed(pump, 32);
        scene.world().propagatePipeChange(pumpPos);
        scene.effects().indicateSuccess(pumpPos);
        for (int step = 0; step < 10; step++) {
            transfer(scene, tankPos, generatorPos, 400);
            scene.idle(10);
        }
        scene.idle(10);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("coral_generator.storage", CoralGeneratorBlockEntity.FLUID_CAPACITY_MB,
                CoralGeneratorBlockEntity.ENERGY_CAPACITY)
            .pointAt(util.vector().centerOf(generatorPos))
            .placeNearTarget();
        scene.idle(100);
        scene.markAsFinished();
    }

    private static void setGeneratorEnergy(CreateSceneBuilder scene, Selection generator, int fe) {
        scene.world().modifyBlockEntityNBT(generator, CoralGeneratorBlockEntity.class,
            nbt -> nbt.putInt("EnergyStored", fe));
    }

    private static void setGeneratorCoral(CreateSceneBuilder scene, BlockPos generatorPos, int amount) {
        scene.world().modifyBlockEntity(generatorPos, CoralGeneratorBlockEntity.class, be -> {
            var handler = be.getFluidHandler(null);
            if (handler == null) {
                return;
            }
            handler.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
            if (amount > 0) {
                handler.fill(new FluidStack(PropulsionFluids.CORAL.get(), amount), FluidAction.EXECUTE);
            }
        });
    }

    private static void transfer(CreateSceneBuilder scene, BlockPos tankPos, BlockPos generatorPos, int amount) {
        scene.world().modifyBlockEntity(tankPos, FluidTankBlockEntity.class, be -> {
            FluidTankBlockEntity controller = be.getControllerBE();
            if (controller != null) {
                controller.getTankInventory().drain(amount, FluidAction.EXECUTE);
            }
        });
        scene.world().modifyBlockEntity(generatorPos, CoralGeneratorBlockEntity.class, be -> {
            var handler = be.getFluidHandler(null);
            if (handler != null) {
                handler.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
                handler.fill(new FluidStack(PropulsionFluids.CORAL.get(), 1000), FluidAction.EXECUTE);
            }
        });
    }
}
