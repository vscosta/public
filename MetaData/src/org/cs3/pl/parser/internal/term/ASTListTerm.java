/* Generated By:JJTree: Do not edit this line. ASTListTerm.java */

package org.cs3.pl.parser.internal.term;

public class ASTListTerm extends SimpleNode {
	public ASTListTerm(int id) {
		super(id);
	}

	public ASTListTerm(PrologTermParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(PrologTermParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	public String toString() {
		return super.toString() + " [..]";
	}

	public SimpleNode[] getElements(){
		if(jjtGetNumChildren()==0){
			return new SimpleNode[0];
		}
		SimpleNode node = (SimpleNode) children[0];
		if (node instanceof ASTInfixTerm) {
			ASTInfixTerm term = (ASTInfixTerm) node;
			
			if ("|".equals(term.getPrincipal().getImage())) {
				node = (SimpleNode) term.children[1];
				if(node instanceof ASTInfixTerm){
					term=(ASTInfixTerm) node;
				}
				return new SimpleNode[] { node};
			}
			if (",".equals(term.getOperator().getValue())) {
				SimpleNode[] args = new SimpleNode[term.children.length - 1];
				System.arraycopy(term.children, 1, args, 0, args.length);
				return args;
			}
			
		}
		return new SimpleNode[] { (SimpleNode) node };
	}
	
	public SimpleNode getTail(){
		if(children==null||children.length==0){
			return null;
		}
		SimpleNode node = (SimpleNode) children[0];
		if (node instanceof ASTInfixTerm) {
			ASTInfixTerm term = (ASTInfixTerm) node;
			
			if ("|".equals(term.getOperator().getValue())) {
				return (SimpleNode) term.children[1];
			}
		}
		return null;
	}
	
	protected void synthesizeImage(StringBuffer sb) {
		sb.append("[");
		SimpleNode[] elms = getElements();
		if(elms!=null){
			for (int i = 0; i < elms.length; i++) {
				SimpleNode c = (SimpleNode) elms[i];
				if(i>0){
					sb.append(", ");					
				}
				c.synthesizeImage(sb);
			}
			
		}
		SimpleNode tail = getTail();
		if(tail!=null){
			sb.append("|");
			tail.synthesizeImage(sb);
		}
		sb.append("]");
	}

	public  SimpleNode createShallowCopy() {
		ASTListTerm copy = new ASTListTerm(parser,id);
		copy.copy=true;
		
		return copy;
	}
	
	public SimpleNode toCanonicalTerm(boolean linked, boolean deep){
		
		if(jjtGetNumChildren()==0){
			return (SimpleNode) clone(linked,deep);
		}
		int i=children==null?0:getElements().length-1;
		
		SimpleNode r=null;
		
		ASTInfixOperator comma = new ASTInfixOperator(parser,PrologTermParserTreeConstants.JJTINFIXOPERATOR);
		comma.value=",";
		comma.copy=true;
		ASTCharacters label = new ASTCharacters(parser,PrologTermParserTreeConstants.JJTCHARACTERS);
		label.copy=true;
		label.value=".";
		if(linked){
			label.original=this;
			comma.original=this;
		}
		SimpleNode[] elms = getElements();
		r = getTail();
		if(deep&&r!=null){
			r=r.toCanonicalTerm(linked,deep);
		}else{
			r = new ASTListTerm(parser,id);
			r.copy=true;
			if(linked){
				r.original=this;
			}
		}
		
		while(i>=0){						
			ASTInfixTerm args = new ASTInfixTerm(parser,PrologTermParserTreeConstants.JJTINFIXTERM);
			args.copy=true;
			if(linked){
				args.original=this;
			}
			args.children = new Node[3];
			args.children[0]=(Node) comma.clone(linked,deep);
			if(r==null){
				args.children[1]=deep? ((SimpleNode)elms[i-1]).toCanonicalTerm(linked,deep):elms[i-1];
				args.children[2]=deep? ((SimpleNode)elms[i]).toCanonicalTerm(linked,deep):elms[i];
				i-=2;
			}
			else{
				args.children[1]=deep? ((SimpleNode)elms[i]).toCanonicalTerm(linked,deep):elms[i];
				args.children[2]=r;
				i-=1;
			}
			r = new ASTCompoundTerm(parser,PrologTermParserTreeConstants.JJTCOMPOUNDTERM);
			r.copy=true;
			if(linked){
				r.original=this;
			}
			r.children=new Node[2];
			r.children[0]=(Node) label.clone(linked, deep);
			r.children[1]=args;
		}
		return (ASTCompoundTerm) r;
	}
	
	public String getLabel() {	
		return "'.'";
	}
	public int getArity(){
		return 2;
	}
}
