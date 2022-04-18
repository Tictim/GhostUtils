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
import net.minecraft.nbt.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
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
import tictim.ghostutils.text.ITextNode;
import tictim.ghostutils.text.TextBranch;
import tictim.ghostutils.text.TextNode;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.util.text.TextFormatting.*;
import static tictim.ghostutils.GhostUtils.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class ItemInfoHandler{
	private ItemInfoHandler(){}
	
	private static FontRenderer fontRenderer;
	private static CompoundNBT debugNbt;
	
	static void init(){
		//noinspection SpellCheckingInspection
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
			draw(gui, nbtToTextNode(debugNbt).build(), event.getMouseY());
		}else{
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
		for(String s: list) if(!s.isEmpty()) width = Math.max(width, fontRenderer.getStringWidth(s));
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
			TextBranch text = new TextBranch();
			
			// Item name and its stack size
			text.appendAtNewLine(format(String.valueOf(stack.getCount()), GOLD)+" x "+stack.getDisplayName().getFormattedText());
			// Item/Block ID
			text.appendAtNewLine(format("Item ID: "+item.getRegistryName(), GRAY));
			if(item instanceof BlockItem) text.appendAtNewLine(format("Block ID: "+((BlockItem)item).getBlock().getRegistryName(), GRAY));
			
			if(stack.isDamageable()){
				int m = stack.getMaxDamage(), d = stack.getDamage();
				text.newLine();
				text.appendAtNewLine(String.format("%s / %d (%s)", format(BOLD+Integer.toString(m-d), getColorByDamage((double)(m-d)/m)), m, format(percentage(m-d, m), GOLD)));
			}
			
			List<Tag<Item>> itemTags = itemTags(item);
			if(!itemTags.isEmpty()){
				text.newLine();
				text.appendAtNewLine(format(BOLD+"Item Tags:", YELLOW));
				for(Tag<Item> tag: itemTags) text.appendAtNewLine(" - "+tag.getId());
			}
			if(item instanceof BlockItem){
				List<Tag<Block>> blockTags = blockTags(((BlockItem)item).getBlock());
				if(!blockTags.isEmpty()){
					text.newLine();
					text.appendAtNewLine(format(BOLD+"Block Tags:", YELLOW));
					for(Tag<Block> tag: blockTags) text.appendAtNewLine(" - "+tag.getId());
				}
			}
			CompoundNBT nbt = stack.getTag();
			if(nbt!=null){
				text.newLine();
				text.appendAtNewLine(format(BOLD+"NBT: ", GREEN));
				text.append(nbtToTextNode(nbt));
			}
			latestText = text.build();
		}
		return latestText;
	}
	
	private static List<Tag<Item>> itemTags(Item item){
		return tags(ItemTags.getCollection(), item);
	}
	
	private static List<Tag<Block>> blockTags(Block block){
		return tags(BlockTags.getCollection(), block);
	}
	
	private static <T> List<Tag<T>> tags(TagCollection<T> tags, T t){
		List<Tag<T>> returns = null;
		for(Tag<T> tag: tags.getTagMap().values()){
			if(tag.contains(t)){
				if(returns==null) returns = new ArrayList<>();
				returns.add(tag);
			}
		}
		return returns!=null ? returns : Collections.emptyList();
	}
	
	private static TextFormatting getColorByDamage(double percentage){
		if(percentage >= .5) return GREEN;
		else if(percentage >= .25) return YELLOW;
		else if(percentage >= .125) return GOLD;
		else return RED;
	}
	
	private static String percentage(int damage, int maxDamage){
		double d = damage/(double)maxDamage;
		if(d<0.01) return "<1%";
		else return (int)(d*100)+"%";
	}
	
	private static ITextNode nbtToTextNode(INBT nbt){
		switch(nbt.getId()){
			case NBT.TAG_END:
				return new TextNode(format("(END)", DARK_GRAY));
			case NBT.TAG_BYTE:
				return new TextNode(format(nbt.toString(), ((ByteNBT)nbt).getByte()!=0 ? GREEN : RED));
			case NBT.TAG_SHORT:
			case NBT.TAG_INT:
			case NBT.TAG_LONG:
			case NBT.TAG_FLOAT:
			case NBT.TAG_DOUBLE:
				return new TextNode(format(nbt.toString(), GOLD));
			case NBT.TAG_BYTE_ARRAY:
				return TextBranch.bracket((ByteArrayNBT)nbt, "[B;", "]", (arr, b) -> {
					byte[] byteArray = arr.getByteArray();
					for(int i = 0; i<byteArray.length; i++){
						if(i!=0) b.append(", ");
						b.append(format(Byte.toString(byteArray[i]), byteArray[i]!=0 ? GREEN : RED));
					}
				});
			case NBT.TAG_STRING:{
				String str = nbt.getString();
				ResourceLocation rl = ResourceLocation.tryCreate(str);
				if(rl!=null) return new TextNode(String.format("%s%s%s%s%s", format("\"", GREEN), format(rl.getNamespace(), YELLOW), format(":", GREEN), rl.getPath(), format("\"", GREEN)));
				else return new TextNode(format("\""+str+"\"", GREEN)); // TODO JSON formatting
			}
			case NBT.TAG_LIST:
				return TextBranch.bracket((ListNBT)nbt, "[NBT;", "]", (list, b) -> {
					for(INBT nbt2: list) b.appendAtNewLine(nbtToTextNode(nbt2));
				});
			case NBT.TAG_COMPOUND:
				return TextBranch.bracket((CompoundNBT)nbt, "{", "}", (
						compound, b) -> compound.keySet().stream().sorted().forEachOrdered(key -> {
					b.appendAtNewLine(format(key, YELLOW));
					b.append(": ");
					b.append(nbtToTextNode(compound.get(key)));
				}));
			case NBT.TAG_INT_ARRAY:
				return TextBranch.bracket((IntArrayNBT)nbt, "[I;", "]",
						(arr, b) -> b.append(Arrays.stream(arr.getIntArray()).mapToObj(i -> format(Integer.toString(i), GOLD)).collect(Collectors.joining(", "))));
			case NBT.TAG_LONG_ARRAY:
				return TextBranch.bracket((LongArrayNBT)nbt, "[L;", "]",
						(arr, b) -> b.append(Arrays.stream(arr.getAsLongArray()).mapToObj(i -> format(Long.toString(i), GOLD)).collect(Collectors.joining(", "))));
			default:
				return new TextNode("(Unknown NBT Data)");
		}
	}
	
	private static String format(String string, TextFormatting formatting){
		return formatting+string+RESET;
	}
	
	private static boolean debugMode(){
		return false;
	}
}
