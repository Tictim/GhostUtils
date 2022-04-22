package tictim.ghostutils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import static tictim.ghostutils.GhostUtils.MODID;

@Mod(MODID)
public class GhostUtils{
	public static final String MODID = "ghostutils";

	public GhostUtils() {
		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> Cfg::init);
	}

	@Mod.EventBusSubscriber(modid = MODID, bus = Bus.MOD, value = Dist.CLIENT)
	public static final class ClientHandler{
		private ClientHandler(){}

		private static KeyMapping toggleF7;
		private static KeyMapping toggleItemInfo;

		public static KeyMapping getToggleLightOverlay(){
			return toggleF7;
		}
		public static KeyMapping getToggleItemInfo(){
			return toggleItemInfo;
		}

		@SubscribeEvent
		public static void clientInit(FMLClientSetupEvent event){
			toggleF7 = new KeyMapping("key.toggleLightOverlay", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.getKey(GLFW.GLFW_KEY_F7, 0), "key.categories.misc");
			toggleItemInfo = new KeyMapping("key.toggleItemInfo", KeyConflictContext.GUI, KeyModifier.NONE, InputConstants.getKey(GLFW.GLFW_KEY_F9, 0), "key.categories.misc");
			ClientRegistry.registerKeyBinding(toggleF7);
			ClientRegistry.registerKeyBinding(toggleItemInfo);
		}
	}
}
