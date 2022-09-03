package tictim.ghostutils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static tictim.ghostutils.GhostUtils.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Side.CLIENT)
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

	private static final MutableBlockPos mpos = new MutableBlockPos();
	private static boolean lightOverlayEnabled;

	@SubscribeEvent
	public static void onTick(TickEvent.ClientTickEvent event){
		if(event.phase==TickEvent.Phase.START){
			if(Cfg.enableLightOverlay()){
				KeyBinding key = GhostUtils.ClientHandler.getToggleLightOverlay();
				if(key.getKeyConflictContext().isActive()&&key.isPressed()) lightOverlayEnabled = !lightOverlayEnabled;
			}else lightOverlayEnabled = false;
		}
	}

	@SubscribeEvent
	public static void onTick(RenderWorldLastEvent event){
		if(!lightOverlayEnabled) return;
		Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
		if(entity==null) return;

		Frustum frustum = new Frustum();
		frustum.setPosition(TileEntityRendererDispatcher.staticPlayerX, TileEntityRendererDispatcher.staticPlayerY, TileEntityRendererDispatcher.staticPlayerZ);


		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GL11.glLineWidth(1.5f);

		GlStateManager.pushMatrix();
		GlStateManager.translate(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY, -TileEntityRendererDispatcher.staticPlayerZ);

		GL11.glBegin(GL11.GL_LINES);

		World world = entity.world;
		int x1 = MathHelper.floor(entity.posX)-X_RADIUS;
		int y1 = Math.max(MathHelper.floor(entity.posY)-Y_LOWER_OFFSET, 0);
		int z1 = MathHelper.floor(entity.posZ)-Z_RADIUS;
		for(int x = x1; x<=x1+X_DIAMETER; x++){
			for(int z = z1; z<=z1+Z_DIAMETER; z++){
				mpos.setPos(x, y1, z);
				for(int y = y1; y<y1+Y_HEIGHT; y++){
					if(world.getHeight()<y) break;
					//if(!frustum.isBoxInFrustum(x, y, z, x+1, y+0.004, z+1)) continue;
					mpos.setY(y);
					switch(getSpawnMode(world)){
						case SPAWN_IN_NIGHT:
							GlStateManager.color(1, 1, 0);
							break;
						case SPAWN:
							GlStateManager.color(1, 0.35f, 0);
							break;
						case ZERO_LIGHT:
							GlStateManager.color(1, 0, 0);
							break;
						default: // case NO_SPAWN:
							continue;
					}
					final float dy = world.getBlockState(mpos)==Blocks.SNOW.getDefaultState() ? y+(0.01f+(2/16f)) : y+0.01f;
					GL11.glVertex3d(x, dy, z);
					GL11.glVertex3d(x+1, dy, z+1);
					GL11.glVertex3d(x+1, dy, z);
					GL11.glVertex3d(x, dy, z+1);
				}
			}
		}

		GL11.glEnd();

		renderLightingPredicate(world);

		GlStateManager.popMatrix();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

	private static int getSpawnMode(World world){
		if(!canCreatureTypeSpawnAtLocation(world, mpos)) return NO_SPAWN;
		int light = world.getLightFor(EnumSkyBlock.BLOCK, mpos);
		if(light>=8) return NO_SPAWN;
		int sky = world.getLightFor(EnumSkyBlock.SKY, mpos);
		if(sky>=8) return SPAWN_IN_NIGHT;
		else return Math.max(light, sky)==0 ? ZERO_LIGHT : SPAWN;
	}

	/**
	 * @see WorldEntitySpawner#isValidEmptySpawnBlock
	 */
	private static boolean canCreatureTypeSpawnAtLocation(World world, BlockPos pos){
		if(!world.getWorldBorder().contains(pos)) return false;
		BlockPos down = pos.down();
		IBlockState downState = world.getBlockState(down);
		if(downState.getBlock().canCreatureSpawn(downState, world, down, SpawnPlacementType.ON_GROUND)){
			Block block = downState.getBlock();
			return block!=Blocks.BEDROCK&&block!=Blocks.BARRIER&&
					WorldEntitySpawner.isValidEmptySpawnBlock(world.getBlockState(pos));
		}
		return false;
	}

	/* @see Minecraft#rightClickMouse */
	private static void renderLightingPredicate(World world){
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if(player==null) return;
		if(Minecraft.getMinecraft().objectMouseOver!=null){
			RayTraceResult trace = Minecraft.getMinecraft().objectMouseOver;
			if(trace.typeOfHit==RayTraceResult.Type.BLOCK){
				IBlockState blockState = world.getBlockState(trace.getBlockPos());
				if(blockState.getMaterial()!=Material.AIR){
					LightValueEstimate light = LightValueEstimate.getBrighter(
							getEstimatedLightValue(player, EnumHand.MAIN_HAND, trace),
							getEstimatedLightValue(player, EnumHand.OFF_HAND, trace));
					if(light!=null){
						List<BlockPos> list = blockFinder.reset(world, light.pos, light.brightness).run();

						if(!list.isEmpty()){
							float alpha = (float)((Math.sin((world.getTotalWorldTime()%40/40.0)*(2*Math.PI))/2+0.5)*0.25+0.25);
							GlStateManager.enableBlend();
							GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
							GlStateManager.color(0, 1, 0, alpha);
							GL11.glBegin(GL11.GL_QUADS);
							for(BlockPos p : list){
								float x = p.getX(), y = world.getBlockState(p)==Blocks.SNOW.getDefaultState() ? p.getY()+(0.005f+2/16f) : p.getY()+(0.005f), z = p.getZ();
								GL11.glVertex3d(x, y, z);
								GL11.glVertex3d(x, y, z+1);
								GL11.glVertex3d(x+1, y, z+1);
								GL11.glVertex3d(x+1, y, z);
							}
							GL11.glEnd();
							GlStateManager.disableBlend();
						}
					}
				}
			}
		}
	}

	@SuppressWarnings({"deprecation", "ConstantConditions"})
	@Nullable
	private static LightValueEstimate getEstimatedLightValue(EntityPlayer player, EnumHand hand, RayTraceResult rayTraceResult){
		ItemStack stack = player.getHeldItem(hand);
		if(stack.isEmpty()||!(stack.getItem() instanceof ItemBlock)) return null;
		ItemBlock item = (ItemBlock)stack.getItem();
		Block block = item.getBlock();
		BlockPos pos = rayTraceResult.getBlockPos();
		EnumFacing side = rayTraceResult.sideHit;
		if(block!=null&&item.canPlaceBlockOnSide(player.world, pos, side, player, stack)){
			if(block==Blocks.SNOW_LAYER&&block.isReplaceable(player.world, pos)) side = EnumFacing.UP;
			else if(!block.isReplaceable(player.world, pos)) pos = pos.offset(side);
			IBlockState state = block.getStateForPlacement(player.world, pos, side,
					(float)rayTraceResult.hitVec.x, (float)rayTraceResult.hitVec.y, (float)rayTraceResult.hitVec.z,
					stack.getMetadata(), player, hand);
			int lightValue = state.getLightValue();
			return lightValue>7 ? new LightValueEstimate(pos, lightValue) : null;
		}else return null;
	}

	private static final class LightValueEstimate{
		public final BlockPos pos;
		public final int brightness;

		private LightValueEstimate(BlockPos pos, int brightness){
			this.pos = pos;
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
			MutableBlockPos mpos = new MutableBlockPos();
			while(!searchQueue.isEmpty()){
				BlockPos pos = searchQueue.remove();
				int lightValue = blockLightMap.get(pos);
				IBlockState state = world.getBlockState(pos);
				if(lightValue>=8){
					int opacity = 1+state.getLightValue(world, pos);
					if(lightValue-opacity>=7){
						int c = lightValue-opacity;
						// Add nearby BlockPos to queue
						for(EnumFacing d : EnumFacing.values()) queue(mpos.setPos(pos).move(d), c);
					}
				}
				if(canCreatureTypeSpawnAtLocation(world, pos)) list.add(pos);
			}
			return list;
		}

		private void queue(BlockPos pos, int blockLight){
			pos = pos.toImmutable();
			if(blockLightMap.containsKey(pos)){
				if(this.blockLightMap.get(pos)<blockLight) this.blockLightMap.put(pos, blockLight);
			}else{
				this.blockLightMap.put(pos, blockLight);
				if(blockLight>7) this.searchQueue.add(pos);
			}
		}
	}
}