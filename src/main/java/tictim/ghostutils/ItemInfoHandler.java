package tictim.ghostutils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.GuiUtils;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.tags.IReverseTag;
import net.minecraftforge.registries.tags.ITagManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static net.minecraft.ChatFormatting.*;
import static tictim.ghostutils.GhostUtils.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class ItemInfoHandler{
	private ItemInfoHandler(){}

	private static ItemStack stackUsedForTooltip = ItemStack.EMPTY;
	private static boolean itemInfoEnabled;

	@SubscribeEvent
	public static void onTick(TickEvent.ClientTickEvent e){
		if(e.phase==TickEvent.Phase.START){
			if(Cfg.enableItemInfo()){
				KeyMapping key = GhostUtils.ClientHandler.getToggleItemInfo();
				if(key.getKeyConflictContext().isActive()&&key.consumeClick()){
					itemInfoEnabled = !itemInfoEnabled;
				}
			}else itemInfoEnabled = false;
		}
	}

	@SubscribeEvent
	public static void onDrawTooltip(RenderTooltipEvent.GatherComponents event){
		if(itemInfoEnabled) stackUsedForTooltip = event.getItemStack();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void guiRender(ScreenEvent.DrawScreenEvent.Post event){
		if(!itemInfoEnabled||!(event.getScreen() instanceof AbstractContainerScreen gui)){
			return;
		}
		PoseStack poseStack = event.getPoseStack();
		if(Cfg.itemInfoTest()){
			draw(poseStack, gui, getDebugText(), event.getMouseY());
		}else if(gui.getMinecraft().player!=null){
			ItemStack stack = gui.getMenu().getCarried();
			if(stack.isEmpty()){
				Slot slotSelected = gui.getSlotUnderMouse();
				if(slotSelected!=null) stack = slotSelected.getItem();
				else if(!stackUsedForTooltip.isEmpty()) stack = stackUsedForTooltip;
				if(!stack.isEmpty()) draw(poseStack, gui, getString(stack), 0);
			}else draw(poseStack, gui, getString(stack), event.getMouseY());
		}
		stackUsedForTooltip = ItemStack.EMPTY;
	}

	private static void draw(PoseStack poseStack, AbstractContainerScreen<?> gui, String text, int scroll){
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		Window window = mc.getWindow();

		RenderSystem.disableDepthTest();
		RenderSystem.disableBlend();
		poseStack.pushPose();
		poseStack.translate(0, 0, 1);
		double mag = InputConstants.isKeyDown(window.getWindow(), mc.options.keyShift.getKey().getValue()) ?
				Cfg.itemInfoZoomOnSneak() :
				Cfg.itemInfoZoom();
		poseStack.scale((float)((double)window.getGuiScaledWidth()/window.getWidth()*mag), (float)((double)window.getGuiScaledHeight()/window.getHeight()*mag), 1);
		RenderSystem.setShaderColor(1, 1, 1, 1);

		boolean fu = mc.options.forceUnicodeFont;
		if(fu) mc.selectMainFont(false);

		List<FormattedCharSequence> split = font.split(FormattedText.of(text), window.getWidth()/3);
		poseStack.translate(0, font.lineHeight-maxScroll(window, font, split.size(), mag)*scroll/(double)gui.height, 400);

		int width = 0;
		for(FormattedCharSequence s : split) width = Math.max(width, font.width(s));
		GuiUtils.drawGradientRect(poseStack.last().pose(), 0, 0, 2, 4+width, 2+(split.size()+1)*font.lineHeight, 0x80000000, 0x80000000);

		for(int i = 0; i<split.size(); i++){
			font.draw(poseStack, split.get(i), 2, 2+i*font.lineHeight, -1);
		}

		if(fu) mc.selectMainFont(true);

		poseStack.popPose();
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
	}

	private static int maxScroll(Window window, Font font, int lines, double mag){
		return Math.max(0, (lines+2)*font.lineHeight-(int)((double)window.getHeight()/mag));
	}

	private static ItemStack latestStack;
	private static String latestText;

	private static String getString(ItemStack stack){
		if(latestStack==null||stack!=latestStack){
			latestStack = stack.copy();
			Item item = stack.getItem();
			Block block = item instanceof BlockItem bi ? bi.getBlock() : null;

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
			TagKey<?>[] itemTags = fucks(ForgeRegistries.ITEMS, item).toArray(TagKey[]::new);
			if(itemTags.length>0){
				text.nl().nl().write(YELLOW).write(BOLD).write("Item Tags:").rst();
				for(TagKey<?> tag : itemTags) text.nl().write(" - ").write(tag.location());
			}
			if(block!=null){
				TagKey<?>[] blockTags = fucks(ForgeRegistries.BLOCKS, block).toArray(TagKey[]::new);
				if(blockTags.length>0){
					if(itemTags.length==0) text.nl();
					text.nl().write(YELLOW).write(BOLD).write("Block Tags:").rst();
					for(TagKey<?> tag : blockTags) text.nl().write(" - ").write(tag.location());
				}
			}
			CompoundTag nbt = stack.getTag();
			if(nbt!=null){
				text.nl().nl().write(GREEN).write(BOLD).write("NBT: ").rst();
				nbtToText(text, nbt);
			}
			latestText = text.toString();
		}
		return latestText;
	}

	private static <T extends ForgeRegistryEntry<T>> Stream<TagKey<T>> fucks(IForgeRegistry<T> registry, T entry){
		ITagManager<T> tags = registry.tags();
		if(tags==null) return Stream.empty();
		Optional<IReverseTag<T>> reverseTag = tags.getReverseTag(entry);
		if(!reverseTag.isPresent()) return Stream.empty();
		return reverseTag.get().getTagKeys();
	}

	private static void nbtToText(TextWriter text, Tag nbt){
		switch(nbt.getId()){
			case Tag.TAG_END -> text.write(DARK_GRAY).write("(END)").rst();
			case Tag.TAG_BYTE -> text.write(((ByteTag)nbt).getAsByte()!=0 ? GREEN : RED).write(nbt.toString()).rst();
			case Tag.TAG_SHORT, Tag.TAG_INT, Tag.TAG_LONG, Tag.TAG_FLOAT, Tag.TAG_DOUBLE -> text.write(GOLD).write(nbt.toString()).rst();
			case Tag.TAG_BYTE_ARRAY -> {
				if(((ByteArrayTag)nbt).isEmpty()){
					text.write("[B; ]");
					return;
				}
				text.write("[B;").tab();
				boolean first = true;
				for(byte b : ((ByteArrayTag)nbt).getAsByteArray()){
					if(first){
						first = false;
						text.nl();
					}else text.write(", ");
					text.write(b!=0 ? GREEN : RED).write(b).rst();
				}
				text.untab().writeAtNewLine("]");
			}
			case Tag.TAG_STRING -> {
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
			}
			case Tag.TAG_LIST -> {
				if(((ListTag)nbt).isEmpty()){
					text.write("[NBT; ]");
					return;
				}
				text.write("[NBT;").tab();
				for(Tag nbt2 : (ListTag)nbt) nbtToText(text.nl(), nbt2);
				text.untab().writeAtNewLine("]");
			}
			case Tag.TAG_COMPOUND -> {
				CompoundTag compound = (CompoundTag)nbt;
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
			}
			case Tag.TAG_INT_ARRAY -> {
				if(((IntArrayTag)nbt).isEmpty()){
					text.write("[I; ]");
					return;
				}
				text.write("[I;").tab();
				boolean first = true;
				for(int i : ((IntArrayTag)nbt).getAsIntArray()){
					if(first){
						first = false;
						text.nl();
					}else text.write(", ");
					text.write(GOLD).write(i).rst();
				}
				text.untab().writeAtNewLine("]");
			}
			case Tag.TAG_LONG_ARRAY -> {
				if(((LongArrayTag)nbt).isEmpty()){
					text.write("[L; ]");
					return;
				}
				text.write("[L;").tab();
				boolean first = true;
				for(long l : ((LongArrayTag)nbt).getAsLongArray()){
					if(first){
						first = false;
						text.nl();
					}else text.write(", ");
					text.write(GOLD).write(l).rst();
				}
				text.untab().writeAtNewLine("]");
			}
			default -> text.write("(Unknown NBT Data)");
		}
	}

	@Nullable private static String debugText;

	private static String getDebugText(){
		if(debugText==null){
			CompoundTag tag = new CompoundTag();
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
			ListTag list = new ListTag();
			list.add(StringTag.valueOf("List Element 1"));
			list.add(StringTag.valueOf("List Element 2"));
			list.add(StringTag.valueOf("List Element 3"));
			tag.put("StringListValue", list);
			ListTag list2 = new ListTag();
			CompoundTag nbt1 = new CompoundTag();
			nbt1.putInt("ListElementIndex", 0);
			list2.add(nbt1);
			CompoundTag nbt2 = new CompoundTag();
			nbt2.putInt("ListElementIndex", 1);
			list2.add(nbt2);
			CompoundTag nbt3 = new CompoundTag();
			nbt3.putInt("ListElementIndex", 2);
			list2.add(nbt3);
			tag.put("CompoundListValue", list2);
			tag.putUUID("UUID", UUID.randomUUID());
			CompoundTag innerNBT = new CompoundTag();
			innerNBT.putInt("InnerIntValue", 1000);
			innerNBT.putString("InnerStringValue", "I am god of the universe");
			ListTag innerList = new ListTag();
			innerList.add(StringTag.valueOf("List Element 1"));
			innerList.add(StringTag.valueOf("List Element 2"));
			innerList.add(StringTag.valueOf("List Element 3"));
			innerNBT.put("InnerListValue", innerList);
			tag.put("InnerNBT", innerNBT);
			tag.putByteArray("EmptyByteArrayValue", new byte[0]);
			tag.putIntArray("EmptyIntArrayValue", new int[0]);
			tag.putLongArray("EmptyLongArrayValue", new long[0]);
			tag.put("EmptyNBTArrayValue", new ListTag());
			tag.putString("ReallyReallyReallyReallyLongStringOfStringsAreReallyReallyReallyReallyLong",
					"ReallyReallyReallyReallyLongStringOfStringsAreReallyReallyReallyReallyLong");

			TextWriter text = new TextWriter();
			nbtToText(text, tag);
			debugText = text.toString();
		}
		return debugText;
	}
}
