package tictim.ghostutils;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.util.text.TextFormatting.*;
import static tictim.ghostutils.GhostUtils.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Side.CLIENT)
public final class ItemInfoHandler{
	private ItemInfoHandler(){}

	private static ItemStack stackUsedForTooltip = ItemStack.EMPTY;
	private static boolean itemInfoEnabled;

	@SubscribeEvent
	public static void beforeKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Post event){
		if(Cfg.enableItemInfo()){
			if(Keyboard.getEventKeyState()){
				KeyBinding itemInfo = GhostUtils.ClientHandler.getToggleItemInfo();
				if(itemInfo.isActiveAndMatches(Keyboard.getEventKey())){
					itemInfoEnabled = !itemInfoEnabled;
					event.setCanceled(true);
				}
			}
		}else itemInfoEnabled = false;
	}

	@SubscribeEvent
	public static void onDrawTooltip(RenderTooltipEvent.PostText event){
		if(itemInfoEnabled) stackUsedForTooltip = event.getStack();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void guiRender(GuiScreenEvent.DrawScreenEvent.Post event){
		if(!itemInfoEnabled||!(event.getGui() instanceof GuiContainer)) return;
		GuiContainer gui = (GuiContainer)event.getGui();
		if(Cfg.itemInfoTest()){
			draw(gui, getDebugText(), event.getMouseY());
		}else if(gui.mc.player!=null){
			ItemStack stack = gui.mc.player.inventory.getItemStack();
			if(stack.isEmpty()){
				Slot slotSelected = gui.getSlotUnderMouse();
				if(slotSelected!=null) stack = slotSelected.getStack();
				else if(!stackUsedForTooltip.isEmpty()) stack = stackUsedForTooltip;

				if(!stack.isEmpty()) draw(gui, getString(stack), 0);
			}else draw(gui, getString(stack), event.getMouseY());
		}
		stackUsedForTooltip = ItemStack.EMPTY;
	}

	private static void draw(GuiContainer gui, String text, int scroll){
		Minecraft mc = Minecraft.getMinecraft();
		FontRenderer font = mc.fontRenderer;
		ScaledResolution window = new ScaledResolution(mc);

		GlStateManager.disableDepth();
		GlStateManager.disableBlend();
		GlStateManager.disableLighting();
		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 0, 1);
		GuiContainer.isShiftKeyDown();
		double mag = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()) ?
				Cfg.itemInfoZoomOnSneak() :
				Cfg.itemInfoZoom();
		GlStateManager.scale(mag/window.getScaleFactor(), mag/window.getScaleFactor(), 1);
		GlStateManager.color(1, 1, 1, 1);

		boolean fu = mc.gameSettings.forceUnicodeFont;
		if(fu) font.setUnicodeFlag(false);

		List<String> split = font.listFormattedStringToWidth(text, window.getScaledWidth()/3);
		GlStateManager.translate(0, font.FONT_HEIGHT-maxScroll(window, font, split.size(), mag)*scroll/(double)gui.height, 400);

		int width = 0;
		for(String s : split) width = Math.max(width, font.getStringWidth(s));
		GuiUtils.drawGradientRect(0, 0, 2, 4+width, 2+(split.size()+1)*font.FONT_HEIGHT, 0x80000000, 0x80000000);

		for(int i = 0; i<split.size(); i++){
			font.drawString(split.get(i), 2, 2+i*font.FONT_HEIGHT, -1);
		}

		if(fu) font.setUnicodeFlag(true);

		GlStateManager.popMatrix();
		GlStateManager.enableDepth();
		GlStateManager.enableBlend();
		GlStateManager.enableLighting();
	}

	private static int maxScroll(ScaledResolution window, FontRenderer font, int lines, double mag){
		return Math.max(0, (lines+2)*font.FONT_HEIGHT-(int)(window.getScaledHeight_double()/mag));
	}

	private static ItemStack latestStack;
	private static String latestText;

	private static String getString(ItemStack stack){
		if(latestStack==null||stack!=latestStack){
			latestStack = stack.copy();
			Item item = stack.getItem();
			Block block = item instanceof ItemBlock ? ((ItemBlock)item).getBlock() : null;

			TextWriter text = new TextWriter();

			// Item name and its stack size
			text.write(GOLD).write(stack.getCount()).rst().write(" x ").write(stack.getDisplayName());
			// Item/Block ID
			text.nl().write(GRAY).write("Item ID: ").write(item.getRegistryName()).rst();
			if(block!=null)
				text.nl().write(GRAY).write("Block ID: ").write(block.getRegistryName()).rst();

			if(stack.isItemStackDamageable()){
				int maxDamage = stack.getMaxDamage(), damage = stack.getItemDamage();
				double percentage = (double)(maxDamage-damage)/maxDamage;
				text.nl().nl().write(percentage>=.5 ? GREEN :
								percentage>=.25 ? YELLOW : percentage>=.125 ? GOLD : RED)
						.write(BOLD).write(maxDamage-damage).rst()
						.write(" / ").write(maxDamage)
						.write(" (")
						.write(GOLD).write(percentage<0.01 ? "<1%" : (int)(percentage*100)+"%").rst()
						.write(")");
			}

			int[] oreIDs = OreDictionary.getOreIDs(stack);
			if(oreIDs.length>0){
				text.nl().nl().write(YELLOW).write(BOLD).write("Ore Dictionary:").rst();
				Arrays.stream(oreIDs)
						.mapToObj(OreDictionary::getOreName)
						.sorted()
						.forEach(s -> text.nl().write(" - ").write(s));
			}
			NBTTagCompound nbt = stack.getTagCompound();
			if(nbt!=null){
				text.nl().nl().write(GREEN).write(BOLD).write("NBT: ").rst();
				nbtToText(text, nbt);
			}
			latestText = text.toString();
		}
		return latestText;
	}

	// catch-all for most of the potential ResourceLocation use cases, since there's no set rules for ResourceLocation pattern yet
	private static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile("([a-zA-Z0-9_/.-]+):([a-zA-Z0-9_/.-]+)");

	private static void nbtToText(TextWriter text, NBTBase nbt){
		switch(nbt.getId()){
			case NBT.TAG_END:
				text.write(DARK_GRAY).write("(END)").rst();
				return;
			case NBT.TAG_BYTE:
				text.write(((NBTTagByte)nbt).getByte()!=0 ? GREEN : RED).write(nbt.toString()).rst();
				return;
			case NBT.TAG_SHORT:
			case NBT.TAG_INT:
			case NBT.TAG_LONG:
			case NBT.TAG_FLOAT:
			case NBT.TAG_DOUBLE:
				text.write(GOLD).write(nbt.toString()).rst();
				return;
			case NBT.TAG_BYTE_ARRAY:{
				byte[] byteArray = ((NBTTagByteArray)nbt).getByteArray();
				if(byteArray.length==0){
					text.write("[B; ]");
					return;
				}
				text.write("[B;").tab();
				boolean first = true;
				for(byte b : byteArray){
					if(first){
						first = false;
						text.nl();
					}else text.write(", ");
					text.write(b!=0 ? GREEN : RED).write(b).rst();
				}
				text.untab().writeAtNewLine("]");
				return;
			}
			case NBT.TAG_STRING:{
				String str = nbt.toString();
				Matcher matcher = RESOURCE_LOCATION_PATTERN.matcher(str);
				if(matcher.matches()){
					text.write(GREEN).write('"')
							.write(YELLOW).write(matcher.group(1))
							.write(GREEN).write(':')
							.rst().write(matcher.group(2))
							.write(GREEN).write('"').rst();
				}else{
					text.write(GREEN).write('"').write(str).write('"').rst();
				}
				return;
			}
			case NBT.TAG_LIST:
				if(nbt.isEmpty()){
					text.write("[NBT; ]");
					return;
				}
				text.write("[NBT;").tab();
				for(NBTBase nbt2 : (NBTTagList)nbt) nbtToText(text.nl(), nbt2);
				text.untab().writeAtNewLine("]");
				return;
			case NBT.TAG_COMPOUND:{
				NBTTagCompound compound = (NBTTagCompound)nbt;
				if(compound.isEmpty()){
					text.write("{ }");
					return;
				}
				text.write("{").tab();
				compound.getKeySet().stream().sorted().forEachOrdered(key -> {
					text.writeAtNewLine(YELLOW).write(key).rst().write(": ");
					nbtToText(text, Objects.requireNonNull(compound.getTag(key)));
				});
				text.untab().writeAtNewLine("}");
				return;
			}
			case NBT.TAG_INT_ARRAY:{
				int[] intArray = ((NBTTagIntArray)nbt).getIntArray();
				if(intArray.length==0){
					text.write("[I; ]");
					return;
				}
				text.write("[I;").tab();
				boolean first = true;
				for(int i : intArray){
					if(first){
						first = false;
						text.nl();
					}else text.write(", ");
					text.write(GOLD).write(i).rst();
				}
				text.untab().writeAtNewLine("]");
				return;
			}
			case NBT.TAG_LONG_ARRAY:
				long[] longArray = ((NBTTagLongArray)nbt).data;
				if(longArray.length==0){
					text.write("[L; ]");
					return;
				}
				text.write("[L;").tab();
				boolean first = true;
				for(long l : longArray){
					if(first){
						first = false;
						text.nl();
					}else text.write(", ");
					text.write(GOLD).write(l).rst();
				}
				text.untab().writeAtNewLine("]");
				return;
			default:
				text.write("(Unknown NBT Data)");
		}
	}

	@Nullable private static String debugText;

	private static String getDebugText(){
		if(debugText==null){
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("IntValue", 420);
			tag.setBoolean("BoolValue", true);
			tag.setByte("ByteValue", (byte)127);
			tag.setShort("ShortValue", (short)6300);
			tag.setLong("LongValue", 12352346725L);
			tag.setFloat("FloatValue", 2.5f);
			tag.setDouble("DoubleValue", Math.PI);
			tag.setByteArray("ByteArrayValue", new byte[]{0, 1, 2, 3, 4, 5});
			tag.setIntArray("IntArrayValue", new int[]{0, 1, 2, 3, 4, 5});
			tag.setTag("LongArrayValue", new NBTTagLongArray(new long[]{0, 1, 2, 3, 4, 5}));
			tag.setString("StringValue", "Hello petty mortals");
			NBTTagList list = new NBTTagList();
			list.appendTag(new NBTTagString("List Element 1"));
			list.appendTag(new NBTTagString("List Element 2"));
			list.appendTag(new NBTTagString("List Element 3"));
			tag.setTag("StringListValue", list);
			NBTTagList list2 = new NBTTagList();
			NBTTagCompound nbt1 = new NBTTagCompound();
			nbt1.setInteger("ListElementIndex", 0);
			list2.appendTag(nbt1);
			NBTTagCompound nbt2 = new NBTTagCompound();
			nbt2.setInteger("ListElementIndex", 1);
			list2.appendTag(nbt2);
			NBTTagCompound nbt3 = new NBTTagCompound();
			nbt3.setInteger("ListElementIndex", 2);
			list2.appendTag(nbt3);
			tag.setTag("CompoundListValue", list2);
			tag.setUniqueId("UUID", UUID.randomUUID());
			NBTTagCompound NBTTaginner = new NBTTagCompound();
			NBTTaginner.setInteger("InnerIntValue", 1000);
			NBTTaginner.setString("InnerStringValue", "I am god of the universe");
			NBTTagList innerList = new NBTTagList();
			innerList.appendTag(new NBTTagString("List Element 1"));
			innerList.appendTag(new NBTTagString("List Element 2"));
			innerList.appendTag(new NBTTagString("List Element 3"));
			NBTTaginner.setTag("InnerListValue", innerList);
			tag.setTag("NBTTagInner", NBTTaginner);
			tag.setByteArray("EmptyByteArrayValue", new byte[0]);
			tag.setIntArray("EmptyIntArrayValue", new int[0]);
			tag.setTag("EmptyLongArrayValue", new NBTTagLongArray(new long[0]));
			tag.setTag("NBTTagEmptyArrayValue", new NBTTagList());
			tag.setString("ReallyReallyReallyReallyLongStringOfStringsAreReallyReallyReallyReallyLong",
					"ReallyReallyReallyReallyLongStringOfStringsAreReallyReallyReallyReallyLong");

			TextWriter text = new TextWriter();
			nbtToText(text, tag);
			debugText = text.toString();
		}
		return debugText;
	}
}
