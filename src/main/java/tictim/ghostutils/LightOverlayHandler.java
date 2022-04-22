package tictim.ghostutils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelLastEvent;
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

	private static final BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
	private static boolean lightOverlayEnabled;

	@SubscribeEvent
	public static void onTick(TickEvent.ClientTickEvent event){
		if(event.phase==TickEvent.Phase.START){
			if(Cfg.enableLightOverlay()){
				KeyMapping key = GhostUtils.ClientHandler.getToggleLightOverlay();
				if(key.getKeyConflictContext().isActive()&&key.consumeClick()) lightOverlayEnabled = !lightOverlayEnabled;
			}else lightOverlayEnabled = false;
		}
	}

	@SubscribeEvent
	public static void onTick(RenderLevelLastEvent event){
		if(!lightOverlayEnabled) return;
		Entity entity = Minecraft.getInstance().getCameraEntity();
		if(entity==null) return;
		RenderSystem.disableTexture();
		GL11.glLineWidth(1.5f);

		Vec3 projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);
		VertexConsumer buffer = buffers.getBuffer(GhostUtilsRenderType.GHOSTUTILS_LINES);
		Matrix4f matrix = poseStack.last().pose();

		Level level = entity.getLevel();
		int x1 = Mth.floor(entity.getX())-X_RADIUS;
		int y1 = Math.max(Mth.floor(entity.getY())-Y_LOWER_OFFSET, 0);
		int z1 = Mth.floor(entity.getZ())-Z_RADIUS;
		for(int x = x1; x<=x1+X_DIAMETER; x++){
			for(int z = z1; z<=z1+Z_DIAMETER; z++){
				mpos.set(x, y1, z);
				for(int y = y1; y<y1+Y_HEIGHT; y++){
					if(level.getMaxBuildHeight()<y) break;
					//if(!frustum.isBoxInFrustum(x, y, z, x+1, y+0.004, z+1)) continue;
					mpos.setY(y);
					float r, g, b;
					switch(getSpawnMode(level)){
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
					final float dy = level.getBlockState(mpos)==Blocks.SNOW.defaultBlockState() ? y+(0.01f+(2/16f)) : y+0.01f;
					buffer.vertex(matrix, x, dy, z).color(r, g, b, 1).endVertex();
					buffer.vertex(matrix, x+1, dy, z+1).color(r, g, b, 1).endVertex();
					buffer.vertex(matrix, x+1, dy, z).color(r, g, b, 1).endVertex();
					buffer.vertex(matrix, x, dy, z+1).color(r, g, b, 1).endVertex();
				}
			}
		}

		buffers.endLastBatch();

		renderLightingPredicate(level, poseStack, buffers);

		poseStack.popPose();
		RenderSystem.enableTexture();
	}

	private static int getSpawnMode(Level level){
		if(!canCreatureTypeSpawnAtLocation(level, mpos)) return NO_SPAWN;
		int light = level.getBrightness(LightLayer.BLOCK, mpos);
		if(light>=8) return NO_SPAWN;
		int sky = level.getBrightness(LightLayer.SKY, mpos);
		if(sky>=8) return SPAWN_IN_NIGHT;
		else return Math.max(light, sky)==0 ? ZERO_LIGHT : SPAWN;
	}

	/**
	 * @see NaturalSpawner#isValidEmptySpawnBlock
	 */
	private static boolean canCreatureTypeSpawnAtLocation(Level level, BlockPos pos){
		if(!level.getWorldBorder().isWithinBounds(pos)) return false;
		BlockPos down = pos.below();
		if(level.getBlockState(down).isValidSpawn(level, down, SpawnPlacements.Type.ON_GROUND, EntityType.ZOMBIE)) {
			return NaturalSpawner.isValidEmptySpawnBlock(level, pos, level.getBlockState(pos), level.getFluidState(pos), EntityType.ZOMBIE);
		}
		return false;
	}

	/* @see Minecraft#rightClickMouse */
	private static void renderLightingPredicate(Level level, PoseStack stack, MultiBufferSource.BufferSource buffers){
		LocalPlayer player = Minecraft.getInstance().player;
		if(player==null) return;
		if(!player.isVehicle()&&Minecraft.getInstance().hitResult!=null){//isboat?
			HitResult _t = Minecraft.getInstance().hitResult;
			if(_t instanceof BlockHitResult trace){
				if(trace.getType()==HitResult.Type.BLOCK){
					BlockState blockState = level.getBlockState(trace.getBlockPos());
					if(blockState.getMaterial()!=Material.AIR){// TODO BlockItemUseContext
						LightValueEstimate light = LightValueEstimate.getBrighter(
								getEstimatedLightValue(player, InteractionHand.MAIN_HAND, trace),
								getEstimatedLightValue(player, InteractionHand.OFF_HAND, trace));
						if(light!=null){
							List<BlockPos> list = blockFinder.reset(level, light.ctx.getClickedPos(), light.brightness).run();

							float alpha = (float)((Math.sin((level.getGameTime()%40/40.0)*(2*Math.PI))/2+0.5)*0.25+0.25);
							if(!list.isEmpty()){
								Matrix4f matrix = stack.last().pose();
								VertexConsumer buffer = buffers.getBuffer(GhostUtilsRenderType.GHOSTUTILS_QUADS);
								for(BlockPos p : list){
									float x = p.getX(), y = level.getBlockState(p)==Blocks.SNOW.defaultBlockState() ? p.getY()+(0.005f+2/16f) : p.getY()+(0.005f), z = p.getZ();
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
	private static LightValueEstimate getEstimatedLightValue(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult){
		ItemStack stack = player.getItemInHand(hand);
		if(stack.isEmpty()||!(stack.getItem() instanceof BlockItem bi)) return null;
		//noinspection ConstantConditions
		if(bi.getBlock()!=null){
			BlockPlaceContext ctx = new BlockPlaceContext(new UseOnContext(player, hand, hitResult));
			if(ctx.canPlace()){
				BlockState state = bi.getBlock().getStateForPlacement(ctx);
				if(state!=null&&!state.hasBlockEntity()){
					int lightValue = state.getLightEmission();
					return lightValue>7 ? new LightValueEstimate(ctx, state.getLightEmission()) : null;
				}
			}
		}
		return null;
	}

	private static final class LightValueEstimate{
		public final BlockPlaceContext ctx;
		public final int brightness;

		public LightValueEstimate(BlockPlaceContext ctx, int brightness){
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
		private Level level;

		public LightViewFinder reset(Level level, BlockPos pos, int initialBlockLight){
			this.searchQueue.clear();
			this.blockLightMap.clear();
			this.level = level;
			if(initialBlockLight>=7) queue(pos, initialBlockLight);
			return this;
		}

		public List<BlockPos> run(){
			List<BlockPos> list = new ArrayList<>();
			BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
			while(!searchQueue.isEmpty()){
				BlockPos pos = searchQueue.remove();
				int lightValue = blockLightMap.get(pos);
				BlockState state = level.getBlockState(pos);
				if(lightValue>=8){
					int opacity = 1+state.getLightEmission(level, pos);
					if(lightValue-opacity>=7){
						int c = lightValue-opacity;
						// Add nearby BlockPos to queue
						for(Direction d : Direction.values()) queue(mpos.set(pos).move(d), c);
					}
				}
				if(canCreatureTypeSpawnAtLocation(level, pos)) list.add(pos);
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
