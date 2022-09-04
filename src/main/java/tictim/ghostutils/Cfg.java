package tictim.ghostutils;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class Cfg{
	private Cfg(){}

	private static ForgeConfigSpec.BooleanValue enableLightOverlay;
	private static ForgeConfigSpec.BooleanValue enableItemInfo;
	private static ForgeConfigSpec.DoubleValue itemInfoZoom;
	private static ForgeConfigSpec.DoubleValue itemInfoZoomOnSneak;
	private static ForgeConfigSpec.BooleanValue itemInfoTest;

	public static boolean enableLightOverlay(){
		return enableLightOverlay.get();
	}
	public static boolean enableItemInfo(){
		return enableItemInfo.get();
	}
	public static double itemInfoZoom(){
		return itemInfoZoom.get();
	}
	public static double itemInfoZoomOnSneak(){
		return itemInfoZoomOnSneak.get();
	}
	public static boolean itemInfoTest(){
		return itemInfoTest.get();
	}

	public static void init(){
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.push("LightOverlay");
		enableLightOverlay = builder.define("enable", true);
		builder.pop().push("ItemInfo");
		enableItemInfo = builder.define("enable", true);
		itemInfoZoom = builder.defineInRange("zoom", 1.0, Double.MIN_VALUE, Double.MAX_VALUE);
		itemInfoZoomOnSneak = builder.defineInRange("zoomOnSneak", 2.0, Double.MIN_VALUE, Double.MAX_VALUE);
		itemInfoTest = builder.define("test", true);
		builder.pop();
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, builder.build(), "ghostutils.toml");
	}
}
