package tictim.ghostutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import static tictim.ghostutils.GhostUtils.MODID;

@Mod(MODID)
public class GhostUtils{
	public static final String MODID = "ghostutils";

	public GhostUtils(){
		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> Cfg::init);
	}

	@Mod.EventBusSubscriber(modid = MODID, bus = Bus.MOD, value = Dist.CLIENT)
	public static final class ClientHandler {
		private ClientHandler(){}

		private static KeyBinding toggleF7;
		private static KeyBinding toggleItemInfo;

		public static KeyBinding getToggleLightOverlay(){
			return toggleF7;
		}
		public static KeyBinding getToggleItemInfo(){
			return toggleItemInfo;
		}

		@SubscribeEvent
		public static void clientInit(FMLClientSetupEvent event){
			toggleF7 = new KeyBinding("key.toggleLightOverlay", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputMappings.getInputByCode(GLFW.GLFW_KEY_F7, 0), "key.categories.misc");
			toggleItemInfo = new KeyBinding("key.toggleItemInfo", KeyConflictContext.GUI, KeyModifier.NONE, InputMappings.getInputByCode(GLFW.GLFW_KEY_F9, 0), "key.categories.misc");
			ClientRegistry.registerKeyBinding(toggleF7);
			ClientRegistry.registerKeyBinding(toggleItemInfo);
			Minecraft.getInstance().execute(ItemInfoHandler::init);
		}
	}
}
