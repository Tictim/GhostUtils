package tictim.ghostutils;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class Cfg{
	private Cfg(){}

	private static ForgeConfigSpec.BooleanValue enableLightOverlay;
	private static ForgeConfigSpec.BooleanValue enableItemInfo;
	private static ForgeConfigSpec.DoubleValue itemInfoZoom;
	private static ForgeConfigSpec.DoubleValue itemInfoZoomInSneak;

	public static boolean enableLightOverlay(){
		return enableLightOverlay.get();
	}
	public static boolean enableItemInfo(){
		return enableItemInfo.get();
	}
	public static double itemInfoZoom(){
		return itemInfoZoom.get();
	}
	public static double itemInfoZoomInSneak(){
		return itemInfoZoomInSneak.get();
	}

	public static void init(){
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.push("LightOverlay");
		enableLightOverlay = builder.define("enableLightOverlay", true);
		builder.pop().push("ItemInfo");
		enableItemInfo = builder.define("enableItemInfo", true);
		itemInfoZoom = builder.defineInRange("itemInfoZoom", 1.0, Double.MIN_VALUE, Double.MAX_VALUE);
		itemInfoZoomInSneak = builder.defineInRange("itemInfoZoomInSneak", 2.0, Double.MIN_VALUE, Double.MAX_VALUE);
		builder.pop();
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, builder.build(), "ghostutils.toml");
	}
}