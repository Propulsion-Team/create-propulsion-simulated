package dev.propulsionteam.propulsionsimulated.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster.SolidFuelThrusterBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class SolidFuelThrusterScenes {
    private static final Direction EXHAUST = Direction.WEST;
    private static final int PARTICLES_PER_TICK = 10;
    private static final double EXHAUST_SPEED = 0.12;

    private SolidFuelThrusterScenes() {
    }

    public static void solidFuelThruster(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("solid_fuel_thruster", "Solid Fuel Thruster");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos leverPos = util.grid().at(2, 1, 1);
        BlockPos thrusterPos = util.grid().at(2, 1, 2);
        BlockPos chestPos = util.grid().at(3, 1, 2);
        Selection thruster = util.select().position(thrusterPos);
        Selection lever = util.select().position(leverPos);
        Selection chest = util.select().position(chestPos);
        ItemStack coal = new ItemStack(Items.COAL);
        ItemStack wrench = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench")));

        setLever(scene, leverPos, false);
        setBurn(scene, thruster, thrusterPos, ItemStack.EMPTY, 0, false);
        setChestCoal(scene, chestPos, 0);

        scene.world().showSection(thruster, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("solid_fuel_thruster.intro")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(100);

        scene.overlay().showControls(util.vector().blockSurface(thrusterPos, Direction.NORTH), Pointing.DOWN, 30)
            .rightClick()
            .withItem(coal);
        scene.idle(35);
        setBurn(scene, thruster, thrusterPos, coal, 0, false);
        scene.effects().indicateSuccess(thrusterPos);
        scene.overlay().showText(80)
            .sharedText("solid_fuel_thruster.insert")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(lever, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("solid_fuel_thruster.power")
            .pointAt(util.vector().centerOf(leverPos))
            .placeNearTarget();
        scene.idle(30);
        setLever(scene, leverPos, true);
        scene.effects().indicateRedstone(leverPos);
        scene.idle(10);
        setBurn(scene, thruster, thrusterPos, coal, 400, true);
        ThrusterScenes.emitManualThrusterBurst(scene, util, 2, 1, 2, -0.4, 0, 0, EXHAUST, PARTICLES_PER_TICK, 190, EXHAUST_SPEED);
        scene.idle(60);

        scene.overlay().showText(100)
            .attachKeyFrame()
            .sharedText("solid_fuel_thruster.keeps_burning")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(20);
        setLever(scene, leverPos, false);
        scene.effects().indicateRedstone(leverPos);
        scene.idle(110);

        setBurn(scene, thruster, thrusterPos, ItemStack.EMPTY, 0, false);
        scene.idle(40);

        scene.world().showSection(chest, Direction.DOWN);
        setChestCoal(scene, chestPos, 4);
        scene.idle(15);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("solid_fuel_thruster.wrench")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(20);
        scene.overlay().showControls(util.vector().blockSurface(thrusterPos, Direction.NORTH), Pointing.DOWN, 30)
            .rightClick()
            .withItem(wrench);
        scene.idle(35);
        setHatch(scene, thruster, true);
        scene.effects().indicateSuccess(thrusterPos);
        scene.idle(60);

        scene.overlay().showText(110)
            .attachKeyFrame()
            .sharedText("solid_fuel_thruster.hatch")
            .pointAt(util.vector().centerOf(chestPos))
            .placeNearTarget();
        scene.idle(20);
        setLever(scene, leverPos, true);
        scene.effects().indicateRedstone(leverPos);
        scene.idle(10);
        int chestCoal = 4;
        for (int cycle = 0; cycle < 4; cycle++) {
            chestCoal--;
            setChestCoal(scene, chestPos, chestCoal);
            setBurn(scene, thruster, thrusterPos, coal, 400, true);
            ThrusterScenes.emitManualThrusterBurst(scene, util, 2, 1, 2, -0.4, 0, 0, EXHAUST, PARTICLES_PER_TICK, 60, EXHAUST_SPEED);
            scene.idle(60);
        }
        setLever(scene, leverPos, false);
        setBurn(scene, thruster, thrusterPos, ItemStack.EMPTY, 0, false);
        scene.idle(30);

        scene.overlay().showText(90)
            .sharedText("solid_fuel_thruster.empty")
            .pointAt(util.vector().centerOf(chestPos))
            .placeNearTarget();
        scene.idle(100);
        scene.markAsFinished();
    }

    private static void setLever(CreateSceneBuilder scene, BlockPos leverPos, boolean powered) {
        scene.world().modifyBlock(leverPos, state -> state.hasProperty(BlockStateProperties.POWERED)
            ? state.setValue(BlockStateProperties.POWERED, powered) : state, false);
    }

    private static void setBurn(CreateSceneBuilder scene, Selection thruster, BlockPos thrusterPos, ItemStack fuel,
        int burnTicks, boolean burning) {
        scene.world().modifyBlockEntity(thrusterPos, SolidFuelThrusterBlockEntity.class,
            be -> be.setFuelStack(fuel.isEmpty() ? ItemStack.EMPTY : fuel.copy()));
        scene.world().modifyBlockEntityNBT(thruster, SolidFuelThrusterBlockEntity.class, nbt -> {
            nbt.putInt("BurnTime", burning ? burnTicks : 0);
            nbt.putInt("TotalBurnTicks", burning ? burnTicks : 0);
        });
    }

    private static void setHatch(CreateSceneBuilder scene, Selection thruster, boolean open) {
        scene.world().modifyBlockEntityNBT(thruster, SolidFuelThrusterBlockEntity.class,
            nbt -> nbt.putBoolean("HatchOpen", open));
    }

    private static void setChestCoal(CreateSceneBuilder scene, BlockPos chestPos, int count) {
        scene.world().modifyBlockEntity(chestPos, ChestBlockEntity.class, be -> {
            for (int slot = 0; slot < be.getContainerSize(); slot++) {
                be.setItem(slot, ItemStack.EMPTY);
            }
            if (count > 0) {
                be.setItem(0, new ItemStack(Items.COAL, count));
            }
        });
    }
}
