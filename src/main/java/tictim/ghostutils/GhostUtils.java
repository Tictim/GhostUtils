package tictim.ghostutils;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

@Mod(modid = GhostUtils.MODID,
		name = GhostUtils.NAME,
		version = GhostUtils.VERSION,
		guiFactory = "tictim.ghostutils.GuiFactory",
		clientSideOnly = true)
public class GhostUtils{
	public static final String MODID = "ghostutils";
	public static final String NAME = "Ghost Utilities";
	public static final String VERSION = "1.0.3.0";

	@Mod.EventHandler
	public void init(FMLInitializationEvent event){
		if(FMLCommonHandler.instance().getSide()==Side.CLIENT){
			ClientHandler.init();
		}
	}

	public static class ClientHandler{
		private static KeyBinding toggleF7;
		private static KeyBinding toggleItemInfo;

		public static KeyBinding getToggleLightOverlay(){
			return toggleF7;
		}
		public static KeyBinding getToggleItemInfo(){
			return toggleItemInfo;
		}

		public static void init(){
			toggleF7 = new KeyBinding("key.toggleLightOverlay", KeyConflictContext.IN_GAME, KeyModifier.NONE, Keyboard.KEY_F7, "key.categories.misc");
			toggleItemInfo = new KeyBinding("key.toggleItemInfo", KeyConflictContext.GUI, KeyModifier.NONE, Keyboard.KEY_F9, "key.categories.misc");
			ClientRegistry.registerKeyBinding(toggleF7);
			ClientRegistry.registerKeyBinding(toggleItemInfo);
		}
	}
}
