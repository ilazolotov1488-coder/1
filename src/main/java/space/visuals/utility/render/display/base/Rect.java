package space.visuals.utility.render.display.base;

import space.visuals.utility.math.MathUtil;

public record Rect(float x, float y, float width, float height) {
    public boolean contains(double mx, double my) {
        return MathUtil.isHovered(mx,my,x,y,width,height);
    }
}