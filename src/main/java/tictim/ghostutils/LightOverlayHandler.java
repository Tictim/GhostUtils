package tictim.ghostutils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.spawner.WorldEntitySpawner;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static tictim.ghostutils.GhostUtils.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class LightOverlayHandler{
	private LightOverlayHandler(){}

	private static final int X_RADIUS = 14;
	private static final int X_DIAMETER = 14*2;
	private static final int Y_LOWER_OFFSET = 14;
	private static final int Y_HEIGHT = 14+9;
	private static final int Z_RADIUS = 14;
	private static final int Z_DIAMETER = 14*2;

	private static final int NO_SPAWN = 0;
	private static final int SPAWN_IN_NIGHT = 1;
	private static final int SPAWN = 2;
	private static final int ZERO_LIGHT = 3;

	private static final LightViewFinder blockFinder = new LightViewFinder();

	private static final BlockPos.Mutable mpos = new BlockPos.Mutable();
	private static boolean lightOverlayEnabled;

	@SubscribeEvent
	public static void onTick(TickEvent.ClientTickEvent event){
		if(event.phase==TickEvent.Phase.START){
			if(Cfg.enableLightOverlay()){
				KeyBinding key = GhostUtils.ClientHandler.getToggleLightOverlay();
				if(key.getKeyConflictContext().isActive()&&key.consumeClick()) lightOverlayEnabled = !lightOverlayEnabled;
			}else lightOverlayEnabled = false;
		}
	}

	@SubscribeEvent
	public static void onTick(RenderWorldLastEvent event){
		if(!lightOverlayEnabled) return;
		Entity entity = Minecraft.getInstance().getCameraEntity();
		if(entity==null) return;
		RenderSystem.disableTexture();
		RenderSystem.disableLighting();
		GL11.glLineWidth(1.5f);

		Vector3d projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		IRenderTypeBuffer.Impl buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		MatrixStack stack = event.getMatrixStack();
		stack.pushPose();
		stack.translate(-projectedView.x, -projectedView.y, -projectedView.z);
		IVertexBuilder buffer = buffers.getBuffer(GhostUtilsRenderType.GHOSTUTILS_LINES);
		Matrix4f matrix = stack.last().pose();

		World world = entity.level;
		int x1 = MathHelper.floor(entity.getX())-X_RADIUS;
		int y1 = Math.max(MathHelper.floor(entity.getY())-Y_LOWER_OFFSET, 0);
		int z1 = MathHelper.floor(entity.getZ())-Z_RADIUS;
		for(int x = x1; x<=x1+X_DIAMETER; x++){
			for(int z = z1; z<=z1+Z_DIAMETER; z++){
				mpos.set(x, y1, z);
				for(int y = y1; y<y1+Y_HEIGHT; y++){
					if(world.getMaxBuildHeight()<y) break;
					//if(!frustum.isBoxInFrustum(x, y, z, x+1, y+0.004, z+1)) continue;
					mpos.setY(y);
					float r, g, b;
					switch(getSpawnMode(world)){
						case SPAWN_IN_NIGHT:
							r = 1;
							g = 1;
							b = 0;
							break;
						case SPAWN:
							r = 1;
							g = 0.35f;
							b = 0;
							break;
						case ZERO_LIGHT:
							r = 1;
							g = 0;
							b = 0;
							break;
						default: // case NO_SPAWN:
							continue;
					}
					final float dy = world.getBlockState(mpos)==Blocks.SNOW.defaultBlockState() ? y+(0.01f+(2/16f)) : y+0.01f;
					buffer.vertex(matrix, x, dy, z).color(r, g, b, 1).endVertex();
					buffer.vertex(matrix, x+1, dy, z+1).color(r, g, b, 1).endVertex();
					buffer.vertex(matrix, x+1, dy, z).color(r, g, b, 1).endVertex();
					buffer.vertex(matrix, x, dy, z+1).color(r, g, b, 1).endVertex();
				}
			}
		}

		buffers.endBatch();

		renderLightingPredicate(world, stack, buffers);

		stack.popPose();

		RenderSystem.enableLighting();
		RenderSystem.enableTexture();
	}

	private static int getSpawnMode(World world){
		if(!canCreatureTypeSpawnAtLocation(world, mpos)) return NO_SPAWN;
		int light = world.getBrightness(LightType.BLOCK, mpos);
		if(light>=8) return NO_SPAWN;
		int sky = world.getBrightness(LightType.SKY, mpos);
		if(sky>=8) return SPAWN_IN_NIGHT;
		else return Math.max(light, sky)==0 ? ZERO_LIGHT : SPAWN;
	}

	/**
	 * @see WorldEntitySpawner#isValidEmptySpawnBlock
	 */
	private static boolean canCreatureTypeSpawnAtLocation(World world, BlockPos pos){
		if(!world.getWorldBorder().isWithinBounds(pos)) return false;
		BlockPos down = pos.below();
		if(world.getBlockState(down).canCreatureSpawn(world, down, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, EntityType.ZOMBIE)){
			return WorldEntitySpawner.isValidEmptySpawnBlock(world, pos, world.getBlockState(pos), world.getFluidState(pos), EntityType.ZOMBIE);
		}
		return false;
	}

	/* @see Minecraft#rightClickMouse */
	private static void renderLightingPredicate(World world, MatrixStack stack, IRenderTypeBuffer.Impl buffers){
		ClientPlayerEntity player = Minecraft.getInstance().player;
		if(player==null) return;
		if(Minecraft.getInstance().hitResult!=null){
			RayTraceResult _t = Minecraft.getInstance().hitResult;
			if(_t instanceof BlockRayTraceResult){
				BlockRayTraceResult trace = (BlockRayTraceResult)_t;
				if(trace.getType()==RayTraceResult.Type.BLOCK){
					BlockState blockState = world.getBlockState(trace.getBlockPos());
					if(blockState.getMaterial()!=Material.AIR){// TODO BlockItemUseContext
						LightValueEstimate light = LightValueEstimate.getBrighter(
								getEstimatedLightValue(player, Hand.MAIN_HAND, trace),
								getEstimatedLightValue(player, Hand.OFF_HAND, trace));
						if(light!=null){
							List<BlockPos> list = blockFinder.reset(world, light.ctx.getClickedPos(), light.brightness).run();

							float alpha = (float)((Math.sin((world.getGameTime()%40/40.0)*(2*Math.PI))/2+0.5)*0.25+0.25);
							if(!list.isEmpty()){
								Matrix4f matrix = stack.last().pose();
								IVertexBuilder buffer = buffers.getBuffer(GhostUtilsRenderType.GHOSTUTILS_QUADS);
								for(BlockPos p : list){
									float x = p.getX(), y = world.getBlockState(p)==Blocks.SNOW.defaultBlockState() ? p.getY()+(0.005f+2/16f) : p.getY()+(0.005f), z = p.getZ();
									buffer.vertex(matrix, x, y, z).color(0, 1, 0, alpha).endVertex();
									buffer.vertex(matrix, x, y, z+1).color(0, 1, 0, alpha).endVertex();
									buffer.vertex(matrix, x+1, y, z+1).color(0, 1, 0, alpha).endVertex();
									buffer.vertex(matrix, x+1, y, z).color(0, 1, 0, alpha).endVertex();
								}
								buffers.endBatch();
							}
							RenderSystem.disableBlend();
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Nullable
	private static LightValueEstimate getEstimatedLightValue(PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult){
		ItemStack stack = player.getItemInHand(hand);
		if(stack.isEmpty()||!(stack.getItem() instanceof BlockItem)) return null;
		BlockItem item = (BlockItem)stack.getItem();
		Block b = item.getBlock();
		//noinspection ConstantConditions
		if(b!=null){
			BlockItemUseContext ctx = new BlockItemUseContext(new ItemUseContext(player, hand, rayTraceResult));
			if(ctx.canPlace()){
				BlockState state = b.getStateForPlacement(ctx);
				if(state!=null&&!state.hasTileEntity()){
					int lightValue = state.getLightEmission();
					return lightValue>7 ? new LightValueEstimate(ctx, state.getLightEmission()) : null;
				}
			}
		}
		return null;
	}

	private static final class LightValueEstimate{
		public final BlockItemUseContext ctx;
		public final int brightness;

		private LightValueEstimate(BlockItemUseContext ctx, int brightness){
			this.ctx = ctx;
			this.brightness = brightness;
		}

		@Nullable
		public static LightValueEstimate getBrighter(@Nullable LightValueEstimate light1, @Nullable LightValueEstimate light2){
			return light1==null ? light2 :
					light2==null ? light1 :
							light1.brightness>=light2.brightness ? light1 : light2;
		}
	}

	private static final class LightViewFinder{
		private final Queue<BlockPos> searchQueue = new ArrayDeque<>();
		private final Map<BlockPos, Integer> blockLightMap = new HashMap<>();
		private World world;

		public LightViewFinder reset(World world, BlockPos pos, int initialBlockLight){
			this.searchQueue.clear();
			this.blockLightMap.clear();
			this.world = world;
			if(initialBlockLight>=7) queue(pos, initialBlockLight);
			return this;
		}

		public List<BlockPos> run(){
			List<BlockPos> list = new ArrayList<>();
			BlockPos.Mutable mpos = new BlockPos.Mutable();
			while(!searchQueue.isEmpty()){
				BlockPos pos = searchQueue.remove();
				int lightValue = blockLightMap.get(pos);
				BlockState state = world.getBlockState(pos);
				if(lightValue>=8){
					int opacity = 1+state.getLightValue(world, pos);
					if(lightValue-opacity>=7){
						int c = lightValue-opacity;
						// Add nearby BlockPos to queue
						for(Direction d : Direction.values()) queue(mpos.set(pos).move(d), c);
					}
				}
				if(canCreatureTypeSpawnAtLocation(world, pos)) list.add(pos);
			}
			return list;
		}

		private void queue(BlockPos pos, int blockLight){
			pos = pos.immutable();
			if(blockLightMap.containsKey(pos)){
				if(this.blockLightMap.get(pos)<blockLight) this.blockLightMap.put(pos, blockLight);
			}else{
				this.blockLightMap.put(pos, blockLight);
				if(blockLight>7) this.searchQueue.add(pos);
			}
		}
	}
}