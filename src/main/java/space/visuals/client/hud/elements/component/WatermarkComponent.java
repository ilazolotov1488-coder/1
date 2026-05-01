package space.visuals.client.hud.elements.component;

import space.visuals.base.user.UserInfo;
import space.visuals.Zenith;
import space.visuals.base.font.Font;
import space.visuals.base.font.Fonts;
import space.visuals.base.theme.Theme;
import space.visuals.client.hud.elements.HudElement;
import space.visuals.client.hud.elements.draggable.DraggableHudElement;
import space.visuals.utility.game.other.TextUtil;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.CustomDrawContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;

public class WatermarkComponent extends DraggableHudElement {
    private final List<HudElement> elements = new ArrayList<>();

    public WatermarkComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name,initialX, initialY,windowWidth,windowHeight,offsetX,offsetY,align);

    }

    @Override
    public void render(CustomDrawContext ctx) {
        float iconSize = 7f;
        float elementSpacing = 1f;
        float iconTextSpacing = 4f;
        float cellPadding = 5f;
        float borderRadius = 4f;
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();

        ColorRGBA mainBgColor =theme.getForegroundColor();
        ColorRGBA highlightBgColor = theme.getForegroundLight();
        ColorRGBA iconColor = theme.getColor();
        ColorRGBA textColor = theme.getWhite();
        Font font = Fonts.MEDIUM.getFont(6);

        elements.clear();
        elements.add(new HudElement("s", ()->"","") {
            @Override
            public void calculateWidth(Font font, float iconSize, float cellPadding, float iconTextSpacing) {
                this.width = cellPadding * 2 + 11f;
            }

            @Override
            public void drawContent(CustomDrawContext ctx, float blockX, float blockY, float blockHeight, float iconSize, float iconTextSpacing, ColorRGBA iconColor, ColorRGBA textColor, Font font) {
                float logoW = 11f, logoH = 7f;
                float logoX = blockX + (getWidth() - logoW) / 2f;
                float logoY = blockY + (blockHeight - logoH) / 2f;
                DrawUtil.drawRoundedTexture(ctx.getMatrices(), Zenith.id("icons/logotype.png"),
                        logoX, logoY, logoW, logoH, BorderRadius.all(2), ColorRGBA.WHITE);
            }
        });
        elements.add(new HudElement("2", ()->String.valueOf(UserInfo.getUsername())) {
            @Override
            public void calculateWidth(Font font, float iconSize, float cellPadding, float iconTextSpacing) {
                super.calculateWidth(font, iconSize, cellPadding, iconTextSpacing);
                this.width += 4f; // доп. отступ справа
            }
        });
        elements.add(new HudElement("G", ()-> String.valueOf(mc.getCurrentFps()),"fps"));
        elements.add(new HudElement("H",()-> mc.player.networkHandler.getServerInfo() == null ||mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid())==null  ? "0" : String.valueOf(mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()).getLatency()),"ms" ));
        elements.add(new HudElement("I",()-> mc.player.networkHandler.getServerInfo() == null ? "20" : TextUtil.formatNumber(Zenith.getInstance().getServerHandler().getTPS()).replace(",", ".").replace(".0", "") ,"tps"));

        float totalWidth = 0;
        for (HudElement el : elements) {
            el.calculateWidth(font, iconSize, cellPadding, iconTextSpacing);
            totalWidth += el.getWidth();
        }
        totalWidth +=4;
        float totalHeight = 17;
        this.width = totalWidth;
        this.height = totalHeight;

        DrawUtil.drawBlurHud(ctx.getMatrices(),x, y, width,height,21,BorderRadius.all(4),ColorRGBA.WHITE);


        float currentX = this.x;

        for (int i = 0; i < elements.size(); i++) {
            HudElement el = elements.get(i);


                BorderRadius r = (i == 0) ? BorderRadius.left(borderRadius, borderRadius) : (i == elements.size() - 1) ? BorderRadius.right(borderRadius, borderRadius) : BorderRadius.ZERO;
                ctx.drawRoundedRect(currentX, y, el.getWidth()+(i==elements.size()-1?4:0), totalHeight, r,i % 2 == 0? highlightBgColor:mainBgColor);


            el.drawContent(ctx, currentX, y, totalHeight, iconSize, iconTextSpacing, iconColor, textColor, font);
            currentX += el.getWidth() ;
        }
        ctx.drawRoundedBorder(x, y,totalWidth, totalHeight,0.01f,BorderRadius.all(4),theme.getForegroundStroke());

        DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y,totalWidth, totalHeight,0.01f,12f,theme.getColor(),BorderRadius.all(4));

    }



}
