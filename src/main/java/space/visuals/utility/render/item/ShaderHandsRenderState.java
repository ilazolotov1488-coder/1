package space.visuals.utility.render.item;

public final class ShaderHandsRenderState {
    private static final ThreadLocal<State> STATE = new ThreadLocal<>();

    private ShaderHandsRenderState() {}

    public static void begin(float redMul, float greenMul, float blueMul, float alphaMul) {
        State current = STATE.get();
        if (current == null) { current = new State(); STATE.set(current); }
        ++current.depth;
        current.redMul   = redMul;
        current.greenMul = greenMul;
        current.blueMul  = blueMul;
        current.alphaMul = alphaMul;
    }

    public static void end() {
        State current = STATE.get();
        if (current == null) return;
        if (--current.depth <= 0) STATE.remove();
    }

    public static boolean isActive() {
        State current = STATE.get();
        return current != null && current.depth > 0;
    }

    public static float tintRed(float v)   { State s = STATE.get(); return s == null ? v : v * s.redMul; }
    public static float tintGreen(float v) { State s = STATE.get(); return s == null ? v : v * s.greenMul; }
    public static float tintBlue(float v)  { State s = STATE.get(); return s == null ? v : v * s.blueMul; }
    public static float tintAlpha(float v) { State s = STATE.get(); return s == null ? v : v * s.alphaMul; }

    private static final class State {
        int depth;
        float redMul, greenMul, blueMul, alphaMul;
    }
}
