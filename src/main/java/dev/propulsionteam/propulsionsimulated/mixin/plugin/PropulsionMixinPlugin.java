package dev.propulsionteam.propulsionsimulated.mixin.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.neoforged.fml.loading.FMLLoader;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class PropulsionMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Supplier<Boolean>> CONDITIONS = new HashMap<>();
    private static final boolean LOG_MIXINS = false;

    static {
        CONDITIONS.put("is_vsaddition_not_loaded", () -> FMLLoader.getLoadingModList().getModFileById("vs_addition") == null);
        CONDITIONS.put("is_createbigcannons_loaded", () -> FMLLoader.getLoadingModList().getModFileById("createbigcannons") != null);
        CONDITIONS.put("is_computercraft_loaded", () -> FMLLoader.getLoadingModList().getModFileById("computercraft") != null);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            ClassNode mixinClassNode = getClassNode(mixinClassName);
            String condition = getAnnotationValue(mixinClassNode, MixinIf.class);

            if (condition == null) { //Not annotated
                return true;
            }

            Supplier<Boolean> conditionSupplier = CONDITIONS.get(condition);
            if (conditionSupplier == null) {
                throw new RuntimeException("Unknown mixin condition '" + condition + "' for mixin " + mixinClassName);
            }
            if (LOG_MIXINS) LOGGER.info("Mixin " + mixinClassName + "is " + (conditionSupplier.get() ? "applied" : "not applied"));
            return conditionSupplier.get();
        } catch (IOException e) {
            throw new RuntimeException("Could not read mixin class " + mixinClassName, e);
        }
    }

    private ClassNode getClassNode(String className) throws IOException {
        String resourceName = className.replace('.', '/') + ".class";
        InputStream classInputStream = PropulsionMixinPlugin.class.getClassLoader().getResourceAsStream(resourceName);
        if (classInputStream == null) {
            throw new IOException("Could not find class " + className);
        }
        ClassReader classReader = new ClassReader(classInputStream);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        classInputStream.close();
        return classNode;
    }


    private <T> String getAnnotationValue(ClassNode classNode, Class<T> annotationClass) {
        String annotationDescriptor = "L" + annotationClass.getName().replace('.', '/') + ";";
        if (classNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : classNode.visibleAnnotations) {
                if (annotation.desc.equals(annotationDescriptor)) {
                    if (annotation.values != null && annotation.values.size() >= 2) {
                        return (String) annotation.values.get(1);
                    }
                }
            }
        }
        return null;
    }

    //Hehe
    @Override
    public void onLoad(String mixinPackage) {}
    @Override
    public String getRefMapperConfig() { return null; }
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
