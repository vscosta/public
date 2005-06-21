/* Generated By:JJTree: Do not edit this line. ASTInfixTerm.java */

package org.cs3.pl.parser.internal.term;

import java.util.Vector;

public class ASTInfixTerm extends SimpleNode {
	public ASTInfixTerm(int id) {
		super(id);
	}

	public ASTInfixTerm(PrologTermParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(PrologTermParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	public SimpleNode getPrincipal() {
		for (int i = 0; i < children.length; i++) {
			if(children[i] instanceof ASTInfixOperator){
				return (SimpleNode) children[i];
			}
		}
		return null;
	}

	public ASTInfixOperator getOperator() {
		return (ASTInfixOperator) getPrincipal();
	}
	
	public SimpleNode[] getOperands(){
		SimpleNode[] operands = new SimpleNode[children.length-1];
		System.arraycopy(children,1,operands,0,operands.length);
		return operands;
	}
	
	
	protected int getPrincipalIndex() {
		for (int i = 0; i < children.length; i++) {
			if(children[i] instanceof ASTInfixOperator){
				return i;
			}
		}
		return -1;
	}
	
	public String toString() {
		return super.toString() + " (" + getPrincipal().getImage()
				+ ")";
	}
	/**
	 * move principal to front, 
	 * inline children, if they are infix terms with the same operator
	 * 
	 * no need for recursion, since tree is build bottom-up 
	 */
	public void flatten() {
		int pi= getPrincipalIndex();		
		SimpleNode tmp = getPrincipal();
		for(int i=pi;i>0;i--){
			children[i]=children[i-1];
		}
		children[0]=tmp;
		
		Vector v = new Vector();
		
		for (int i = 0; i < children.length; i++) {
			Node n = children[i];
			if(n instanceof ASTInfixTerm){
				ASTInfixTerm term = (ASTInfixTerm) n;
				if(tmp.getImage().equals(term.getPrincipal().getImage())){
					for(int j=1;j<term.children.length;j++){
						v.add(term.children[j]);
						term.children[j].jjtSetParent(this);
					}
				}
				else{
					v.add(term);
				}
			}
			else{
				v.add(n);
			}			
		}
		if(v.size()>children.length){ //otherwise nothing is to do.
			children=(Node[]) v.toArray(new Node[v.size()]);
		}
		
	}
	protected void synthesizeImage(StringBuffer sb) {
		for (int i = 1; i < children.length; i++) {
			SimpleNode child = (SimpleNode) children[i];
			if(i>1){
				getPrincipal().synthesizeImage(sb);
			}
			child.synthesizeImage(sb);
		}

	}

	public SimpleNode createShallowCopy() {
		ASTInfixTerm copy = new ASTInfixTerm(parser,id);
		copy.copy=true;		
		return copy;
	}
	
	public SimpleNode toCanonicalTerm(boolean linked, boolean deep){
		int i=children.length-1;
		ASTCompoundTerm r=null;
		if(getLabel().equals("','")&&getOperands()[0].getLabel().equals("classDefT")){
			System.out.println("debug");
		}
		ASTInfixOperator comma = new ASTInfixOperator(parser,PrologTermParserTreeConstants.JJTINFIXOPERATOR);
		comma.value=",";
		comma.copy=true;
		
		ASTCharacters label = new ASTCharacters(parser,PrologTermParserTreeConstants.JJTCHARACTERS);
		label.copy=true;
		label.value=getOperator().getValue();
		if(linked){
			label.original=this;
			comma.original=this;
		}
		while(i>0){						
			ASTInfixTerm args = new ASTInfixTerm(parser,PrologTermParserTreeConstants.JJTINFIXTERM);
			args.copy=true;
			if(linked){
				args.original=this;
			}
			args.children = new Node[3];
			args.children[0]=(Node) comma.clone(linked,deep);
			if(r==null){
				args.children[1]=deep? ((SimpleNode)children[i-1]).toCanonicalTerm(linked,deep):children[i-1];
				args.children[2]=deep? ((SimpleNode)children[i]).toCanonicalTerm(linked,deep):children[i];
				i-=2;
			}
			else{
				args.children[1]=deep? ((SimpleNode)children[i]).toCanonicalTerm(linked,deep):children[i];
				args.children[2]=r;
				i-=1;
			}
			r = new ASTCompoundTerm(parser,PrologTermParserTreeConstants.JJTCOMPOUNDTERM);
			r.copy=true;
			if(linked){
				r.original=this;
			}
			r.children=new Node[2];
			r.children[0]=(Node) label.clone(linked,deep);
			r.children[1]=args;
		}
		return r;
	}
	public int getArity() {
		return 2;
	}
	
	public String getLabel() {	
		return getOperator().getLabel();
	}
}
