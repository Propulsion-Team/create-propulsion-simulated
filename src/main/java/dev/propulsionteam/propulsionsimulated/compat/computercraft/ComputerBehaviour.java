package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.Function;

import javax.annotation.Nullable;

import dev.propulsionteam.propulsionsimulated.content.heat.engine.StirlingEngineBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.redstone_transmission.RedstoneTransmissionBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.creative_vector_thruster.CreativeVectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster.LiquidVectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.tilt_adapter.TiltAdapterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster.SolidFuelThrusterBlockEntity;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dan200.computercraft.api.peripheral.IPeripheral;

public class ComputerBehaviour extends AbstractComputerBehaviour {
    protected IPeripheral peripheral;
    protected Supplier<IPeripheral> peripheralSupplier;

    private static final Map<Class<? extends SmartBlockEntity>, Function<SmartBlockEntity, IPeripheral>> PERIPHERAL_FACTORIES = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <T extends SmartBlockEntity> void register(Class<T> clazz, Function<T, IPeripheral> factory) {
        PERIPHERAL_FACTORIES.put(clazz, be -> factory.apply((T) be));
    }

    static {
        register(IonThrusterBlockEntity.class, IonThrusterPeripheral::new);
        register(ThrusterBlockEntity.class, ThrusterPeripheral::new);
        register(SolidFuelThrusterBlockEntity.class, SolidFuelThrusterPeripheral::new);
        register(CreativeVectorThrusterBlockEntity.class, CreativeVectorThrusterPeripheral::new);
        register(VectorThrusterBlockEntity.class, VectorThrusterPeripheral::new);
        register(LiquidVectorThrusterBlockEntity.class, LiquidVectorThrusterPeripheral::new);
        register(CreativeThrusterBlockEntity.class, CreativeThrusterPeripheral::new);
        register(CoralGeneratorBlockEntity.class, CoralGeneratorPeripheral::new);
        register(StirlingEngineBlockEntity.class, StirlingEnginePeripheral::new);
        register(RedstoneTransmissionBlockEntity.class, RedstoneTransmissionPeripheral::new);
        register(TiltAdapterBlockEntity.class, TiltAdapterPeripheral::new);
    }

    public ComputerBehaviour(SmartBlockEntity blockEntity) {
        this(blockEntity, findPeripheralSupplier(blockEntity).orElseThrow(() ->
            new IllegalArgumentException("No peripheral available for " + blockEntity.getClass().getName())));
    }

    private ComputerBehaviour(SmartBlockEntity blockEntity, Supplier<IPeripheral> peripheralSupplier) {
        super(blockEntity);
        this.peripheralSupplier = peripheralSupplier;
    }

    @Nullable
    public static ComputerBehaviour tryCreate(SmartBlockEntity blockEntity) {
        return findPeripheralSupplier(blockEntity)
            .map(supplier -> new ComputerBehaviour(blockEntity, supplier))
            .orElse(null);
    }

    public static Optional<Supplier<IPeripheral>> findPeripheralSupplier(SmartBlockEntity blockEntity) {
        Class<?> current = blockEntity.getClass();
        while (current != null && SmartBlockEntity.class.isAssignableFrom(current)) {
            @SuppressWarnings("unchecked")
            Function<SmartBlockEntity, IPeripheral> factory =
                PERIPHERAL_FACTORIES.get((Class<? extends SmartBlockEntity>) current);
            if (factory != null) {
                return Optional.of(() -> factory.apply(blockEntity));
            }
            current = current.getSuperclass();
        }
        return Optional.empty();
    }

    /** @deprecated use {@link #findPeripheralSupplier} */
    public static Supplier<IPeripheral> getPeripheralFor(SmartBlockEntity blockEntity) {
        return findPeripheralSupplier(blockEntity).orElseThrow(() ->
            new IllegalArgumentException("No peripheral available for " + blockEntity.getClass().getName()));
    }

    @Override
    public IPeripheral getPeripheralCapability() {
        if (peripheral == null)
            peripheral = peripheralSupplier.get();
        return peripheral;
    }

    @Override
    public void removePeripheral() {
        peripheral = null;
    }
}
