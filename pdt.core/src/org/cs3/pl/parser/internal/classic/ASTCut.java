/* Generated By:JJTree: Do not edit this line. ASTCut.java */

package org.cs3.pl.parser.internal.classic;

public class ASTCut extends SimpleNode implements ASTTerm {
  public ASTCut(int id) {
    super(id);
  }

  public ASTCut(PrologParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(PrologParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
