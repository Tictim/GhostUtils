package tictim.ghostutils;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;

import java.util.OptionalDouble;

public abstract class GhostUtilsRenderType extends RenderType{
	private GhostUtilsRenderType(String nameIn, VertexFormat formatIn, int drawModeIn, int bufferSizeIn, boolean useDelegateIn, boolean needsSortingIn, Runnable setupTaskIn, Runnable clearTaskIn){
		super(nameIn, formatIn, drawModeIn, bufferSizeIn, useDelegateIn, needsSortingIn, setupTaskIn, clearTaskIn);
	}

	public static final RenderType GHOSTUTILS_LINES = RenderType.create("ghostutils_lines",
			DefaultVertexFormats.POSITION_COLOR,
			GL11.GL_LINES,
			256,
			RenderType.State.builder()
					.setLineState(new RenderState.LineState(OptionalDouble.of(1.5)))
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setOutputState(ITEM_ENTITY_TARGET)
					.setWriteMaskState(COLOR_DEPTH_WRITE)
					.setCullState(NO_CULL)
					.createCompositeState(false));
	public static final RenderType GHOSTUTILS_QUADS = RenderType.create("ghostutils_quads",
			DefaultVertexFormats.POSITION_COLOR,
			GL11.GL_QUADS,
			256,
			RenderType.State.builder()
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setWriteMaskState(COLOR_WRITE)
					.createCompositeState(false));
}

