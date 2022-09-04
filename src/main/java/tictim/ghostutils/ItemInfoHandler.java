package tictim.ghostutils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
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
import net.minecraft.tags.ITag.INamedTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.gui.GuiUtils;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
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

	private static ItemStack stackUsedForTooltip = ItemStack.EMPTY;
	private static boolean itemInfoEnabled;

	@SubscribeEvent
	public static void onTick(TickEvent.ClientTickEvent event){
		if(event.phase==TickEvent.Phase.START){
			if(Cfg.enableItemInfo()){
				KeyBinding key = GhostUtils.ClientHandler.getToggleItemInfo();
				if(key.getKeyConflictContext().isActive()&&key.consumeClick()){
					itemInfoEnabled = !itemInfoEnabled;
				}
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
		MatrixStack mat = event.getMatrixStack();
		if(Cfg.itemInfoTest()){
			draw(mat, gui, getDebugText(), event.getMouseY());
		}else if(gui.getMinecraft().player!=null){
			ItemStack stack = gui.getMinecraft().player.inventory.getCarried();
			if(stack.isEmpty()){
				Slot slotSelected = gui.getSlotUnderMouse();
				if(slotSelected!=null) stack = slotSelected.getItem();
				else if(!stackUsedForTooltip.isEmpty()) stack = stackUsedForTooltip;

				if(!stack.isEmpty()) draw(mat, gui, getString(stack), 0);
			}else draw(mat, gui, getString(stack), event.getMouseY());
		}
		stackUsedForTooltip = ItemStack.EMPTY;
	}

	private static void draw(MatrixStack mat, ContainerScreen<?> gui, String text, int scroll){
		Minecraft mc = Minecraft.getInstance();
		FontRenderer font = mc.font;
		MainWindow window = mc.getWindow();

		RenderSystem.disableDepthTest();
		RenderSystem.disableBlend();
		mat.pushPose();
		mat.translate(0, 0, 1);
		double mag = InputMappings.isKeyDown(window.getWindow(), mc.options.keyShift.getKey().getValue()) ?
				Cfg.itemInfoZoomOnSneak() :
				Cfg.itemInfoZoom();
		mat.scale((float)((double)window.getGuiScaledWidth()/window.getWidth()*mag), (float)((double)window.getGuiScaledHeight()/window.getHeight()*mag), 1);
		//noinspection deprecation
		RenderSystem.color4f(1, 1, 1, 1);

		boolean fu = mc.options.forceUnicodeFont;
		if(fu) mc.selectMainFont(false);

		List<IReorderingProcessor> split = font.split(ITextProperties.of(text), window.getWidth()/3);
		mat.translate(0, font.lineHeight-maxScroll(window, font, split.size(), mag)*scroll/(double)gui.height, 400);

		int width = 0;
		for(IReorderingProcessor s : split) width = Math.max(width, font.width(s));
		GuiUtils.drawGradientRect(mat.last().pose(), 0, 0, 2, 4+width, 2+(split.size()+1)*font.lineHeight, 0x80000000, 0x80000000);

		for(int i = 0; i<split.size(); i++){
			font.draw(mat, split.get(i), 2, 2+i*font.lineHeight, -1);
		}

		if(fu) mc.selectMainFont(true);

		mat.popPose();
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
	}

	private static int maxScroll(MainWindow window, FontRenderer font, int lines, double mag){
		return Math.max(0, (lines+2)*font.lineHeight-(int)((double)window.getHeight()/mag));
	}

	private static ItemStack latestStack;
	private static String latestText;

	private static String getString(ItemStack stack){
		if(latestStack==null||stack!=latestStack){
			latestStack = stack.copy();
			Item item = stack.getItem();
			Block block = item instanceof BlockItem ? ((BlockItem)item).getBlock() : null;

			TextWriter text = new TextWriter();

			// Item name and its stack size
			text.write(GOLD).write(stack.getCount()).rst().write(" x ").write(stack.getHoverName().getString());
			// Item/Block ID
			text.nl().write(GRAY).write("Item ID: ").write(item.getRegistryName()).rst();
			if(block!=null)
				text.nl().write(GRAY).write("Block ID: ").write(block.getRegistryName()).rst();

			if(stack.isDamageableItem()){
				int maxDamage = stack.getMaxDamage(), damage = stack.getDamageValue();
				double percentage = (double)(maxDamage-damage)/maxDamage;
				text.nl().nl().write(percentage>=.5 ? GREEN :
								percentage>=.25 ? YELLOW : percentage>=.125 ? GOLD : RED)
						.write(BOLD).write(maxDamage-damage).rst()
						.write(" / ").write(maxDamage)
						.write(" (")
						.write(GOLD).write(percentage<0.01 ? "<1%" : (int)(percentage*100)+"%").rst()
						.write(")");
			}

			List<INamedTag<Item>> itemTags = tags(ItemTags.getWrappers(), item);
			if(!itemTags.isEmpty()){
				text.nl().nl().write(YELLOW).write(BOLD).write("Item Tags:").rst();
				for(INamedTag<Item> tag : itemTags) text.nl().write(" - ").write(tag.getName());
			}
			if(item instanceof BlockItem){
				List<INamedTag<Block>> blockTags = tags(BlockTags.getWrappers(), ((BlockItem)item).getBlock());
				if(!blockTags.isEmpty()){
					text.nl().write(YELLOW).write(BOLD).write("Block Tags:").rst();
					for(INamedTag<Block> tag : blockTags) text.nl().write(" - ").write(tag.getName());
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

	private static <T> List<INamedTag<T>> tags(List<? extends INamedTag<T>> tags, T t){
		List<INamedTag<T>> returns = null;
		for(INamedTag<T> tag : tags){
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
				text.write(((ByteNBT)nbt).getAsByte()!=0 ? GREEN : RED).write(nbt.toString()).rst();
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
				for(byte b : ((ByteArrayNBT)nbt).getAsByteArray()){
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
				String str = nbt.getAsString();
				ResourceLocation rl = ResourceLocation.tryParse(str);
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
				compound.getAllKeys().stream().sorted().forEachOrdered(key -> {
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
				for(int i : ((IntArrayNBT)nbt).getAsIntArray()){
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

	@Nullable private static String debugText;

	private static String getDebugText(){
		if(debugText==null){
			CompoundNBT tag = new CompoundNBT();
			tag.putInt("IntValue", 420);
			tag.putBoolean("BoolValue", true);
			tag.putByte("ByteValue", (byte)127);
			tag.putShort("ShortValue", (short)6300);
			tag.putLong("LongValue", 12352346725L);
			tag.putFloat("FloatValue", 2.5f);
			tag.putDouble("DoubleValue", Math.PI);
			tag.putByteArray("ByteArrayValue", new byte[]{0, 1, 2, 3, 4, 5});
			tag.putIntArray("IntArrayValue", new int[]{0, 1, 2, 3, 4, 5});
			tag.putLongArray("LongArrayValue", new long[]{0, 1, 2, 3, 4, 5});
			tag.putString("StringValue", "Hello petty mortals");
			ListNBT list = new ListNBT();
			list.add(StringNBT.valueOf("List Element 1"));
			list.add(StringNBT.valueOf("List Element 2"));
			list.add(StringNBT.valueOf("List Element 3"));
			tag.put("StringListValue", list);
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
			tag.put("CompoundListValue", list2);
			tag.putUUID("UUID", UUID.randomUUID());
			CompoundNBT innerNBT = new CompoundNBT();
			innerNBT.putInt("InnerIntValue", 1000);
			innerNBT.putString("InnerStringValue", "I am god of the universe");
			ListNBT innerList = new ListNBT();
			innerList.add(StringNBT.valueOf("List Element 1"));
			innerList.add(StringNBT.valueOf("List Element 2"));
			innerList.add(StringNBT.valueOf("List Element 3"));
			innerNBT.put("InnerListValue", innerList);
			tag.put("InnerNBT", innerNBT);
			tag.putByteArray("EmptyByteArrayValue", new byte[0]);
			tag.putIntArray("EmptyIntArrayValue", new int[0]);
			tag.putLongArray("EmptyLongArrayValue", new long[0]);
			tag.put("EmptyNBTArrayValue", new ListNBT());
			tag.putString("ReallyReallyReallyReallyLongStringOfStringsAreReallyReallyReallyReallyLong",
					"ReallyReallyReallyReallyLongStringOfStringsAreReallyReallyReallyReallyLong");

			TextWriter text = new TextWriter();
			nbtToText(text, tag);
			debugText = text.toString();
		}
		return debugText;
	}
}
