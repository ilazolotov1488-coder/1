package space.visuals.client.modules.impl.render.taksa;

/**
 * Time-based smooth interpolation (like InfinityAnimation speed=150ms).
 */
public class SmoothValue {
    private float current;
    private float target;
    private long lastTime;
    private final int speedMs;

    public SmoothValue(float initial, int speedMs) {
        this.current = initial;
        this.target = initial;
        this.lastTime = System.currentTimeMillis();
        this.speedMs = speedMs;
    }

    public void set(float value) {
        this.target = value;
    }

    public float get() {
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / (float) speedMs;
        lastTime = now;
        current += (target - current) * Math.min(1f, dt);
        return current;
    }
}
