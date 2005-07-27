/*
 * Created on 31.03.2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.cs3.pl.parser.internal.classic;

import junit.framework.TestCase;

import org.cs3.pl.metadata.PrologElementData;
import org.cs3.pl.parser.PrologCompiler;




/**
 * @author windeln
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PrologParserTest extends TestCase {
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	public void testCT() {
		String clause = "ct(name,(classDefT(A,B,C,D),new_id(Class)),(add(classDefT(Class,B1,'NewName',[])))).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
	}

	/**
	 * 
	 * wrong number of parenthesis
	 */
	
	public void testCTFail() {
		String clause = "ct(name,(classDefT(A,B,C,D),new_id(Class),(add(classDefT(Class,B1,'NewName',[])))).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 1);
	}
	
	public void testTokenMgr() {
		String clause = "A\\=A";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
	}
	
	public void testDisjunctions() {
		String clause = "new_idT(_H) :-\n(var(_H), new_id(_H));true.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);	
	}

	public void testNoArg() {
		String clause = "noarg :-\ntrue.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);	
	}

	public void testNumberFollowedByDot() {
		String clause = "ifNegAdd(_val, _val, 0, _) :- _val >= 0.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);	
	}
	
	public void testPredicateSignature() {
		String clause = ":- dynamic test_unify/1.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		
		assertTrue(checker.getProblemNumber() == 0);
		assertEquals(((ASTPredicateSignature)checker.getUnit().children[0].jjtGetChild(0)).getArity(),1);
		clause = ":- dynamic test_unify/0.";
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);
		assertEquals(((ASTPredicateSignature)checker.getUnit().children[0].jjtGetChild(0)).getArity(),0);
		clause = "test(dynamic).";
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);
	}
	
	public void testPredicateArity() {
		String clause = "test(a,b,c).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);
		ASTClause astClause = (ASTClause)checker.getUnit().children[0];
		assertEquals(3,astClause.getArity());
		clause = "test.";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);
		astClause = (ASTClause)checker.getUnit().children[0];
		assertEquals(0,astClause.getArity());
		clause = "test(A) :- A,b.";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);
		astClause = (ASTClause)checker.getUnit().children[0];
		assertEquals(1,astClause.getArity());
	}
	
	public void testSmallerThan() {
		String clause = "compare(<,Element,Element).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);
	}
	
	public void testParenthesis() {
		String clause = "comma_prepend_(_T, ','(_comma,(_comma)), _T).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);
	}
	
	public void testMinusPlus() {
		String clause = "num(-1).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);
	}

	public void testSurroundingParenthesis() {
		String clause = "ct(name,classDefT(_,Parent,Name,[]),add(classDefT(1,B,'Parent',[]))).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		String msg0 = checker.getMsg(0);
		String msg1 = checker.getMsg(1);
		assertTrue(msg0.startsWith(PrologCompiler.MSG_COND_PAREN));		
		assertTrue(msg1.startsWith(PrologCompiler.MSG_ACTION_PAREN));
	}

	public void testParameterizedCTName() {
		String clause = "ct(acget(Name),(classDefT(_,Parent,Name,[])),(add(classDefT(1,Parent,Name,[])))).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);
	}
	
	public void testIsSingleton() {
		String clause = "ct(acget(ClassName), (classDefT(ID,Parent,ClassName,List),\nParent\\=ID),(delete(classDefT(ID,Parent,ClassName,[])))).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 1);
		assertTrue(checker.getMsg(0).equals(PrologCompiler.MSG_SINGLETON_VAR_PREFIX + "List"+PrologCompiler.MSG_SINGLETON_VAR_POSTFIX));
	}

	public void testNotSingleton() {
		String clause = "ct(acget(ClassName), (classDefT(ID,Parent,ClassName,[]),\nParent\\=ID),(delete(classDefT(ID,Parent,ClassName,[])))).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertTrue(checker.getProblemNumber() == 0);
	}

	public void testTokenOffsetTab() {
		String clause = "				test(A).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),1);
		assertEquals(10,checker.getToken(0).beginColumn);
	}

	public void testModule() {
		String clause = ":- help:help(test).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);
		Node child = checker.getUnit().jjtGetChild(0).jjtGetChild(0).jjtGetChild(0).jjtGetChild(0);
		ASTFunctor func = ((ASTFunctor)child);
		assertTrue(func.getModuleName().equals("help") && func.getName().equals("help") );
	}

	public void testDollarInFunctor() {
		String clause = "$test(a).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}
	
	public void testSeveralDynamic() {
		String clause = ":- dynamic (^)/2, (ct)/4, (@)/2, ct/1, ct/2,ct/3.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}

	public void testDefineModule() {
		String clause = "user:goal_expansion( true).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}

	public void testVariableModule() {
		String clause = "p(A) :- A:aha(A).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
		clause = "p(a) :- aha.";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}
	
		public void testSpecialAtoms() {
		String clause = "p(a) :- p(:,$,:-,*->,=@=).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}
	
	public void testNotProvable() {
		String clause = "p(a) :- p(c), \\+ \\+ p(b).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}
	
	public void testMessage() { // TODO: what is this?
		String clause = "prolog:message(assumption_failed(G)) :- \n[ 'Assumption failed: ~p'-[G] ].";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
		clause = "prolog:message --> \n[ 'Assumption failed: ~p'-[a] ].";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}

	public void testVariableDividedAtom() { 
		String clause = "test(\\+ a).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}	

	public void testDoubleChar() { 
		String clause = "test(\"\"\"\",'''a''''').";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}

	public void testModuleExport() { 
		String clause = ":- module(gensym, [ reset_gensym/0, reset_gensym/1, gensym/2]).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
		assertEquals("gensym",checker.getModuleName());
	}

	public void testInitialization() { 
		String clause = ":- initialization p2(a).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}

	public void testBraces() { 
		String clause = "test :- {a,b}.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}

	public void testParenAroundClause() { 
		String clause = "user:(c(test) :- a,b).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}
	
	public void testCharAtomFunctor() { 
		String clause = "'$->'(a).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
		clause = "user:'$->'(a).";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}
	public void testBinOctHex() { 
		String clause = "test(0x01,0o07,0b10,0xAFaf).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
		clause = "test(0b2)";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),1);	
	}
	
	public void testBitWise() { 
		String clause = "test(A,B) :- A xor B, A /\\ B, A \\/ B, A \\// B.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
	}
	
	public void testCorrectAndUncorrectClauses() { 
		String clause = "test(C) :- C. test(A) :- A,. test(B) :- B.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),1);	
		assertEquals(checker.getUnit().children.length,2);
	}
	
	public void testSeveralErrors() { 
		String clause = "test(A) :- A,. test(B) :- B,.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),2);	
	}
	public void testWeirdStuff() { 
		String clause = "test(\\+ a).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
		clause = "test(A:A/A).";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
		clause = "test(A:A).";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);	
		clause = "test(a:a).";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);
		clause = "style_chck(+dollar).";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);
		clause = "test(0'$).";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);
		clause = "test(A):- A^(a),t^(b).";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);
		clause = "append(-, _).";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);
		clause = "@(a). ^(b).";
		checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);
		
	}

	public void testReadSpecialTokensMultiLine() { 
		String clause = "/* test(?a)\n \"documentation*/\ntest(A) :- A.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals("\"test(?a)\n \\\"documentation\"",((ASTClause)checker.getClauses().get(0)).getComment("test"));	
	}
	public void testReadSpecialTokensSingleLine() { 
		String clause = "% test(?a)\n% \"documentation\ntest(A) :- A.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals("\"test(?a)\n \\\"documentation\n\"",((ASTClause)checker.getClauses().get(0)).getComment("test"));	
	}
	public void testDynamicDocumenation() { 
		String clause = "/*test(?a)\naha*/\n:-dynamic test/1,test/2.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		ASTPredicateSignature siganture = (ASTPredicateSignature)checker.getDynamic()[0].children[0];
		assertEquals(siganture.getName(),"test");	
		assertEquals(siganture.getModule(),"user");	
		assertEquals(siganture.getArity(),1);	
		assertEquals("\"test(?a)\naha\"", checker.getDynamic()[0].getComment("test"));	
	}
	public void testWrongArity() { 
		String clause = ":- getFieldT(_Get,Parent, _, _Encl, _Recv, _Name, _VID),applyT(_call, _parent, _enclMethod, _origReceiver, _).";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),2);
		assertEquals("Expected arity for 'getFieldT' is 6, arity found: 7.", checker.getMsg(0));
		assertEquals("Expected arity for 'applyT' is 7, arity found: 5.", checker.getMsg(1));
	}
	
	/**
	 * @throws CoreException
	 * 
	 */
