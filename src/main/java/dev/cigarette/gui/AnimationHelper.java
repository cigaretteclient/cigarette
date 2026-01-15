package dev.cigarette.gui;

/**
 * Helper class for smooth UI animations and transitions.
 */
public class AnimationHelper {
    private double current;
    private double target;
    private final double speed;

    public AnimationHelper(double initial, double speed) {
        this.current = initial;
        this.target = initial;
        this.speed = Math.max(0.01, Math.min(1.0, speed)); // Clamp between 0.01 and 1.0
    }

    /**
     * Sets the target value for the animation.
     */
    public void setTarget(double target) {
        this.target = target;
    }

    /**
     * Updates the animation, smoothly moving current toward target.
     * @param deltaTicks Time delta for frame-independent animation
     */
    public void update(float deltaTicks) {
        if (Math.abs(current - target) < 0.01) {
            current = target;
            return;
        }
        
        // Smooth interpolation with frame-independent speed
        double factor = 1.0 - Math.pow(1.0 - speed, deltaTicks);
        current += (target - current) * factor;
    }

    /**
     * Gets the current animated value.
     */
    public double get() {
        return current;
    }

    /**
     * Gets the current value as an integer.
     */
    public int getInt() {
        return (int) Math.round(current);
    }

    /**
     * Instantly sets both current and target to the given value.
     */
    public void set(double value) {
        this.current = value;
        this.target = value;
    }

    /**
     * Returns whether the animation has reached its target.
     */
    public boolean isComplete() {
        return Math.abs(current - target) < 0.01;
    }

    /**
     * Gets the progress of the animation (0.0 to 1.0).
     */
    public double getProgress() {
        return current / Math.max(1, target);
    }
}
