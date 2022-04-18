package tictim.ghostutils.text;

import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class TextBranch implements ITextNode{
	public static ITextNode bracket(CompoundNBT nbt, String opening, String closing, BiConsumer<CompoundNBT, ITextNode> appender){
		return nbt.isEmpty() ? new TextNode(opening+" "+closing) : bracketInternal(nbt, opening, closing, appender);
	}
	public static ITextNode bracket(ListNBT nbt, String opening, String closing, BiConsumer<ListNBT, ITextNode> appender){
		return nbt.isEmpty() ? new TextNode(opening+" "+closing) : bracketInternal(nbt, opening, closing, appender);
	}
	public static ITextNode bracket(ByteArrayNBT nbt, String opening, String closing, BiConsumer<ByteArrayNBT, ITextNode> appender){
		return nbt.isEmpty() ? new TextNode(opening+" "+closing) : bracketInternal(nbt, opening, closing, appender);
	}
	public static ITextNode bracket(IntArrayNBT nbt, String opening, String closing, BiConsumer<IntArrayNBT, ITextNode> appender){
		return nbt.isEmpty() ? new TextNode(opening+" "+closing) : bracketInternal(nbt, opening, closing, appender);
	}
	public static ITextNode bracket(LongArrayNBT nbt, String opening, String closing, BiConsumer<LongArrayNBT, ITextNode> appender){
		return nbt.isEmpty() ? new TextNode(opening+" "+closing) : bracketInternal(nbt, opening, closing, appender);
	}
	private static <NBT extends INBT> TextBranch bracketInternal(NBT nbt, String opening, String closing, BiConsumer<NBT, ITextNode> appender){
		TextBranch branch = new TextBranch();
		appender.accept(nbt, branch.append(opening));
		branch.appendAtNewLine(closing);
		return branch;
	}

	private final List<ITextNode> children = new ArrayList<>();

	@Override
	public <T extends ITextNode> T append(T child){
		this.children.add(Objects.requireNonNull(child));
		return child;
	}

	@Override
	public <T extends ITextNode> T appendAtNewLine(T node){
		if(!children.isEmpty()) newLine();
		append(node);
		return node;
	}

	public boolean hasChild(){
		return !children.isEmpty();
	}

	@Override
	public void build(StringBuilder stb){
		if(hasChild()) for(ITextNode child : children) child.build(stb);
	}
}
