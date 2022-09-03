package tictim.ghostutils;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static tictim.ghostutils.GhostUtils.MODID;

@Mod.EventBusSubscriber(modid = MODID)
@Config(modid = MODID, category = "")
public class Cfg{
	@SubscribeEvent
	@SuppressWarnings("unused")
	public static void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent event){
		if(event.getModID().equals(MODID))
			ConfigManager.sync(MODID, Config.Type.INSTANCE);
	}
	public static final LightOverlay LightOverlay = new LightOverlay();
	public static final ItemInfo ItemInfo = new ItemInfo();

	public static boolean enableLightOverlay(){
		return LightOverlay.enableLightOverlay;
	}
	public static boolean enableItemInfo(){
		return ItemInfo.enableItemInfo;
	}
	public static double itemInfoZoom(){
		return ItemInfo.itemInfoZoom;
	}
	public static double itemInfoZoomInSneak(){
		return ItemInfo.itemInfoZoomInSneak;
	}

	public static class LightOverlay{
		public boolean enableLightOverlay = true;
	}

	public static class ItemInfo{
		public boolean enableItemInfo = true;
		public double itemInfoZoom = 1;
		public double itemInfoZoomInSneak = 2;
	}
}
