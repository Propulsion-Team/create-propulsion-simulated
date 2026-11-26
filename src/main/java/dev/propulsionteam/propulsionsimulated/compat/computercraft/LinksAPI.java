package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.propulsionteam.propulsionsimulated.utility.redstone_link.ShipScopedLinks;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class LinksAPI implements ILuaAPI {
    static final String EVENT = "redstone_link";

    private final IComputerSystem computer;
    private final Map<Couple<Frequency>, VirtualLink> transmitters = new LinkedHashMap<>();
    private final Map<Couple<Frequency>, VirtualLink> listeners = new LinkedHashMap<>();
    @Nullable
    private ServerLevel linkLevel;

    public LinksAPI(IComputerSystem computer) {
        this.computer = computer;
    }

    @Override
    public String[] getNames() {
        return new String[] {"links"};
    }

    @Override
    public void shutdown() {
        ServerThreadTasks.queue(this::clear);
    }

    private enum Scope {
        ALL, SHIP;

        String luaName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final class VirtualLink implements ShipScopedLinks.ShipScoped {
        final Couple<Frequency> key;
        final Scope scope;
        final boolean listening;
        int transmitted;
        int received;
        boolean alive = true;

        VirtualLink(Couple<Frequency> key, Scope scope, boolean listening) {
            this.key = key;
            this.scope = scope;
            this.listening = listening;
        }

        LinksAPI owner() {
            return LinksAPI.this;
        }

        @Override
        public boolean isShipScoped() {
            return scope == Scope.SHIP;
        }

        @Override
        public int getTransmittedStrength() {
            return alive ? transmitted : 0;
        }

        @Override
        public void setReceivedStrength(int power) {
            if (!alive || !listening || linkLevel == null) return;
            int now = signal(linkLevel, key, scope);
            if (now != received) {
                received = now;
                computer.queueEvent(EVENT, frequencyToLua(key.getFirst()), frequencyToLua(key.getSecond()), now);
            }
        }

        @Override
        public boolean isListening() {
            return listening;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public Couple<Frequency> getNetworkKey() {
            return key;
        }

        @Override
        public BlockPos getLocation() {
            return computer.getPosition();
        }
    }

    private static RedstoneLinkNetworkHandler handler() {
        return Create.REDSTONE_LINK_NETWORK_HANDLER;
    }

    private ServerLevel level() throws LuaException {
        ServerLevel level = computer.getLevel();
        if (level == null) throw new LuaException("Computer is not in a world");
        if (linkLevel != null && linkLevel != level) clear();
        linkLevel = level;
        return level;
    }

    private static Scope scope(Optional<String> name) throws LuaException {
        if (name.isEmpty()) return Scope.ALL;
        return switch (name.get().toLowerCase(Locale.ROOT)) {
            case "all" -> Scope.ALL;
            case "ship" -> Scope.SHIP;
            default -> throw new LuaException("Unknown scope '" + name.get() + "' (expected \"all\" or \"ship\")");
        };
    }

    @Nullable
    private SubLevel ownShip(ServerLevel level) {
        return ShipScopedLinks.subLevelOf(level, computer.getPosition());
    }

    private void requireShip(ServerLevel level, Scope scope) throws LuaException {
        if (scope == Scope.SHIP && ownShip(level) == null) {
            throw new LuaException("Scope \"ship\" needs the computer to be on a ship");
        }
    }

    private boolean isOwn(IRedstoneLinkable link) {
        return link instanceof VirtualLink v && v.owner() == this;
    }

    private int signal(ServerLevel level, Couple<Frequency> key, Scope scope) {
        Set<IRedstoneLinkable> network = handler().networksIn(level).get(key);
        if (network == null || network.isEmpty()) return 0;
        VirtualLink probe = new VirtualLink(key, scope, true);
        Predicate<IRedstoneLinkable> ignore = this::isOwn;
        if (scope == Scope.SHIP) {
            SubLevel ship = ownShip(level);
            ignore = ignore.or(link -> ship == null || ShipScopedLinks.subLevelOf(level, link.getLocation()) != ship);
        }
        return ShipScopedLinks.powerAt(level, network, probe, ignore);
    }

    static Frequency parseFrequency(@Nullable Object value, int argument) throws LuaException {
        if (value == null) return Frequency.EMPTY;
        String id;
        Integer color = null;
        if (value instanceof String s) {
            id = s;
        } else if (value instanceof Map<?, ?> table) {
            Object item = table.get("item");
            if (!(item instanceof String s)) {
                throw new LuaException("bad argument #" + argument + " (frequency table needs an 'item' string)");
            }
            id = s;
            Object c = table.get("color");
            if (c instanceof Number n) color = n.intValue() & 0xFFFFFF;
            else if (c != null) throw new LuaException("bad argument #" + argument + " ('color' must be a number)");
        } else {
            throw new LuaException("bad argument #" + argument + " (item id, frequency table or nil expected)");
        }
        if (id.isEmpty()) return Frequency.EMPTY;

        ResourceLocation location = ResourceLocation.tryParse(id);
        Optional<Item> item = location == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(location);
        if (item.isEmpty()) throw new LuaException("bad argument #" + argument + " (unknown item '" + id + "')");
        if (item.get() == Items.AIR) return Frequency.EMPTY;

        ItemStack stack = new ItemStack(item.get());
        if (color != null) stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, true));
        return Frequency.of(stack);
    }

    static Object frequencyToLua(Frequency frequency) {
        ItemStack stack = frequency.getStack();
        if (stack.isEmpty()) return "";
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
        if (color == null) return id;
        Map<String, Object> out = new HashMap<>();
        out.put("item", id);
        out.put("color", color.rgb());
        return out;
    }

    private static Couple<Frequency> key(IArguments args, int first) throws LuaException {
        return Couple.create(parseFrequency(args.get(first), first + 1), parseFrequency(args.get(first + 1), first + 2));
    }

    @LuaFunction(mainThread = true)
    public final void sendLinkSignal(IArguments args) throws LuaException {
        ServerLevel level = level();
        Couple<Frequency> key = key(args, 0);
        int strength = args.getInt(2);
        Scope scope = scope(args.optString(3));
        send(level, key, strength, scope);
    }

    @LuaFunction(mainThread = true)
    public final void sendLinkSignals(Map<?, ?> signals) throws LuaException {
        ServerLevel level = level();
        for (int i = 1; i <= signals.size(); i++) {
            if (!(signals.get((double) i) instanceof Map<?, ?> entry)) {
                throw new LuaException("Signal #" + i + " must be a table {f1, f2, strength [, scope]}");
            }
            Couple<Frequency> key = Couple.create(parseFrequency(entry.get(1.0), 1), parseFrequency(entry.get(2.0), 1));
            if (!(entry.get(3.0) instanceof Number strength)) {
                throw new LuaException("Signal #" + i + " needs a strength number");
            }
            Object scopeName = entry.get(4.0);
            if (scopeName != null && !(scopeName instanceof String)) {
                throw new LuaException("Signal #" + i + " scope must be a string");
            }
            send(level, key, strength.intValue(), scope(Optional.ofNullable((String) scopeName)));
        }
    }

    private void send(ServerLevel level, Couple<Frequency> key, int strength, Scope scope) throws LuaException {
        strength = Math.clamp(strength, 0, 15);
        VirtualLink existing = transmitters.get(key);
        if (strength == 0) {
            if (existing != null) remove(level, transmitters, existing);
            return;
        }
        requireShip(level, scope);
        if (existing != null && existing.scope != scope) {
            remove(level, transmitters, existing);
            existing = null;
        }
        if (existing == null) {
            VirtualLink link = new VirtualLink(key, scope, false);
            link.transmitted = strength;
            transmitters.put(key, link);
            handler().addToNetwork(level, link);
        } else if (existing.transmitted != strength) {
            existing.transmitted = strength;
            handler().updateNetworkOf(level, existing);
        }
    }

    private void remove(ServerLevel level, Map<Couple<Frequency>, VirtualLink> from, VirtualLink link) {
        from.remove(link.key);
        link.transmitted = 0;
        handler().removeFromNetwork(level, link);
        link.alive = false;
    }

    @LuaFunction(mainThread = true)
    public final List<List<Object>> getActiveSignals() {
        List<List<Object>> out = new ArrayList<>();
        for (VirtualLink link : transmitters.values()) {
            out.add(List.of(frequencyToLua(link.key.getFirst()), frequencyToLua(link.key.getSecond()),
                link.transmitted, link.scope.luaName()));
        }
        return out;
    }

    @LuaFunction(mainThread = true)
    public final int getLinkSignal(IArguments args) throws LuaException {
        ServerLevel level = level();
        Couple<Frequency> key = key(args, 0);
        Scope scope = scope(args.optString(2));
        requireShip(level, scope);
        return signal(level, key, scope);
    }

    @LuaFunction(mainThread = true)
    public final int listen(IArguments args) throws LuaException {
        ServerLevel level = level();
        Couple<Frequency> key = key(args, 0);
        Scope scope = scope(args.optString(2));
        requireShip(level, scope);
        VirtualLink existing = listeners.get(key);
        if (existing != null) {
            if (existing.scope == scope) return existing.received;
            remove(level, listeners, existing);
        }
        VirtualLink link = new VirtualLink(key, scope, true);
        link.received = signal(level, key, scope);
        listeners.put(key, link);
        handler().addToNetwork(level, link);
        return link.received;
    }

    @LuaFunction(mainThread = true)
    public final boolean unlisten(IArguments args) throws LuaException {
        ServerLevel level = level();
        VirtualLink link = listeners.get(key(args, 0));
        if (link == null) return false;
        remove(level, listeners, link);
        return true;
    }

    @LuaFunction(mainThread = true)
    public final List<List<Object>> scan(Optional<String> scopeName) throws LuaException {
        ServerLevel level = level();
        Scope scope = scope(scopeName);
        requireShip(level, scope);
        List<List<Object>> out = new ArrayList<>();
        for (Couple<Frequency> key : List.copyOf(handler().networksIn(level).keySet())) {
            int strength = signal(level, key, scope);
            if (strength > 0) {
                out.add(List.of(frequencyToLua(key.getFirst()), frequencyToLua(key.getSecond()), strength));
            }
        }
        return out;
    }

    @LuaFunction(mainThread = true)
    public final int getRange() {
        return ShipScopedLinks.linkRange();
    }

    @LuaFunction(mainThread = true)
    public final void clearAll() {
        clear();
    }

    private void clear() {
        ServerLevel level = linkLevel;
        if (level != null) {
            for (VirtualLink link : List.copyOf(transmitters.values())) remove(level, transmitters, link);
            for (VirtualLink link : List.copyOf(listeners.values())) remove(level, listeners, link);
        }
    }
}
