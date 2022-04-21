package tictim.ghostutils;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

public abstract class GhostUtilsRenderType extends RenderType{
	private GhostUtilsRenderType(String nameIn, VertexFormat formatIn, VertexFormat.Mode drawModeIn, int bufferSizeIn, boolean useDelegateIn, boolean needsSortingIn, Runnable setupTaskIn, Runnable clearTaskIn){
		super(nameIn, formatIn, drawModeIn, bufferSizeIn, useDelegateIn, needsSortingIn, setupTaskIn, clearTaskIn);
	}

	public static final RenderType GHOSTUTILS_LINES = RenderType.create("ghostutils_lines",
			DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.LINES,
			256,
			false,//affectsCrumbling
			true,//sortOnUpload
			CompositeState.builder()
					.setLineState(new LineStateShard(OptionalDouble.of(1.5)))
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setWriteMaskState(COLOR_WRITE)
					.setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
					.createCompositeState(false));
	public static final RenderType GHOSTUTILS_QUADS = RenderType.create("ghostutils_quads",
			DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS,
			256,
			false,
			true,
			RenderType.CompositeState.builder()
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setWriteMaskState(COLOR_WRITE)
					.setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
					.createCompositeState(false));
}