//	public void testModuleHelp() {
//		String clause = "\n/*module help*/\n:- module(test, [test1/1,test2/2]).\n"+
//		"test2(a,b).\ntest2(b,b).\ntest1(a).\n ";
//		PrologCompilerBackend checker = new PrologCompilerBackend();
//		checker.compile(clause);
//		assertEquals(checker.getModuleName(),"test");
//		assertEquals("\"module help\"", checker.getUnit().getComment(null));
//		PrologElementData[] elems = checker.getInterfaceElements();
//		assertEquals(2,elems.length);
//		assertEquals("test1/1",elems[0].getSignature());
//		assertEquals("test2/2",elems[1].getSignature());
//	}
//	
//	public void testModuleHelpError() {
//		String clause = "\n/*module help*/\n:- module(test, [test3/1,test2/2]).\n"+
//		"test2(a,b).\ntest2(b,b).\ntest1(a).\n ";
//		PrologCompilerBackend checker = new PrologCompilerBackend();
//		checker.compile(clause);
//		PrologModule module = checker.getModule(); 
//		assertEquals(module.getName(),"test");
//		assertEquals("module help", module.getHelp());
//		PrologElementData[] elems = checker.getInterfaceElements();
//		assertEquals(elems.length,1);
//		assertEquals("test2/2",elems[0].getSignature());
//	}
//
//	public void testUserModuleHelp() {
//		String clause = "test2(a,b).\ntest2(b,b).\ntest1(a).\n ";
//		PrologCompilerBackend checker = new PrologCompilerBackend();
//		checker.compile(clause);
//		assertEquals(checker.getModuleName(),"user");
//		assertEquals(null, checker.getUnit().getComment(null));
//		PrologElementData[] elems = checker.getInterfaceElements();
//		assertEquals(2,elems.length);
//		assertEquals("test1/1",elems[0].getSignature());
//		assertEquals("test2/2",elems[1].getSignature());
//	}

	public void testIntegerDivision() {
		String clause = "hello(X, Y) :- Y is 100 // X.\n";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);
	}
	
	public void testFactbase() {
		String clause = "";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(checker.getProblemNumber(),0);
	}

	
	public void testLineOffset() {
		String clause = 
	    "\nt1.\nt2.\ntestccc.";
		PrologCompilerBackend checker = new PrologCompilerBackend();
		checker.compile(clause);
		assertEquals(1,checker.getLineOffset(((SimpleNode) checker.getUnit().jjtGetChild(0)).getFirstToken().beginLine));
		assertEquals(5,checker.getLineOffset(((SimpleNode) checker.getUnit().jjtGetChild(1)).getFirstToken().beginLine));
		assertEquals(9,checker.getLineOffset(((SimpleNode) checker.getUnit().jjtGetChild(2)).getFirstToken().beginLine));
		PrologElementData[] elements = checker.getPrologElements();
		assertEquals(1,elements[0].getPosition());
		assertEquals(2,elements[0].getLength());
		assertEquals(5,elements[1].getPosition());
		assertEquals(9,elements[2].getPosition());
	}
}


