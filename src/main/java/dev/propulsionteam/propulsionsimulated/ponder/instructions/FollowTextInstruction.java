package dev.propulsionteam.propulsionsimulated.ponder.instructions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.world.phys.Vec3;

public class FollowTextInstruction extends TickingInstruction {
    private static final Field VEC_FIELD = findVecField();

    private final ElementLink<WorldSectionElement> link;
    private final Map<TextWindowElement, Anchor> anchors = new IdentityHashMap<>();

    public FollowTextInstruction(ElementLink<WorldSectionElement> link, int ticks) {
        super(false, ticks);
        this.link = link;
    }

    @Override
    public void reset(PonderScene scene) {
        super.reset(scene);
        anchors.clear();
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        if (VEC_FIELD == null) {
            return;
        }
        WorldSectionElement section = scene.resolve(link);
        if (section == null) {
            return;
        }
        Vec3 offset = section.getAnimatedOffset();
        for (PonderElement element : new ArrayList<>(scene.getElements())) {
            if (!(element instanceof TextWindowElement text)) {
                continue;
            }
            try {
                Vec3 current = (Vec3) VEC_FIELD.get(text);
                if (current == null) {
                    continue;
                }
                Anchor anchor = anchors.computeIfAbsent(text, key -> new Anchor(current, offset));
                VEC_FIELD.set(text, anchor.base.add(offset.subtract(anchor.offset)));
            } catch (IllegalAccessException ignored) {
                return;
            }
        }
    }

    private record Anchor(Vec3 base, Vec3 offset) {
    }

    private static Field findVecField() {
        try {
            Field field = TextWindowElement.class.getDeclaredField("vec");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
