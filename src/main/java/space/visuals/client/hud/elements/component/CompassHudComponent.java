package space.visuals.client.hud.elements.component;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import space.visuals.Zenith;
import space.visuals.base.font.Font;
import space.visuals.base.font.Fonts;
import space.visuals.base.theme.Theme;
import space.visuals.client.hud.elements.draggable.DraggableHudElement;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.CustomDrawContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

/**
 * MiniMap — мини-карта с фиксированной ориентацией (север = вверх).
 *
 * - Сетка и позиции игроков зависят от мировых координат (не от взгляда).
 * - Треугольник в центре вращается по yaw — показывает куда смотришь.
 * - ПКМ при открытом чате → показывает слайдер размера (drag влево/вправо).
 * - Размер: 50..200 px, сохраняется в конфиге.
 */
public class CompassHudComponent extends DraggableHudElement {

    // Размер карты (настраивается слайдером)
    private float mapSize = 90f;
    private static final float MAP_SIZE_MIN = 50f;
    private static final float MAP_SIZE_MAX = 200f;

    // Радиус видимости в блоках
    private static final float MAP_RADIUS = 48f;
    // Полуразмер точки игрока
    private static final float ARROW_HALF = 7f;

    // Слайдер размера
    private boolean sliderVisible  = false;   // показывать слайдер?
    private boolean sliderDragging = false;   // тянем слайдер?
    private float   sliderDragStartX = 0f;
    private float   sliderDragStartSize = 0f;

    public CompassHudComponent(String name, float initialX, float initialY,
                                float windowWidth, float windowHeight,
                                float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
    }

    // ── Вызывается из Interface при ПКМ ──────────────────────────────────────
    public void onRightClick() {
        sliderVisible = !sliderVisible;
    }

    // ── Вызывается из Interface при нажатии ЛКМ (action=1) ──────────────────
    public boolean onLeftPress(float mouseX, float mouseY) {
        if (!sliderVisible) return false;
        float sliderRect[] = getSliderRect();
        float sx = sliderRect[0], sy = sliderRect[1], sw = sliderRect[2];
        // Проверяем попадание в полосу слайдера
        if (mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + 10f) {
            sliderDragging = true;
            sliderDragStartX = mouseX;
            sliderDragStartSize = mapSize;
            return true; // поглощаем событие — не двигаем карту
        }
        return false;
    }

    // ── Вызывается из Interface при движении мыши (drag) ─────────────────────
    public void onMouseMove(float mouseX) {
        if (!sliderDragging) return;
        float sliderRect[] = getSliderRect();
        float sw = sliderRect[2];
        float delta = mouseX - sliderDragStartX;
        float range = MAP_SIZE_MAX - MAP_SIZE_MIN;
        mapSize = Math.max(MAP_SIZE_MIN, Math.min(MAP_SIZE_MAX,
                sliderDragStartSize + delta / sw * range));
    }

    // ── Вызывается из Interface при отпускании ЛКМ ───────────────────────────
    public void onLeftRelease() {
        sliderDragging = false;
    }

    public boolean isSliderDragging() { return sliderDragging; }

    /** [x, y, width, height] полосы слайдера */
    private float[] getSliderRect() {
        float sw = mapSize;
        float sx = this.x;
        float sy = this.y + mapSize + 4f;
        return new float[]{sx, sy, sw, 10f};
    }

