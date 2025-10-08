package dev.cigarette.gui;

import net.minecraft.client.gui.DrawContext;

import java.util.Stack;

/**
 * Custom scissor utility for screen rendering.
 */
public class Scissor {
    /**
     * An exclusive (non-additive) scissor. Use {@link #apply()} to activate and {@link #remove()} to deactivate.
     */
    private static class Bound {
        /**
         * The draw context this scissor is attached to.
         */
        DrawContext context;
        /**
         * The left edge of this bounding box.
         */
        int left;
        /**
         * The top edge of this bounding box.
         */
        int top;
        /**
         * The right edge of this bounding box.
         */
        int right;
        /**
         * The bottom edge of this bounding box.
         */
        int bottom;

        /**
         * Creates a new exclusive scissor.
         *
         * @param context The draw context to apply the scissor to
         * @param left    The left edge of the bounding box
         * @param top     The top edge of the bounding box
         * @param right   The right edge of the bounding box
         * @param bottom  The bottom edge of the bounding box
         */
        Bound(DrawContext context, int left, int top, int right, int bottom) {
            this.context = context;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        /**
         * Activate this scissor. This does not deactivate any other scissors that are applied.
         */
        void apply() {
            if (this.context == null) return;
            this.context.enableScissor(left, top, right, bottom);
        }

        /**
         * Deactivate this scissor.
         */
        void remove() {
            if (this.context == null) return;
            this.context.disableScissor();
        }
    }

    private static final Stack<Bound> exclusiveScissors = new Stack<>();

    /**
     * Push an exclusive (non-additive) scissor to a {@link DrawContext}.
     *
     * @param context The draw context to activate the scissor on
     * @param left    The left edge of the bounding box
     * @param top     The top edge of the bounding box
     * @param right   The right edge of the bounding box
     * @param bottom  The bottom edge of the bounding box
     */
    public static void pushExclusive(DrawContext context, int left, int top, int right, int bottom) {
        Bound bound = new Bound(context, left, top, right, bottom);

        if (!exclusiveScissors.isEmpty()) {
            Bound previous = exclusiveScissors.peek();
            previous.remove();
        }

        exclusiveScissors.add(bound);
        bound.apply();
    }

    /**
     * Pop the top-most exclusive scissor from the stack.
     */
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
