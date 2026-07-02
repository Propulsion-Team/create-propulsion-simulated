package dev.propulsionteam.propulsionsimulated.events;

import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.content.cable.fe.FeCableBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.heat.burners.liquid.LiquidBurnerBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.heat.burners.liquid.PassthroughFluidHandler;
import dev.propulsionteam.propulsionsimulated.content.cable.hub.CableHubBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.heat.burners.solid.SolidBurnerBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster.LiquidVectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster.SolidFuelThrusterBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class ModCapabilityEvents {
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            PropulsionBlockEntities.THRUSTER_BLOCK_ENTITY.get(),
            ModCapabilityEvents::getThrusterFluidHandler
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            PropulsionBlockEntities.LIQUID_VECTOR_THRUSTER_BLOCK_ENTITY.get(),
            ModCapabilityEvents::getLiquidVectorThrusterFluidHandler
        );

        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            PropulsionBlockEntities.LIQUID_BURNER_BLOCK_ENTITY.get(),
            ModCapabilityEvents::getLiquidBurnerFluidHandler
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            PropulsionBlockEntities.SOLID_BURNER_BLOCK_ENTITY.get(),
            ModCapabilityEvents::getSolidBurnerItemHandler
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            PropulsionBlockEntities.SOLID_FUEL_THRUSTER_BLOCK_ENTITY.get(),
            ModCapabilityEvents::getSolidFuelThrusterItemHandler
        );

        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            PropulsionBlockEntities.ION_THRUSTER_BLOCK_ENTITY.get(),
            (be, side) -> ((IonThrusterBlockEntity) be).getEnergyHandler(side)
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            PropulsionBlockEntities.CORAL_GENERATOR_BLOCK_ENTITY.get(),
            (be, side) -> ((CoralGeneratorBlockEntity) be).getFluidHandler(side)
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            PropulsionBlockEntities.PLATINUM_FLUID_TANK_BLOCK_ENTITY.get(),
            (be, side) -> be.getCapabilityHandler()
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            PropulsionBlockEntities.PLATINUM_FLUID_VESSEL_BLOCK_ENTITY.get(),
            (be, side) -> be.getCapabilityHandler()
        );
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            PropulsionBlockEntities.CORAL_GENERATOR_BLOCK_ENTITY.get(),
            (be, side) -> ((CoralGeneratorBlockEntity) be).getEnergyHandler(side)
        );
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            PropulsionBlockEntities.CABLE_HUB_BLOCK_ENTITY.get(),
            (be, side) -> ((CableHubBlockEntity) be).getEnergyHandler(side)
        );
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            PropulsionBlockEntities.FE_CABLE_BLOCK_ENTITY.get(),
            (be, side) -> ((FeCableBlockEntity) be).getEnergyHandler(side)
        );

        registerComputerCraftCapabilitiesIfAvailable(event);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerComputerCraftCapabilitiesIfAvailable(RegisterCapabilitiesEvent event) {
        if (!PropulsionCompatibility.CC_ACTIVE) {
            return;
        }
        try {
            Class<?> peripheralCapabilityClass = Class.forName("dan200.computercraft.api.peripheral.PeripheralCapability");
            Object peripheralCapability = peripheralCapabilityClass.getMethod("get").invoke(null);
            BlockCapability capability = (BlockCapability) peripheralCapability;

            event.registerBlockEntity(
                capability,
                PropulsionBlockEntities.THRUSTER_BLOCK_ENTITY.get(),
                (be, side) -> be.computerBehaviour == null ? null : be.computerBehaviour.getPeripheralCapability()
            );
            event.registerBlockEntity(
                capability,
                PropulsionBlockEntities.ION_THRUSTER_BLOCK_ENTITY.get(),
                (be, side) -> be.computerBehaviour == null ? null : be.computerBehaviour.getPeripheralCapability()
            );
            event.registerBlockEntity(
                capability,
                PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get(),
                (be, side) -> be.computerBehaviour == null ? null : be.computerBehaviour.getPeripheralCapability()
            );
            event.registerBlockEntity(
                capability,
                PropulsionBlockEntities.CREATIVE_VECTOR_THRUSTER_BLOCK_ENTITY.get(),
                (be, side) -> be.computerBehaviour == null ? null : be.computerBehaviour.getPeripheralCapability()
            );
            event.registerBlockEntity(
                capability,
                PropulsionBlockEntities.LIQUID_VECTOR_THRUSTER_BLOCK_ENTITY.get(),
                (be, side) -> be.computerBehaviour == null ? null : be.computerBehaviour.getPeripheralCapability()
            );
            event.registerBlockEntity(
                capability,
                PropulsionBlockEntities.SOLID_FUEL_THRUSTER_BLOCK_ENTITY.get(),
                (be, side) -> be.computerBehaviour == null ? null : be.computerBehaviour.getPeripheralCapability()
            );
            event.registerBlockEntity(
                capability,
                PropulsionBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY.get(),
                (be, side) -> be.computerBehaviour == null ? null : be.computerBehaviour.getPeripheralCapability()
            );
            event.registerBlockEntity(
                capability,
                PropulsionBlockEntities.REDSTONE_TRANSMISSION_BLOCK_ENTITY.get(),
                (be, side) -> be.computerBehaviour == null ? null : be.computerBehaviour.getPeripheralCapability()
            );
            event.registerBlockEntity(
                capability,
                PropulsionBlockEntities.TILT_ADAPTER_BLOCK_ENTITY.get(),
                (be, side) -> be.computerBehaviour == null ? null : be.computerBehaviour.getPeripheralCapability()
            );
            event.registerBlockEntity(
                capability,
                PropulsionBlockEntities.CORAL_GENERATOR_BLOCK_ENTITY.get(),
                (be, side) -> be.computerBehaviour == null ? null : be.computerBehaviour.getPeripheralCapability()
            );
        } catch (Throwable ignored) {
            // ComputerCraft not installed or API unavailable.
        }
    }

    private static IFluidHandler getThrusterFluidHandler(ThrusterBlockEntity thrusterBlockEntity, Direction side) {
        return thrusterBlockEntity.getFluidHandler(side);
    }

    private static IFluidHandler getLiquidVectorThrusterFluidHandler(LiquidVectorThrusterBlockEntity blockEntity, Direction side) {
        return blockEntity.getFluidHandler(side);
    }

    private static IFluidHandler getLiquidBurnerFluidHandler(LiquidBurnerBlockEntity blockEntity, Direction side) {
        IFluidHandler primaryHandler = blockEntity.getPrimaryFluidHandler();
        if (primaryHandler == null) {
            return null;
        }
        if (side == null) {
            return primaryHandler;
        }
        return new PassthroughFluidHandler(blockEntity, side);
    }

    private static IItemHandler getSolidBurnerItemHandler(SolidBurnerBlockEntity blockEntity, Direction side) {
        return blockEntity.getItemHandler(side);
    }

    private static IItemHandler getSolidFuelThrusterItemHandler(SolidFuelThrusterBlockEntity blockEntity, Direction side) {
        return blockEntity.getItemHandler(side);
    }
}