    @Override
    public void render(CustomDrawContext ctx) {
        if (mc.player == null || mc.world == null) return;

        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA accent = theme.getColor();

        float mw = mapSize;
        float mh = mapSize;
        // Высота с учётом слайдера
        this.width  = mw;
        this.height = sliderVisible ? mh + 18f : mh;

        float mx = this.x;
        float my = this.y;
        float cx = mx + mw / 2f;
        float cy = my + mh / 2f;

        float pxPerBlock = (mw / 2f) / MAP_RADIUS;

        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        float yawDeg   = mc.player.getYaw();

        // ── Фон ──────────────────────────────────────────────────────────────
        DrawUtil.drawBlurHud(ctx.getMatrices(), mx, my, mw, mh, 21, BorderRadius.all(4), ColorRGBA.WHITE);
        ctx.drawRoundedRect(mx, my, mw, mh, BorderRadius.all(4f), new ColorRGBA(15, 15, 20, 210));

        // ── Scissor ───────────────────────────────────────────────────────────
        ctx.enableScissor((int) mx, (int) my, (int)(mx + mw), (int)(my + mh));

        // ── Сетка (фиксированная, север = вверх) ─────────────────────────────
        // В Minecraft: +X = восток (вправо), +Z = юг (вниз на карте)
        // Экранный X = мировой X, экранный Y = мировой Z
        float gridStep = 16f * pxPerBlock;
        ColorRGBA gridColor = new ColorRGBA(255, 255, 255, 22);

        // Смещение сетки: позиция игрока внутри чанка
        float offX = (float)(((playerX % 16) + 16) % 16) * pxPerBlock;
        float offZ = (float)(((playerZ % 16) + 16) % 16) * pxPerBlock;

        // Вертикальные линии (по X)
        for (float gx = cx - offX; gx >= mx; gx -= gridStep)
            ctx.drawRoundedRect(gx, my, 0.5f, mh, BorderRadius.ZERO, gridColor);
        for (float gx = cx - offX + gridStep; gx <= mx + mw; gx += gridStep)
            ctx.drawRoundedRect(gx, my, 0.5f, mh, BorderRadius.ZERO, gridColor);

        // Горизонтальные линии (по Z)
        for (float gy = cy - offZ; gy >= my; gy -= gridStep)
            ctx.drawRoundedRect(mx, gy, mw, 0.5f, BorderRadius.ZERO, gridColor);
        for (float gy = cy - offZ + gridStep; gy <= my + mh; gy += gridStep)
            ctx.drawRoundedRect(mx, gy, mw, 0.5f, BorderRadius.ZERO, gridColor);

        // ── Крестообразные линии по центру ───────────────────────────────────
        ColorRGBA crossColor = new ColorRGBA(255, 255, 255, 40);
        ctx.drawRoundedRect(cx - 0.5f, my, 1f, mh, BorderRadius.ZERO, crossColor);
        ctx.drawRoundedRect(mx, cy - 0.5f, mw, 1f, BorderRadius.ZERO, crossColor);

        // ── Другие игроки (позиция по мировым координатам, север = вверх) ────
        for (PlayerEntity other : mc.world.getPlayers()) {
            if (other == mc.player) continue;
            // Инвизка — не показываем
            if (other.isInvisible()) continue;
            // За стеной — не показываем (raycast от камеры до игрока)
            if (!isVisible(other)) continue;

            float screenDx = (float)((other.getX() - playerX) * pxPerBlock);
            float screenDz = (float)((other.getZ() - playerZ) * pxPerBlock);

            float px = cx + screenDx;
            float pz = cy + screenDz;

            if (px < mx || px > mx + mw || pz < my || pz > my + mh) continue;

            boolean isFriend = Zenith.getInstance().getFriendManager().isFriend(other.getGameProfile().getName());
            ColorRGBA dotColor = isFriend
                    ? new ColorRGBA(80, 220, 80, 255)   // зелёная — друг
                    : new ColorRGBA(220, 60, 60, 255);  // красная — враг

            float r = 3f;
            ctx.drawRoundedRect(px - r, pz - r, r * 2f, r * 2f, BorderRadius.all(r), dotColor);
        }

        // ── Мы — зелёная точка чуть крупнее ─────────────────────────────────
        drawArrow(ctx, cx, cy, ARROW_HALF, accent);

        ctx.disableScissor();

        // ── Рамка + акцент ────────────────────────────────────────────────────
        ctx.drawRoundedBorder(mx, my, mw, mh, 0.1f, BorderRadius.all(4f),
                theme.getForegroundStroke());
        DrawUtil.drawRoundedCorner(ctx.getMatrices(), mx, my, mw, mh, 0.1f, 12f,
                accent, BorderRadius.all(4f));

        // ── Метки сторон света (фиксированные) ───────────────────────────────
        Font font = Fonts.MEDIUM.getFont(4.5f);
        ColorRGBA labelColor = theme.getGrayLight();
        // Север — вверху по центру
        float nw = font.width("N");
        ctx.drawText(font, "N", cx - nw / 2f, my + 2f, accent);
        // Юг — внизу
        float sw2 = font.width("S");
        ctx.drawText(font, "S", cx - sw2 / 2f, my + mh - font.height() - 2f, labelColor);
        // Запад — слева
        ctx.drawText(font, "W", mx + 2f, cy - font.height() / 2f, labelColor);
        // Восток — справа
        float ew = font.width("E");
        ctx.drawText(font, "E", mx + mw - ew - 2f, cy - font.height() / 2f, labelColor);

        // ── Слайдер размера ───────────────────────────────────────────────────
        if (sliderVisible) {
            float[] sr = getSliderRect();
            float sx = sr[0], sy = sr[1], slW = sr[2], slH = sr[3];

            // Фон слайдера
            ctx.drawRoundedRect(sx, sy, slW, slH, BorderRadius.all(3f),
                    new ColorRGBA(15, 15, 20, 200));
            ctx.drawRoundedBorder(sx, sy, slW, slH, 0.1f, BorderRadius.all(3f),
                    theme.getForegroundStroke());

            // Заполненная часть
            float t = (mapSize - MAP_SIZE_MIN) / (MAP_SIZE_MAX - MAP_SIZE_MIN);
            ctx.drawRoundedRect(sx + 1f, sy + 1f, (slW - 2f) * t, slH - 2f,
                    BorderRadius.all(2f), accent.withAlpha(180));

            // Ручка
            float knobX = sx + (slW - 2f) * t - 2f;
            ctx.drawRoundedRect(knobX, sy, 4f, slH, BorderRadius.all(3f), accent);

            // Текст размера
            String sizeText = (int) mapSize + "px";
            float tw = font.width(sizeText);
            ctx.drawText(font, sizeText, sx + slW / 2f - tw / 2f, sy + (slH - font.height()) / 2f,
                    theme.getWhite());
        }
    }

