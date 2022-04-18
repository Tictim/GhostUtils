package tictim.ghostutils.text;

import javax.annotation.Nullable;
import java.util.Objects;

public final class TextNode implements ITextNode{
	private final String text;
	@Nullable private TextBranch children;
	
	public TextNode(String text){
		this.text = Objects.requireNonNull(text);
	}
	
	@Override
	public <T extends ITextNode> T append(T node){
		if(this.children==null) this.children = new TextBranch();
		return this.children.append(node);
	}
	
	@Override
	public <T extends ITextNode> T appendAtNewLine(T node){
		if(this.children==null) this.children = new TextBranch();
		return this.children.appendAtNewLine(node);
	}
	
	public boolean hasChild(){
		return children!=null&&children.hasChild();
	}
	
	@Override
	public void build(StringBuilder stb){
		stb.append(text);
		if(hasChild()) stb.append("\n  ").append(children.build().replace("\n", "\n  "));
	}
}
