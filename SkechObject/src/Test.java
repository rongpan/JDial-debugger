
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import constraintfactory.ConstraintFactory;
import javaparser.simpleJavaLexer;
import javaparser.simpleJavaParser;
import sketchobj.core.FcnHeader;
import sketchobj.core.Function;
import sketchobj.core.SketchObject;
import sketchobj.core.TypePrimitive;
import sketchobj.expr.ExprConstInt;
import sketchobj.expr.Expression;
import sketchobj.stmts.Statement;
import sketchobj.stmts.StmtBlock;
import trace.ProgState;
import trace.Trace;
import visitor.EvalVisitor;

public class Test {

	@org.junit.Test
	public void test1() {
		Function f = ConstraintFactory.addConstFun(0, 5, new TypePrimitive(4));
		//System.out.println(f);
	}
	

	
	@org.junit.Test
	public void test3() {
		Statement s = ConstraintFactory.varArrayDecl("t", 5, new TypePrimitive(4));
		//System.out.println(s);
	}
	
	@org.junit.Test
	public void test4() {
		List<String> otherVars = new ArrayList<String>();
		otherVars.add("y");
		otherVars.add("z");
		Statement s = ConstraintFactory.recordState(0, otherVars);
		//System.out.println(s);
	}
	
	@org.junit.Test
	public void testReplaceConst() {
		ANTLRInputStream input = new ANTLRInputStream(
				"int largestGap(int[] a){ int max = 1; a[1] = 10; c = max++; int min = 100;  for(int i=0; i < a.Length; i++){ if(max < a[i]) max = a[i]; }return max-min;}");
		Function f = (Function) compile(input);
		Statement s = f.getBody();
	//	System.out.println(s);
		//System.out.println(ConstraintFactory.repalceConst(s));
	//	System.out.println(s);
	}
	@org.junit.Test
	public void testRecordStmt() {
		ANTLRInputStream input = new ANTLRInputStream(
				"int largestGap(int[] a){ int max = 1; a[1] = 10; c = max++; int min = 100;  for(int i=0; i < a.Length; i++){ if(max < a[i]) max = a[i]; }return max-min;}");
		Function f = (Function) compile(input);
		Statement s = f.getBody();
		ConstraintFactory.repalceConst(s);
		ConstraintFactory.addRecordStmt((StmtBlock) s);
	//	System.out.println(s);
	}
	
	@org.junit.Test
	public void testSimpleExample() throws InterruptedException{
		System.out.println();
		System.out.println("testSimpleExample:");
		ANTLRInputStream input = new ANTLRInputStream(
				"int SimpleJava(){ "
				+ "int a = 2; "
				+ "int b = a + 1; "
				+ "int c = a+ b; "
				+ "return c;}");
		Function root = (Function) compile(input);
	
		
		List<ProgState> traces = new ArrayList<ProgState>();
		// {line = 1, a = 2}
		Map<String, Integer> m1 = new HashMap<String, Integer>();
		m1.put("a", 2);
		ProgState state1 = new ProgState(1,m1);
		traces.add(state1);
		
		// {line = 2, a = 2, b = 5}
		Map<String,Integer> m2 = new HashMap<String, Integer>(m1);
		m2.put("b", 5);
		ProgState state2 = new ProgState(2,m2);
		traces.add(state2);
		
		// {line = 3, a = 2, b = 5, c = 7}		
		Map<String,Integer> m3 = new HashMap<String, Integer>(m2);
		m3.put("c", 7);
		ProgState state3 = new ProgState(3,m3);
		traces.add(state3);
		
		// correct: {line = 3, c = 8}
		Map<String,Integer> m4 = new HashMap<String, Integer>();
		m4.put("c", 8);
		//m4.put("b", 5);
		ProgState finalState = new ProgState(3, m4);
		

		Trace oriTrace = new Trace(traces);
		///////
		
		ConstraintFactory cf = new ConstraintFactory(oriTrace, finalState,  new FcnHeader(root.getName(),root.getReturnType(),root.getParames()));
		
		System.out.println(root);
		
		String script = cf.getScript(root.getBody());
		System.out.println(CallSketch.CallByString(script));
	}
	
