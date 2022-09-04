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
		return LightOverlay.enable;
	}
	public static boolean enableItemInfo(){
		return ItemInfo.enable;
	}
	public static double itemInfoZoom(){
		return ItemInfo.zoom;
	}
	public static double itemInfoZoomOnSneak(){
		return ItemInfo.zoomOnSneak;
	}
	public static boolean itemInfoTest(){
		return ItemInfo.test;
	}

	public static class LightOverlay{
		public boolean enable = true;
	}

	public static class ItemInfo{
		public boolean enable = true;
		public double zoom = 1;
		public double zoomOnSneak = 2;
		public boolean test = false;
	}
}
