/* Generated By:JJTree: Do not edit this line. ASTBracesTerm.java */

package org.cs3.pl.parser.internal.term;

public class ASTBracesTerm extends SimpleNode {
	public ASTBracesTerm(int id) {
		super(id);
	}

	public ASTBracesTerm(PrologTermParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(PrologTermParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	public String toString() {
		return super.toString() + " {..}";
	}

	protected void synthesizeImage(StringBuffer sb) {
		sb.append("{");
		((SimpleNode) children[0]).synthesizeImage(sb);
		sb.append("}");

	}

	public SimpleNode createShallowCopy() {
		ASTBracesTerm copy = new ASTBracesTerm(parser,id);
		copy.copy=true;		
		return copy;
	}

	public SimpleNode toCanonicalTerm(boolean linked, boolean deep){
		ASTCompoundTerm a = new ASTCompoundTerm(parser,PrologTermParserTreeConstants.JJTCOMPOUNDTERM);
		a.copy=true;
		if(linked){
			a.original=this;
		}
		a.children = new Node[2];
		ASTIdentifier label = new ASTIdentifier(parser,PrologTermParserTreeConstants.JJTIDENTIFIER);
		label.copy=true;
		label.value="{}";
		if(linked){
			label.original=this;
		}
		a.children[0]= label;
		if(deep){
			a.children[1]=((SimpleNode)children[0]).toCanonicalTerm(linked,deep);
		}else{
			a.children[1]=children[0];
		}
		
		return a;
	}
	public String getLabel() {
		return "{}";
	}
	public int getArity(){
		return 1;
	}
}
