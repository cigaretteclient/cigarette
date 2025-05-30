package io.github.waqfs.gui.util;

import net.minecraft.client.gui.DrawContext;

import java.util.Stack;

public class Scissor {
    private static class Bound {
        DrawContext context;
        int left;
        int top;
        int right;
        int bottom;

        Bound(DrawContext context, int left, int top, int right, int bottom) {
            this.context = context;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        void apply() {
            if (this.context == null) return;
            this.context.enableScissor(left, top, right, bottom);
        }

        void remove() {
            if (this.context == null) return;
            this.context.disableScissor();
        }
    }

    private static final Stack<Bound> exclusiveScissors = new Stack<>();

    public static void pushExclusive(DrawContext context, int left, int top, int right, int bottom) {
        Bound bound = new Bound(context, left, top, right, bottom);

        if (!exclusiveScissors.isEmpty()) {
            Bound previous = exclusiveScissors.peek();
            previous.remove();
        }

        exclusiveScissors.add(bound);
        bound.apply();
    }

    public static void popExclusive() {
        if (exclusiveScissors.isEmpty()) return;
        Bound latest = exclusiveScissors.pop();
        latest.remove();

        if (!exclusiveScissors.isEmpty()) {
            Bound next = exclusiveScissors.peek();
            next.apply();
        }
    }
}
