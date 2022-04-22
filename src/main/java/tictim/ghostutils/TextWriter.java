package tictim.ghostutils;

import static net.minecraft.ChatFormatting.RESET;

public final class TextWriter{
	private final StringBuilder stb = new StringBuilder();
	private int tabs = 0;

	public TextWriter write(Object thing){
		stb.append(thing);
		return this;
	}
	public TextWriter write(char c){
		stb.append(c);
		return this;
	}

	public TextWriter writeAtNewLine(Object thing){
		if(stb.length()>0) nl();
		return write(thing);
	}
	public TextWriter writeAtNewLine(char c){
		if(stb.length()>0) nl();
		return write(c);
	}

	public TextWriter nl(){
		write('\n');
		for(int i=0; i<tabs; i++) write("  ");
		return this;
	}

	public TextWriter rst(){
		return write(RESET);
	}

	public TextWriter tab(){
		tabs++;
		return this;
	}
	public TextWriter untab(){
		tabs--;
		return this;
	}

	@Override public String toString(){
		return stb.toString();
	}
}