    /** Мы — зелёная точка */
    private void drawArrow(CustomDrawContext ctx, float cx, float cy, float half, ColorRGBA color) {
        float r = 2.5f;
        ColorRGBA green = new ColorRGBA(80, 220, 80, 255);
        ctx.drawRoundedRect(cx - r - 0.5f, cy - r - 0.5f, (r + 0.5f) * 2f, (r + 0.5f) * 2f,
                BorderRadius.all(r + 0.5f), new ColorRGBA(40, 120, 40, 180));
        ctx.drawRoundedRect(cx - r, cy - r, r * 2f, r * 2f, BorderRadius.all(r), green);
    }

    /**
     * Проверяет виден ли игрок (не за стеной).
     * Кидаем raycast от камеры до центра/головы/ног игрока.
     */
    private boolean isVisible(PlayerEntity player) {
        if (mc.world == null) return false;
        net.minecraft.util.math.Vec3d cam = mc.gameRenderer.getCamera().getPos();
        net.minecraft.util.math.Box box = player.getBoundingBox();
        net.minecraft.util.math.Vec3d[] points = {
            box.getCenter(),
            new net.minecraft.util.math.Vec3d(box.getCenter().x, box.maxY - 0.1, box.getCenter().z),
            new net.minecraft.util.math.Vec3d(box.getCenter().x, box.minY + 0.1, box.getCenter().z),
        };
        for (net.minecraft.util.math.Vec3d point : points) {
            net.minecraft.world.RaycastContext ctx = new net.minecraft.world.RaycastContext(
                    cam, point,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE,
                    mc.player
            );
            if (mc.world.raycast(ctx).getType() == net.minecraft.util.hit.HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    // ── Сохранение/загрузка ──────────────────────────────────────────────────

    @Override
    public JsonObject save() {
        JsonObject obj = super.save();
        obj.addProperty("mapSize", mapSize);
        return obj;
    }

    @Override
    public void load(JsonObject obj) {
        super.load(obj);
        if (obj.has("mapSize")) {
            float s = obj.get("mapSize").getAsFloat();
            mapSize = Math.max(MAP_SIZE_MIN, Math.min(MAP_SIZE_MAX, s));
        }
    }
}
