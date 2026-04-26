package space.visuals.utility.render.display.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import space.visuals.utility.interfaces.IMinecraft;
import space.visuals.utility.mixin.accessors.ShaderProgramAccessor;

import java.util.ArrayList;
import java.util.List;

public class GlProgram implements IMinecraft {

    private static final List<Runnable> REGISTERED_PROGRAMS = new ArrayList<>();

    protected ShaderProgram backingProgram;
    protected ShaderProgramKey programKey;

    public GlProgram(Identifier id, VertexFormat vertexFormat) {
        this.programKey = new ShaderProgramKey(id.withPrefixedPath("core/"), vertexFormat, Defines.EMPTY);

        REGISTERED_PROGRAMS.add(
                () -> {
                    try {
                        this.backingProgram = mc.getShaderLoader().getProgramToLoad(programKey);
                        this.setup();
                    } catch (ShaderLoader.LoadException e) {
                        System.err.println("[GlProgram] FAILED to load " + programKey + ": " + e.getMessage());
                        e.printStackTrace();
                    } catch (Throwable t) {
                        System.err.println("[GlProgram] UNEXPECTED error loading " + programKey + ": " + t.getMessage());
                        t.printStackTrace();
                    }
                }
        );
    }

    public RenderPhase renderPhaseProgram() {
        return new RenderPhase.ShaderProgram(programKey);
    }

    public ShaderProgram use() {
        if (backingProgram == null) return null;
        try {
            return RenderSystem.setShader(programKey);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isReady() {
        return backingProgram != null;
    }

    protected void setup() {}

    public GlUniform findUniform(String name) {
        if (backingProgram == null) return null;
        try {
            return ((ShaderProgramAccessor) this.backingProgram).getUniformsByName().get(name);
        } catch (Exception e) {
            return null;
        }
    }

    @ApiStatus.Internal
    public static void loadAndSetupPrograms() {
        REGISTERED_PROGRAMS.forEach(Runnable::run);
    }

    @ApiStatus.Internal
    public static void clearPrograms() {
        REGISTERED_PROGRAMS.clear();
    }
}
