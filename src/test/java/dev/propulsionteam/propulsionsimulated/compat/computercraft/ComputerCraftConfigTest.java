package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dan200.computercraft.api.lua.LuaFunction;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class ComputerCraftConfigTest {
    @Test
    void everyLuaFunctionIsConfigurable() throws ClassNotFoundException {
        for (ComputerCraftConfig.Group group : ComputerCraftConfig.GROUPS) {
            Class<?> type = Class.forName(
                    getClass().getPackageName() + "." + group.className(), false, getClass().getClassLoader());
            Set<String> declared = new TreeSet<>();
            for (Method method : type.getMethods()) {
                if (method.isAnnotationPresent(LuaFunction.class)) {
                    declared.add(method.getName());
                }
            }
            assertEquals(declared, new TreeSet<>(group.methods()), group.name());
        }
    }
}
