package tictim.ghostutils.text;

public interface ITextNode{
	default TextNode append(String text){
		return append(new TextNode(text));
	}
	<T extends ITextNode> T append(T node);
	
	default void newLine(){
		append(new TextNode("\n"));
	}
	default TextNode appendAtNewLine(String text){
		return appendAtNewLine(new TextNode(text));
	}
	<T extends ITextNode> T appendAtNewLine(T node);
	
	default String build(){
		StringBuilder stb = new StringBuilder();
		build(stb);
		return stb.toString();
	}
	
	void build(StringBuilder stb);
}
