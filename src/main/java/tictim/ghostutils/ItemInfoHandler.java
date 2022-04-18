package tictim.ghostutils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.gui.GuiUtils;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static net.minecraft.util.text.TextFormatting.*;
import static tictim.ghostutils.GhostUtils.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class ItemInfoHandler{
	private ItemInfoHandler(){}

	private static FontRenderer fontRenderer;

	static void init(){
		fontRenderer = Minecraft.getInstance().getFontResourceManager().getFontRenderer(new ResourceLocation(MODID, "nonunicode"));
	}

	private static ItemStack stackUsedForTooltip = ItemStack.EMPTY;
	private static boolean itemInfoEnabled;

	@SubscribeEvent
	public static void onTick(TickEvent.ClientTickEvent event){
		if(event.phase==TickEvent.Phase.START){
			if(Cfg.enableItemInfo()){
				KeyBinding key = GhostUtils.ClientHandler.getToggleItemInfo();
				if(key.getKeyConflictContext().isActive()&&key.isPressed()) itemInfoEnabled = !itemInfoEnabled;
			}else itemInfoEnabled = false;
		}
	}

	@SubscribeEvent
	public static void onDrawTooltip(RenderTooltipEvent.PostText event){
		if(itemInfoEnabled) stackUsedForTooltip = event.getStack();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void guiRender(GuiScreenEvent.DrawScreenEvent.Post event){
		if(!itemInfoEnabled||!(event.getGui() instanceof ContainerScreen<?>)) return;
		ContainerScreen<?> gui = (ContainerScreen<?>)event.getGui();

		if(debugMode()){
			TextWriter text = new TextWriter();
			nbtToText(text, getDebugNbt());
			draw(gui, text.toString(), event.getMouseY());
		}else if(gui.getMinecraft().player!=null){
			ItemStack stack = gui.getMinecraft().player.inventory.getItemStack();
			if(stack.isEmpty()){
				Slot slotSelected = gui.getSlotUnderMouse();
				if(slotSelected!=null) stack = slotSelected.getStack();
				else if(!stackUsedForTooltip.isEmpty()) stack = stackUsedForTooltip;

				if(!stack.isEmpty()) draw(gui, getString(stack), 0);
			}else draw(gui, getString(stack), event.getMouseY());
		}
		stackUsedForTooltip = ItemStack.EMPTY;
	}

	private static void draw(ContainerScreen<?> gui, String text, int scroll){
		Minecraft mc = Minecraft.getInstance();
		RenderSystem.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
		RenderSystem.disableLighting();
		RenderSystem.disableDepthTest();
		RenderSystem.pushMatrix();

		MainWindow window = mc.getMainWindow();
		double mag = InputMappings.isKeyDown(window.getHandle(), mc.gameSettings.keyBindSneak.getKey().getKeyCode()) ?
				Cfg.itemInfoZoomInSneak() :
				Cfg.itemInfoZoom();
		RenderSystem.scaled((double)window.getScaledWidth()/window.getWidth()*mag, (double)window.getScaledHeight()/window.getHeight()*mag, 1);
		RenderSystem.color4f(1, 1, 1, 1);

		List<String> list = Minecraft.getInstance().fontRenderer.listFormattedStringToWidth(text, window.getWidth()/3);
		GL11.glTranslated(0, fontRenderer.FONT_HEIGHT-maxScroll(window, list.size(), mag)*scroll/(double)gui.height, 0);

		int width = 0;
		for(String s : list) if(!s.isEmpty()) width = Math.max(width, fontRenderer.getStringWidth(s));
		GuiUtils.drawGradientRect(0, 0, 2, 4+width, 2+(list.size()+1)*fontRenderer.FONT_HEIGHT, 0x80000000, 0x80000000);

		for(int i = 0; i<list.size(); i++) fontRenderer.drawStringWithShadow(list.get(i), 2, 2+i*fontRenderer.FONT_HEIGHT, -1);

		RenderSystem.enableRescaleNormal();
		RenderSystem.popMatrix();
		RenderSystem.enableLighting();
		RenderSystem.enableDepthTest();
		RenderHelper.enableStandardItemLighting();
	}

	private static int maxScroll(MainWindow window, int lines, double mag){
		return Math.max(0, (lines+2)*fontRenderer.FONT_HEIGHT-(int)((double)window.getHeight()/mag));
	}

	private static ItemStack latestStack;
	private static String latestText;

	private static String getString(ItemStack stack){
		//noinspection PointlessNullCheck
		if(latestStack==null||!ItemStack.areItemStacksEqual(stack, latestStack)){
			latestStack = stack.copy();
			Item item = stack.getItem();

			TextWriter text = new TextWriter();

			// Item name and its stack size
			text.write(GOLD).write(stack.getCount()).rst().write(" x ").write(stack.getDisplayName().getFormattedText());
			// Item/Block ID
			text.nl().write(GRAY).write("Item ID: ").write(item.getRegistryName()).rst();
			if(item instanceof BlockItem)
				text.nl().write(GRAY).write("Block ID: ").write(((BlockItem)item).getBlock().getRegistryName()).rst();

			if(stack.isDamageable()){
				int maxDamage = stack.getMaxDamage(), damage = stack.getDamage();
				double percentage = (double)(maxDamage-damage)/maxDamage;
				text.nl().nl().write(percentage>=.5 ? GREEN :
								percentage>=.25 ? YELLOW : percentage>=.125 ? GOLD : RED)
						.write(BOLD).write(maxDamage-damage).rst()
						.write(" / ").write(maxDamage)
						.write(" (")
						.write(GOLD).write(percentage<0.01 ? "<1%" : (int)(percentage*100)+"%").rst()
						.write(")");
			}

			List<Tag<Item>> itemTags = tags(ItemTags.getCollection(), item);
			if(!itemTags.isEmpty()){
				text.nl().nl().write(YELLOW).write(BOLD).write("Item Tags:").rst();
				for(Tag<Item> tag : itemTags) text.nl().write(" - ").write(tag.getId());
			}
			if(item instanceof BlockItem){
				List<Tag<Block>> blockTags = tags(BlockTags.getCollection(), ((BlockItem)item).getBlock());
				if(!blockTags.isEmpty()){
					text.nl().write(YELLOW).write(BOLD).write("Block Tags:").rst();
					for(Tag<Block> tag : blockTags) text.nl().write(" - ").write(tag.getId());
				}
			}
			CompoundNBT nbt = stack.getTag();
			if(nbt!=null){
				text.nl().nl().write(GREEN).write(BOLD).write("NBT: ").rst();
				nbtToText(text, nbt);
			}
			latestText = text.toString();
		}
		return latestText;
	}

	private static <T> List<Tag<T>> tags(TagCollection<T> tags, T t){
		List<Tag<T>> returns = null;
		for(Tag<T> tag : tags.getTagMap().values()){
			if(tag.contains(t)){
				if(returns==null) returns = new ArrayList<>();
				returns.add(tag);
			}
		}
		return returns!=null ? returns : Collections.emptyList();
	}

	private static void nbtToText(TextWriter text, INBT nbt){
		switch(nbt.getId()){
			case NBT.TAG_END:
				text.write(DARK_GRAY).write("(END)").rst();
				return;
			case NBT.TAG_BYTE:
				text.write(((ByteNBT)nbt).getByte()!=0 ? GREEN : RED).write(nbt.toString()).rst();
				return;
			case NBT.TAG_SHORT:
			case NBT.TAG_INT:
			case NBT.TAG_LONG:
			case NBT.TAG_FLOAT:
			case NBT.TAG_DOUBLE:
				text.write(GOLD).write(nbt.toString()).rst();
				return;
			case NBT.TAG_BYTE_ARRAY:{
				if(((ByteArrayNBT)nbt).isEmpty()){
					text.write("[B; ]");
					return;
				}
				text.write("[B;").tab();
				boolean first = true;
				for(byte b : ((ByteArrayNBT)nbt).getByteArray()){
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
				String str = nbt.getString();
				ResourceLocation rl = ResourceLocation.tryCreate(str);
				if(rl!=null){
					text.write(GREEN).write('"')
							.write(YELLOW).write(rl.getNamespace())
							.write(GREEN).write(':')
							.rst().write(rl.getPath())
							.write(GREEN).write('"').rst();
				}else{
					text.write(GREEN).write('"').write(str).write('"').rst();
				}
				return;
			}
			case NBT.TAG_LIST:
				if(((ListNBT)nbt).isEmpty()){
					text.write("[NBT; ]");
					return;
				}
				text.write("[NBT;").tab();
				for(INBT nbt2 : (ListNBT)nbt) nbtToText(text.nl(), nbt2);
				text.untab().writeAtNewLine("]");
				return;
			case NBT.TAG_COMPOUND:{
				CompoundNBT compound = (CompoundNBT)nbt;
				if(compound.isEmpty()){
					text.write("{ }");
					return;
				}
				text.write("{").tab();
				compound.keySet().stream().sorted().forEachOrdered(key -> {
					text.writeAtNewLine(YELLOW).write(key).rst().write(": ");
					nbtToText(text, Objects.requireNonNull(compound.get(key)));
				});
				text.untab().writeAtNewLine("}");
				return;
			}
			case NBT.TAG_INT_ARRAY:{
				if(((IntArrayNBT)nbt).isEmpty()){
					text.write("[I; ]");
					return;
				}
				text.write("[I;").tab();
				boolean first = true;
				for(int i : ((IntArrayNBT)nbt).getIntArray()){
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
				if(((LongArrayNBT)nbt).isEmpty()){
					text.write("[L; ]");
					return;
				}
				text.write("[L;").tab();
				boolean first = true;
				for(long l : ((LongArrayNBT)nbt).getAsLongArray()){
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

	private static boolean debugMode(){
		return false;
	}

	private static CompoundNBT debugNbt;

	private static CompoundNBT getDebugNbt(){
		if(debugNbt==null){
			debugNbt = new CompoundNBT();
			debugNbt.putInt("IntValue", 420);
			debugNbt.putBoolean("BoolValue", true);
			debugNbt.putByte("ByteValue", (byte)127);
			debugNbt.putShort("ShortValue", (short)6300);
			debugNbt.putLong("LongValue", 12352346725L);
			debugNbt.putFloat("FloatValue", 2.5f);
			debugNbt.putDouble("DoubleValue", Math.PI);
			debugNbt.putByteArray("ByteArrayValue", new byte[]{0, 1, 2, 3, 4, 5});
			debugNbt.putIntArray("IntArrayValue", new int[]{0, 1, 2, 3, 4, 5});
			debugNbt.putLongArray("LongArrayValue", new long[]{0, 1, 2, 3, 4, 5});
			debugNbt.putString("StringValue", "Hello petty mortals");
			ListNBT list = new ListNBT();
			list.add(StringNBT.valueOf("List Element 1"));
			list.add(StringNBT.valueOf("List Element 2"));
			list.add(StringNBT.valueOf("List Element 3"));
			debugNbt.put("StringListValue", list);
			ListNBT list2 = new ListNBT();
			CompoundNBT nbt1 = new CompoundNBT();
			nbt1.putInt("ListElementIndex", 0);
			list2.add(nbt1);
			CompoundNBT nbt2 = new CompoundNBT();
			nbt2.putInt("ListElementIndex", 1);
			list2.add(nbt2);
			CompoundNBT nbt3 = new CompoundNBT();
			nbt3.putInt("ListElementIndex", 2);
			list2.add(nbt3);
			debugNbt.put("CompoundListValue", list2);
			debugNbt.putUniqueId("UUID", UUID.randomUUID());
			CompoundNBT innerNBT = new CompoundNBT();
			innerNBT.putInt("InnerIntValue", 1000);
			innerNBT.putString("InnerStringValue", "I am god of the universe");
			ListNBT innerList = new ListNBT();
			innerList.add(StringNBT.valueOf("List Element 1"));
			innerList.add(StringNBT.valueOf("List Element 2"));
			innerList.add(StringNBT.valueOf("List Element 3"));
			innerNBT.put("InnerListValue", innerList);
			debugNbt.put("InnerNBT", innerNBT);
			debugNbt.putByteArray("EmptyByteArrayValue", new byte[0]);
			debugNbt.putIntArray("EmptyIntArrayValue", new int[0]);
			debugNbt.putLongArray("EmptyLongArrayValue", new long[0]);
			debugNbt.put("EmptyNBTArrayValue", new ListNBT());
		}
		return debugNbt;
	}
}
