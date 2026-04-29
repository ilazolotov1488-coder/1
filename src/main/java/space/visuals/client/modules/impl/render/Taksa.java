package space.visuals.client.modules.impl.render;

import by.saskkeee.annotations.CompileToNative;
import com.adl.nativeprotect.Native;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.impl.render.taksa.TaksaBrain;
import space.visuals.client.modules.impl.render.taksa.TaksaModel;

@ModuleAnnotation(name = "Taksa", category = Category.MOVEMENT, description = "Питомец такса")
public final class Taksa extends Module {

    public static final Taksa INSTANCE = new Taksa();

    private static final Identifier TEXTURE = Zenith.id("textures/taksa.png");

    private final TaksaBrain brain = new TaksaBrain();
    private final TaksaModel model = new TaksaModel();

    private Taksa() {}

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        brain.setEntity(mc.player);
        brain.tick();
    }

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;

        Vec3d petPos = brain.getPos();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        MatrixStack matrices = event.getMatrix();
        matrices.push();
        matrices.translate(petPos.x - cam.x, petPos.y - cam.y, petPos.z - cam.z);

        int light = WorldRenderer.getLightmapCoordinates(mc.world, BlockPos.ofFloored(petPos));
        int overlay = OverlayTexture.DEFAULT_UV;

        RenderLayer layer = RenderLayer.getEntityTranslucent(TEXTURE);
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vc = immediate.getBuffer(layer);

        model.render(matrices, vc, light, overlay, brain, mc.player.age + event.getPartialTicks());

        immediate.draw(layer);
        matrices.pop();
    }

    @Override
    public void onDisable() {
        brain.setEntity(null);
        super.onDisable();
    }
}