	@org.junit.Test
	public void testSumUp() throws InterruptedException{
		System.out.println();
		System.out.println("testSumUp:");
		ANTLRInputStream input = new ANTLRInputStream(
				"int orig(int x){"
				+ "int t = 1; "
				+ "for(int i = x; i > 0; i--)"
				+ "{t = t + i;}"
				+ "return t;}");
		Function root = (Function) compile(input);
	
		
		List<ProgState> traces = new ArrayList<ProgState>();
		// {line = 1, t = 1}
		Map<String, Integer> m1 = new HashMap<String, Integer>();
		m1.put("t", 1);
		ProgState state1 = new ProgState(1,m1);
		traces.add(state1);
		
		// {line = 2, t = 1, i = 3}
		Map<String,Integer> m2 = new HashMap<String, Integer>(m1);
		m2.put("i", 3);
		ProgState state2 = new ProgState(2,m2);
		traces.add(state2);
		
		// {line = 3, t = 4, i = 3}		
		Map<String,Integer> m3 = new HashMap<String, Integer>();
		m3.put("t", 4);
		m3.put("i", 3);
		ProgState state3 = new ProgState(3,m3);
		traces.add(state3);
		
		// {line = 2, t = 4, i = 2}		
		Map<String,Integer> m4 = new HashMap<String, Integer>();
		m4.put("t", 4);
		m4.put("i", 2);
		ProgState state4 = new ProgState(2,m3);
		traces.add(state4);
		
		// {line = 3, t = 6, i = 2}		
		Map<String,Integer> m = new HashMap<String, Integer>();
		m.put("t", 4);
		m.put("i", 2);
		ProgState state = new ProgState(3,m);
		traces.add(state);
		
		// {line = 2, t = 6, i = 1}		
		m = new HashMap<String, Integer>();
		m.put("t", 6);
		m.put("i", 1);
		state = new ProgState(2,m);
		traces.add(state);
		
		
		// correct: {line = 2, t = 5, i = 1}
		Map<String,Integer> fm = new HashMap<String, Integer>();
		fm.put("t", 5);
		fm.put("i", 1);
		ProgState finalState = new ProgState(2, fm);
		

		Trace oriTrace = new Trace(traces);
		///////
		
		ConstraintFactory cf = new ConstraintFactory(oriTrace, finalState,  new FcnHeader(root.getName(),root.getReturnType(),root.getParames()),new ExprConstInt(3));
		
		System.out.println(root);
		
		String script = cf.getScript(root.getBody());
		System.out.println(CallSketch.CallByString(script));
	}

	@org.junit.Test
	public void testLargestGap() throws InterruptedException{
		System.out.println();
		System.out.println("testLargestGap:");
		ANTLRInputStream input = new ANTLRInputStream(
				"int largestGap(int[] a){"
				+ "int max = 100; "
				+ "int min = 0; "
				+ "for(int i=0; i < a.Length; i++){"
				+ 	"if(max < a[i]) "
				+ 		"max = a[i];"
				+ 	"if(min > a[i]) "
				+ 		"min = a[i];"
				+ "}"
				+ "return max-min;}");
		Function root = (Function) compile(input);
	
		
		List<ProgState> traces = new ArrayList<ProgState>();


		Map<String,Integer> m = new HashMap<String, Integer>();
		m.put("max", 100);
		ProgState state = new ProgState(1,m);
		traces.add(state);
		
		m = new HashMap<String, Integer>();
		m.put("max", 100);
		m.put("min", 0);
		state = new ProgState(2,m);
		traces.add(state);

		m = new HashMap<String, Integer>();
		m.put("max", 100);
		m.put("min", 0);
		m.put("i", 0);
		state = new ProgState(3,m);
		traces.add(state);
		
		m = new HashMap<String, Integer>();
		m.put("max", 100);
		m.put("min", 0);
		m.put("i", 0);
		state = new ProgState(4,m);
		traces.add(state);
		
		m = new HashMap<String, Integer>();
		m.put("max", 100);
		m.put("min", 0);
		m.put("i", 0);
		state = new ProgState(6,m);
		traces.add(state);
		
		m = new HashMap<String, Integer>();
		m.put("max", 100);
		m.put("min", 0);
		m.put("i", 1);
		state = new ProgState(3,m);
		traces.add(state);
		
		m = new HashMap<String, Integer>();
		m.put("max", 100);
		m.put("min", 0);
		m.put("i", 1);
		state = new ProgState(4,m);
		traces.add(state);
		
		m = new HashMap<String, Integer>();
		m.put("max", 100);
		m.put("min", 0);
		m.put("i", 1);
		state = new ProgState(6,m);
		traces.add(state);
		
		m = new HashMap<String, Integer>();
		m.put("max", 100);
		m.put("min", 0);
		m.put("i", 2);
		state = new ProgState(3,m);
		
		traces.add(state);
		// correct
		Map<String,Integer> fm = new HashMap<String, Integer>();
		fm.put("max", 4);
		fm.put("min", 1);
		fm.put("i", 2);
		ProgState finalState = new ProgState(2, fm);
		

		Trace oriTrace = new Trace(traces);
		///////
		
		List<Expression> parameters = new ArrayList<Expression>();
		parameters.add(new ExprConstInt(1));
		parameters.add(new ExprConstInt(4));
		parameters.add(new ExprConstInt(2));
		parameters.add(new ExprConstInt(7));
		
		ConstraintFactory cf = new ConstraintFactory(oriTrace, finalState,  new FcnHeader(root.getName(),root.getReturnType(),root.getParames()),parameters);
		
		System.out.println(root);
		
		String script = cf.getScript(root.getBody());
		System.out.println(CallSketch.CallByString(script));
	}
	
	public static SketchObject compile(ANTLRInputStream input) {
		simpleJavaLexer lexer = new simpleJavaLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		simpleJavaParser parser = new simpleJavaParser(tokens);
		ParseTree tree = parser.methodDeclaration();
		return new EvalVisitor().visit(tree);
	}
}
