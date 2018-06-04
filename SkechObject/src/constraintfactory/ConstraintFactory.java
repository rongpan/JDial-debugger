package constraintfactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import jsonast.Trace;
import jsonast.Traces;
import org.apache.tools.ant.taskdefs.XSLTProcess;

import global.Global;
import sketchobj.core.*;
import sketchobj.core.Function.FcnType;
import sketchobj.expr.*;
import sketchobj.stmts.*;

public class ConstraintFactory {
	// TODO: repair arrayInit.replaceConst(), else statement, Expr.field, all
	// varDecl should be init now

	// map from coeff to linenumber; map from linenumber to string of statement
	public static Map<Integer, Integer> coeffIndex_to_Line = new HashMap<Integer, Integer>();
	public static Map<Integer, String> line_to_string = new HashMap<Integer, String>();

	// number of constant
	static int constNumber = 0;
	static Map<String, Set<Integer>> constMap = new HashMap<String, Set<Integer>>();
	static List<String> varList = new ArrayList<String>();
	static List<String> funcVarList = new ArrayList<String>();	
	static Map<String, Type> namesToType = new HashMap<String, Type>();
	static Map<Integer, Integer> constMapLine = new HashMap<Integer, Integer>();
	static List<Integer> noWeightCoeff = new ArrayList<Integer>();
	static Integer numLines = -1;

	//
	static Traces oriTrace;
	static Trace finalState;

	// static int finalCount;
	static FcnHeader fh;
	static int hitline = 0;
	static int hitnumber = 0;
	static int length = 5;
	static int originalLength = 5;
	static List<Expression> args = new ArrayList<>();
	public static int correctionIndex = 0;

	// configurations
	static boolean is_linearcombination = false;
	static int distance_type = 0;
	static int numberOfChange = 1;

	// specified range
	static boolean sign_limited_range = false;
	static List<Integer> repair_range = null;
	static Integer sign_distance = 0; // 0 : Hamming distance
										// 1 : edit distance
										// 2 : longest subsequence

	static Integer mod = 0; // 0: global minimization
							// 1: 1 line minimization
							// 2: 1 line fix before minimization
	
	// added 11/19 the manipulated variable
	static String final_var = null;

	public static List<ExternalFunction> externalFuncs = new ArrayList<ExternalFunction>();

	// added whether use prime mod
    public static boolean prime_mod;	
    public List<Function> otherFunctions;
    public static int coeffIndex;
    public static Set<Statement> dupStmt = new HashSet<>();
	
	// ------------ Construct method
    // added
    public ConstraintFactory(Traces oriTrace, Trace finalState, FcnHeader fh, List<Expression> args, Integer mod,
    		boolean prime_mod, List<Function> otherFunctions) {
		ConstraintFactory.fh = fh;
		ConstraintFactory.oriTrace = oriTrace;
		ConstraintFactory.finalState = finalState;
		ConstraintFactory.hitline = finalState.getLine();
		this.otherFunctions = otherFunctions;
		this.coeffIndex = 0;
		// added
		System.err.println("----------------------------------------7\n");
		System.err.println(finalState.toString());
		System.err.println(finalState.getLine());
		System.err.println("----------------------------------------7");
		ConstraintFactory.prime_mod = prime_mod;
		
		hitnumber = 0;
		for (int i = 0; i < oriTrace.getLength(); i++) {
			System.err.println(i + " getLine() " + oriTrace.getTraces().get(i).getLine());
			if (oriTrace.getTraces().get(i).getLine() == ConstraintFactory.hitline) {
				hitnumber++;
			}
		}
		System.err.println("hit humber is " + hitnumber);
		originalLength = oriTrace.getLength();
		length = originalLength * 2;

		constMap = new HashMap<String, Set<Integer>>();
		varList = new ArrayList<String>();
		funcVarList = new ArrayList<String>();

		this.args = args;
		ConstraintFactory.mod = mod;

	}

	public ConstraintFactory(Traces oriTrace, Trace finalState, FcnHeader fh, List<Expression> args) {
		this(oriTrace, finalState, fh, args, 0, false, new ArrayList<Function>());
	}

	public ConstraintFactory(Traces oriTrace, Trace finalState, FcnHeader fh) {
		this(oriTrace, finalState, fh, new ArrayList<Expression>());
		// this.args = args;
	}

	public ConstraintFactory(Traces oriTrace, Trace finalState, FcnHeader fh, Expression parameter) {
		this(oriTrace, finalState, fh);
		List<Expression> l = new ArrayList<Expression>();
		l.add(parameter);
		// this.args = l;
	}

	// set allowed ranges
	public void setRange(List<Integer> l) {
		ConstraintFactory.sign_limited_range = true;
		ConstraintFactory.repair_range = l;
	}

	// ------------ main function, generate Sketch script for code <source>
	/*
	 * // constance replacement public String getScript(Statement source) {
	 * 
	 * Statement s = source; Statement constFunDecls = null;
	 * 
	 * // extract info of external functions externalFuncs =
	 * s.extractExternalFuncs(externalFuncs); if (externalFuncs.size() > 0)
	 * System.out.println(externalFuncs.get(0).getName_Java());
	 * 
	 * buildContext((StmtBlock) source); // replace all constants in source code
	 * if (!ConstraintFactory.sign_limited_range) { //
	 * s.replaceLinearCombination(); constFunDecls =
	 * ConstraintFactory.replaceConst(s); } else { //
	 * s.replaceLinearCombination(ConstraintFactory.repair_range); constFunDecls
	 * = ConstraintFactory.replaceConst(s); }
	 * 
	 * // add record stmts to source code and collect vars info Map<String,
	 * Type> vars = ConstraintFactory.addRecordStmt((StmtBlock) s); List<String>
	 * varsNames = new ArrayList<String>(vars.keySet()); varList = varsNames;
	 * List<Type> varsTypes = new ArrayList<Type>(); for (int i = 0; i <
	 * varsNames.size(); i++) { varsTypes.add(vars.get(varsNames.get(i))); }
	 * 
	 * // add declare of <linehit> and <count> s = new StmtBlock(new
	 * StmtVarDecl(new TypePrimitive(4), "linehit", new ExprConstInt(0), 0), s);
	 * s = new StmtBlock(new StmtVarDecl(new TypePrimitive(4), "count", new
	 * ExprConstInt(-1), 0), s);
	 * 
	 * Function f = new Function(ConstraintFactory.fh, s);
	 * 
	 * List<Statement> stmts = new ArrayList<>();
	 * 
	 * // add declare of const functions stmts.add(constFunDecls);
	 * 
	 * // add line array stmts.add( new StmtBlock(varArrayDecl("line", length,
	 * new TypePrimitive(4)), varArrayDecls(varsNames, varsTypes)));
	 * 
	 * // add final state //
	 * System.out.println(finalState.getOrdered_locals().size()); for (String v
	 * : finalState.getOrdered_locals()) { stmts.add(new StmtVarDecl(new
	 * TypePrimitive(4), v + "final", new ExprConstInt(0), 0)); }
	 * 
	 * // add final count stmts.add(new StmtVarDecl(new TypePrimitive(4),
	 * "finalcount", new ExprConstInt(0), 0));
	 * 
	 * Statement block = new StmtBlock(stmts);
	 * 
	 * return block.toString() + "\n" + f.toString() + "\n" +
	 * constraintFunction().toString(); }
	 */

	// ------------ main function, generate Sketch script for code <source>
	// linear combination replace

	
	public String getScript_linearCombination(Statement source, List<Parameter> param)
	{
		Statement s = source;
		Statement coeffFunDecls = null;

		String resv_funcs = ReservedFuncs();

		System.out.println(source);

		// extract info of external functions
		externalFuncs = s.extractExternalFuncs(externalFuncs);
		if (externalFuncs.size() > 0)
			System.out.println(externalFuncs.get(0).getName_Java());

		buildContext((StmtBlock) source);
		System.out.println(source.toString_Context());
		// replace all constants in source code
		if (!ConstraintFactory.sign_limited_range) {
			coeffFunDecls = ConstraintFactory.replaceLinearCombination(s);
			// constFunDecls = ConstraintFactory.replaceConst(s);
		} else {
			// coeffFunDecls = ConstraintFactory.replaceLinearCombination(s,
			// ConstraintFactory.repair_range);
			// constFunDecls = ConstraintFactory.replaceConst(s);
		}

		Statement globalVarDecls = getGlobalDecl();

		// add record stmts to source code and collect vars info
		Map<String, Type> vars = ConstraintFactory.addRecordStmt((StmtBlock) s);

		for(Parameter p:param)
		{
			vars.put(p.getName(), p.getType());
		}

		ConstraintFactory.namesToType = vars;
		List<String> varsNames = new ArrayList<String>(vars.keySet());
		varList = varsNames;

		List<Type> varsTypes = new ArrayList<Type>();
		for (int i = 0; i < varsNames.size(); i++) {
			varsTypes.add(vars.get(varsNames.get(i)));
		}

		// add declare of <linehit> and <count>
		s = new StmtBlock(new StmtVarDecl(new TypePrimitive(4), "linehit", new ExprConstInt(0), 0), s);

		Function f = new Function(ConstraintFactory.fh, s);

		List<Statement> stmts = new ArrayList<>();

		stmts.add(globalVarDecls);

		// add declare of const functions
		stmts.add(coeffFunDecls);

		// add line array
		stmts.add(
				new StmtBlock(varArrayDecl("line", length, new TypePrimitive(4)), varArrayDecls(varsNames, varsTypes,
						f.getName())));

		// add final state
		// System.out.println(finalState.getOrdered_locals().size());
		for (String v : finalState.getOrdered_locals()) {
			// added
			if (ConstraintFactory.prime_mod)
				stmts.add(new StmtVarDecl(new TypePrimitive(4), v + "@1final", new ExprConstInt(0), 0));
			else 
				stmts.add(new StmtVarDecl(new TypePrimitive(4), v + "final", new ExprConstInt(0), 0));
		}

		// add final count
		stmts.add(new StmtVarDecl(new TypePrimitive(4), "finalcount", new ExprConstInt(0), 0));
		stmts.add(new StmtVarDecl(new TypePrimitive(4), "count", new ExprConstInt(-1), 0));

		Statement block = new StmtBlock(stmts);

		String tmp1 = block.toString() + "\n";
		String tmp2 = f.toString() + "\n";
		// args of getAugFunctions() need change
		String tmp3 = getAugFunctions() + constraintFunction_linearCombination().toString();
		return tmp1 + tmp2 + tmp3;
	}

	public String getScript_linearCombination(Statement source) {

		// a script consists of three parts:
		// 1) coeff decl and guess functions decl
		// 2) the interpreted source function with statements recording program
		// states and expressions rewrote
		// 3) the constrain function which compute the cost and search for the
		// least cost rewrite

		// added
		System.err.println("prime_mod is "+ prime_mod);
		Statement s = source;
		Statement coeffFunDecls = null;

		String resv_funcs = ReservedFuncs();

		System.out.println(source);

		// extract info of external functions
		externalFuncs = s.extractExternalFuncs(externalFuncs);
		if (externalFuncs.size() > 0)
			System.out.println(externalFuncs.get(0).getName_Java());

		buildContext((StmtBlock) source);
		System.out.println(source.toString_Context());
		// replace all constants in source code
		if (!ConstraintFactory.sign_limited_range) {
			System.err.println("s1 is: ");
			System.err.println(s);
			coeffFunDecls = ConstraintFactory.replaceLinearCombination(s);
			System.err.println("s2 is: ");
			System.err.println(s);
			// constFunDecls = ConstraintFactory.replaceConst(s);
		} else {
		}

		Statement globalVarDecls = getGlobalDecl();

		// add record stmts to source code and collect vars info
		Global.curFunc = ConstraintFactory.fh.getName();
		
		Map<String, Type> vars = ConstraintFactory.addRecordStmt((StmtBlock) s);
		System.err.println("s3 is: ");
		System.err.println(s);
		ConstraintFactory.namesToType = vars;
		List<String> varsNames = new ArrayList<String>(vars.keySet());
		varList = new ArrayList(varsNames);
		for(int i = 0;i<vars.keySet().size();i++)
		{
			funcVarList.add(Global.curFunc);
		}
		List<Type> varsTypes = new ArrayList<Type>();
		for (int i = 0; i < varsNames.size(); i++) {
			varsTypes.add(vars.get(varsNames.get(i)));
		}

		// add declare of <linehit> and <count>
		s = new StmtBlock(new StmtVarDecl(new TypePrimitive(4), "linehit", new ExprConstInt(0), 0), s);

		Function f = new Function(ConstraintFactory.fh, s);
		
		List<Statement> stmts = new ArrayList<>();

		// 11/28 handle other functions
		Statement coeffFunDecls1 = null;
		StringBuilder st = new StringBuilder();
		//Statement globalVarDecls1 = null;
		Statement declVars = null;
		for (int i = 0; i < this.otherFunctions.size(); i++) {
			Function cur = otherFunctions.get(i);
			Global.curFunc = cur.getName();
			FcnHeader fh1 = new FcnHeader(cur.getName(), cur.getReturnType(), cur.getParames());
			
			Statement s1 = cur.getBody();
			
			System.out.println(s1);

			buildContext((StmtBlock) s1);
			System.out.println(s1.toString_Context());
			// replace all constants in source code
			
			if (!ConstraintFactory.sign_limited_range) {
				coeffFunDecls1 = ConstraintFactory.replaceLinearCombination(s1);
				// constFunDecls = ConstraintFactory.replaceConst(s);
			} else {
			}

			// add record stmts to source code and collect vars info
			Map<String, Type> vars1 = ConstraintFactory.addRecordStmt((StmtBlock) s1);
			//ConstraintFactory.namesToType.putAll(vars1);
			//varList.addAll(vars1.keySet());
			List<String> varsNames1 = new ArrayList<String>(vars1.keySet());
			varList.addAll(varsNames1);
			for(int h = 0;h<vars1.keySet().size();h++)
			{
				funcVarList.add(Global.curFunc);
			}
			for (int j = 0; j < varsNames.size(); j++) {
				varsTypes.add(vars.get(varsNames.get(j)));
			}

			// add declare of <linehit> and <count>
			s1 = new StmtBlock(new StmtVarDecl(new TypePrimitive(4), "linehit", new ExprConstInt(0), 0), s1);

			Function f1 = new Function(fh1, s1);
			
			
			st.append(f1.toString());
			
			declVars = varArrayDecls(varsNames1, varsTypes,cur.getName());
		}
		
		stmts.add(getGlobalDecl());
		//stmts.add(globalVarDecls);

		// add declare of const functions
		System.err.println("coeffFunDecls is " + coeffFunDecls.toString());
		stmts.add(coeffFunDecls);
		if (coeffFunDecls1 != null)
			stmts.add(coeffFunDecls1);

		stmts.add(
				new StmtBlock(varArrayDecl("line", length, new TypePrimitive(4)), varArrayDecls(varsNames, varsTypes,
						f.getName())));
		/*if (Global.prime_mod){
			for (int i = 0; i < varsNames.size(); i++) {
				varsNames.set(i, varsNames.get(i) + "2");
			}
			stmts.add(varArrayDecls(varsNames, varsTypes, f.getName()));
		}*/
		
		
		if (declVars != null)
			stmts.add(declVars);
		
		if(global.Global.rec_mod)
			stmts.add(varArrayDecl("stack", length, new TypePrimitive(4)));
		
		// add final state
		// System.out.println(finalState.getOrdered_locals().size());
		for (String v : finalState.getOrdered_locals()) {
			// added
			if (ConstraintFactory.prime_mod) {
				for (int i : Global.primes)
					stmts.add(new StmtVarDecl(new TypePrimitive(4), v + Integer.toString(i), new ExprConstInt(0), 0));
			}
			else
				stmts.add(new StmtVarDecl(new TypePrimitive(4), v + "final", new ExprConstInt(0), 0));
		}

		// add final count
		stmts.add(new StmtVarDecl(new TypePrimitive(4), "finalcount", new ExprConstInt(0), 0));
		if (Global.prime_mod) {
			stmts.add(new StmtVarDecl(new TypePrimitive(4), "count", new ExprConstInt(0), 0));
		} else {
			stmts.add(new StmtVarDecl(new TypePrimitive(4), "count", new ExprConstInt(-1), 0));
		}
		
		if(global.Global.rec_mod)
			stmts.add(new StmtVarDecl(new TypePrimitive(4), "funcCount", new ExprConstInt(-1), 0));
		
		Statement block = new StmtBlock(stmts);
		
		String tmp1 = block.toString();
		String tmp2 = f.toString();
		// args of getAugFunctions() need change
		String tmp3;
		if (Global.rec_mod) {
			tmp3 = getAugFunctions() + constraintFunction_linearCombination().toString();
		} else {
			tmp3 = constraintFunction().toString();
		}
		return tmp1 + tmp2 + st + tmp3;
	}
	
	/* old approach 2018/04
	public String getScript_linearCombination(Statement source) {

		// a script consists of three parts:
		// 1) coeff decl and guess functions decl
		// 2) the interpreted source function with statements recording program
		// states and expressions rewrote
		// 3) the constrain function which compute the cost and search for the
		// least cost rewrite

		// added
		System.err.println("prime_mod is "+ prime_mod);
		Statement s = source;
		Statement coeffFunDecls = null;

		String resv_funcs = ReservedFuncs();

		System.out.println(source);

		// extract info of external functions
		externalFuncs = s.extractExternalFuncs(externalFuncs);
		if (externalFuncs.size() > 0)
			System.out.println(externalFuncs.get(0).getName_Java());

		buildContext((StmtBlock) source);
		System.out.println(source.toString_Context());
		// replace all constants in source code
		if (!ConstraintFactory.sign_limited_range) {
			coeffFunDecls = ConstraintFactory.replaceLinearCombination(s);
			// constFunDecls = ConstraintFactory.replaceConst(s);
		} else {
			// coeffFunDecls = ConstraintFactory.replaceLinearCombination(s,
			// ConstraintFactory.repair_range);
			// constFunDecls = ConstraintFactory.replaceConst(s);
		}

		Statement globalVarDecls = getGlobalDecl();

		// add record stmts to source code and collect vars info
		Global.curFunc = ConstraintFactory.fh.getName();
		
		Map<String, Type> vars = ConstraintFactory.addRecordStmt((StmtBlock) s);
		ConstraintFactory.namesToType = vars;
		List<String> varsNames = new ArrayList<String>(vars.keySet());
		varList = new ArrayList(varsNames);
		for(int i = 0;i<vars.keySet().size();i++)
		{
			funcVarList.add(Global.curFunc);
		}
		List<Type> varsTypes = new ArrayList<Type>();
		for (int i = 0; i < varsNames.size(); i++) {
			varsTypes.add(vars.get(varsNames.get(i)));
		}

		// add declare of <linehit> and <count>
		s = new StmtBlock(new StmtVarDecl(new TypePrimitive(4), "linehit", new ExprConstInt(0), 0), s);

		Function f = new Function(ConstraintFactory.fh, s);

		List<Statement> stmts = new ArrayList<>();

		// 11/28 handle other functions
		Statement coeffFunDecls1 = null;
		StringBuilder st = new StringBuilder();
		//Statement globalVarDecls1 = null;
		Statement declVars = null;
		for (int i = 0; i < this.otherFunctions.size(); i++) {
			Function cur = otherFunctions.get(i);
			Global.curFunc = cur.getName();
			FcnHeader fh1 = new FcnHeader(cur.getName(), cur.getReturnType(), cur.getParames());
			
			Statement s1 = cur.getBody();
			
			System.out.println(s1);


			buildContext((StmtBlock) s1);
			System.out.println(s1.toString_Context());
			// replace all constants in source code
			
			if (!ConstraintFactory.sign_limited_range) {
				coeffFunDecls1 = ConstraintFactory.replaceLinearCombination(s1);
				// constFunDecls = ConstraintFactory.replaceConst(s);
			} else {
				// coeffFunDecls = ConstraintFactory.replaceLinearCombination(s,
				// ConstraintFactory.repair_range);
				// constFunDecls = ConstraintFactory.replaceConst(s);
			}

			//globalVarDecls1 = getGlobalDecl();

			// add record stmts to source code and collect vars info
			Map<String, Type> vars1 = ConstraintFactory.addRecordStmt((StmtBlock) s1);
			//ConstraintFactory.namesToType.putAll(vars1);
			//varList.addAll(vars1.keySet());
			List<String> varsNames1 = new ArrayList<String>(vars1.keySet());
			varList.addAll(varsNames1);
			for(int h = 0;h<vars1.keySet().size();h++)
			{
				funcVarList.add(Global.curFunc);
			}
			for (int j = 0; j < varsNames.size(); j++) {
				varsTypes.add(vars.get(varsNames.get(j)));
			}

			// add declare of <linehit> and <count>
			s1 = new StmtBlock(new StmtVarDecl(new TypePrimitive(4), "linehit", new ExprConstInt(0), 0), s1);

			Function f1 = new Function(fh1, s1);
			
			
			st.append(f1.toString());
			
			declVars = varArrayDecls(varsNames1, varsTypes,cur.getName());
		}
		
		stmts.add(getGlobalDecl());
		//stmts.add(globalVarDecls);

		// add declare of const functions
		System.err.println("coeffFunDecls is " + coeffFunDecls.toString());
		stmts.add(coeffFunDecls);
		if (coeffFunDecls1 != null)
			stmts.add(coeffFunDecls1);

		// add line array
		//stmts.add(
		//		new StmtBlock(varArrayDecl("line", length, new TypePrimitive(4)), varArrayDecls(varsNames, varsTypes,
		//				f1)));
		stmts.add(
				new StmtBlock(varArrayDecl("line", length, new TypePrimitive(4)), varArrayDecls(varsNames, varsTypes,
						f.getName())));
		
		if (declVars != null)
			stmts.add(declVars);
		
		if(global.Global.rec_mod)
			stmts.add(varArrayDecl("stack", length, new TypePrimitive(4)));
		
		// add final state
		// System.out.println(finalState.getOrdered_locals().size());
		for (String v : finalState.getOrdered_locals()) {
			// added
			if (ConstraintFactory.prime_mod)
				stmts.add(new StmtVarDecl(new TypePrimitive(4), v + "@1final", new ExprConstInt(0), 0));
			else
				stmts.add(new StmtVarDecl(new TypePrimitive(4), v + "final", new ExprConstInt(0), 0));
		}

		// add final count
		stmts.add(new StmtVarDecl(new TypePrimitive(4), "finalcount", new ExprConstInt(0), 0));
		stmts.add(new StmtVarDecl(new TypePrimitive(4), "count", new ExprConstInt(-1), 0));
		
		if(global.Global.rec_mod)
			stmts.add(new StmtVarDecl(new TypePrimitive(4), "funcCount", new ExprConstInt(-1), 0));

		
		
		Statement block = new StmtBlock(stmts);

		
		String tmp1 = block.toString();
		String tmp2 = f.toString();
		// args of getAugFunctions() need change
		String tmp3 = getAugFunctions() + constraintFunction_linearCombination().toString();
		System.err.println("tmp1 is: ");
		System.out.println(tmp1);
		System.err.println("tmp2 is: ");
		System.out.println(tmp2);
		System.err.println("tmp3 is: ");
		System.out.println(tmp3);
		return tmp1 + tmp2 + st + tmp3;
	}*/

	private Statement getGlobalDecl() {
		StmtBlock result = new StmtBlock();
		List<Integer> appeared = new ArrayList<Integer>();
		for (int line : ConstraintFactory.coeffIndex_to_Line.values()) {
			if (appeared.contains(line))
				continue;
			result.addStmt(new StmtVarDecl(new TypePrimitive(1), "line" + line + "change", new ExprConstInt(0), 0));
			appeared.add(line);
		}
		ConstraintFactory.numLines = appeared.size();
		return result;
	}

	// genearting reserved function such as abs min max
	private String ReservedFuncs() {
		// TODO Auto-generated method stub
		return null;
	}

	
	private static Statement replaceLinearCombination(Statement s) {
		List<Statement> list = new ArrayList<Statement>();
		Stack<SketchObject> stmtStack = new Stack<SketchObject>();
		//int coeffIndex = 0;
		stmtStack.push(s);
		//int level = 0;
		while (!stmtStack.empty()) {
			SketchObject target = stmtStack.pop();
			ConstData data = null;
			/*System.err.println("coeffIndex is: " + coeffIndex);
			System.err.println("target is: ");
			System.err.println(target);
			System.err.println("----------------------------------");*/
			if (ConstraintFactory.sign_limited_range) {
				data = target.replaceLinearCombination(coeffIndex, ConstraintFactory.repair_range);
			} else {
				data = target.replaceLinearCombination(coeffIndex);//@2-------added here
			}

			if (data.getType() != null) {
				while (coeffIndex <= data.getPrimaryCoeffIndex()) {
					list.add(coeffChangeDecl(coeffIndex, new TypePrimitive(1)));
					list.add(new StmtFunDecl(addCoeffFun(coeffIndex, 1, data.getType())));
					coeffIndex_to_Line.put(coeffIndex, data.getOriline());
					coeffIndex++;
				}
				if (data.getLiveVarsIndexSet() != null) {
					for (int ii : data.getLiveVarsIndexSet()) {
						list.add(coeffChangeDecl(ii, new TypePrimitive(1)));
						list.add(new StmtFunDecl(addCoeffFun(ii, 0, data.getType())));
						coeffIndex_to_Line.put(ii, data.getOriline());
					}

				}
				coeffIndex = data.getIndex();
				if (!data.isIfLC()) {
					ConstraintFactory.noWeightCoeff.add(coeffIndex - 2);
					list.add(coeffChangeDecl(coeffIndex - 2, new TypePrimitive(1)));//bit coeff0change = ??;
					list.add(new StmtFunDecl(addCoeffFun(coeffIndex - 2, 0, data.getType())));//Coeff0()
					list.add(coeffChangeDecl(coeffIndex - 1, new TypePrimitive(4)));
					list.add(new StmtFunDecl(addLCConstFun(coeffIndex - 1, data.getType())));
					coeffIndex_to_Line.put(coeffIndex - 1, data.getOriline());
					coeffIndex_to_Line.put(coeffIndex - 2, data.getOriline());
				}
			}
			coeffIndex = data.getIndex();
			pushAll(stmtStack, data.getChildren());
		}
		constNumber = coeffIndex;

		// ConstructLineToString() is only for debugging purpose, it has no effect on final output
		System.err.println(s.ConstructLineToString(line_to_string));
		// added
		if (Global.prime_mod)
			dupPrimes(s);
		//System.out.println("end of replaceLinearCombination()");
		//System.out.println(ConstraintFactory.line_to_string);
		//System.out.println(list);
		return new StmtBlock(list);
	}
	
	private static void dupPrimes(Statement s) {
		if (s instanceof StmtBlock) {
			for (int i = 0; i < ((StmtBlock) s).stmts.size(); i++) {
				System.err.println("size is : " + ((StmtBlock) s).stmts.size());
				System.err.println("i is : " + i);
				Statement si = ((StmtBlock) s).stmts.get(i);
				if (Global.facts.get(si.getLineNumber()).size() != 0)
					continue;
				if (si instanceof StmtAssign) {
					((StmtBlock) s).stmts.remove(i);
					Context con = si.getPostctx();
					Map<String, Type> allvars = con.getAllVars();
					dupVars(allvars);
					con.setCurrentVars(allvars);
					for (int p : Global.primes) {
						StmtAssign newSt = ((StmtAssign) si).clone();
						Expression lhs = newSt.getLHS();
						changeExpr(lhs, p);
						Expression rhs = newSt.getRHS();
						changeExpr(rhs, p);
						rhs = modExpr(rhs, p);
						newSt.setLhs(lhs);
						newSt.setRhs(rhs);
						newSt.setPrectx(con);
						newSt.setPostctx(con);
						((StmtBlock) s).stmts.add(i, newSt);
						i++;
						if (p != Global.primes[Global.primes.length - 1])
							ConstraintFactory.dupStmt.add(newSt);
					}
					i--;
					ConstraintFactory.dupStmt.add(si);
					continue;
				}
				if (si instanceof StmtVarDecl) {
					((StmtBlock) s).stmts.remove(i);
					
					String finalVar = finalState.getOrdered_locals().get(0);
					System.err.println("final var is "+ finalVar);
					System.err.println("var name is " + ((StmtVarDecl) si).getName(0));
					if (((StmtVarDecl) si).getName(0).equals(finalVar)) {
						
						Context con = si.getPostctx();
						Map<String, Type> allvars = con.getAllVars();
						dupVars(allvars);
						con.setCurrentVars(allvars);
						for (int p : Global.primes) {
							
							Expression init = ((StmtVarDecl) si).clone().getInit(0);
							changeExpr(init, p);
							init = modExpr(init, p);
							StmtAssign assign = new StmtAssign(new ExprVar(finalVar + Integer.toString(p)), 
									init, si.getLineNumber());
							assign.buildContext(con, 0);
							((StmtBlock) s).stmts.add(i, assign);
							i++;
							if (p != Global.primes[Global.primes.length - 1])
								ConstraintFactory.dupStmt.add(assign);
						}
						i--;
						ConstraintFactory.dupStmt.add(si);
						continue;
					} else {
						Context con = si.getPostctx();
						Map<String, Type> allvars = con.getAllVars();
						dupVars(allvars);
						con.setCurrentVars(allvars);
						for (int p : Global.primes) {
							StmtVarDecl newSt = ((StmtVarDecl) si).clone();
							newSt.SetName(newSt.getName(0) + Integer.toString(p));
							Expression init = newSt.getInit(0);
							changeExpr(init, p);
							init = modExpr(init, p);
							newSt.setInit(0, init);
							newSt.setPrectx(con);
							newSt.setPostctx(con);
							((StmtBlock) s).stmts.add(i, newSt);
							i++;
							if (p != Global.primes[Global.primes.length - 1])
								ConstraintFactory.dupStmt.add(newSt);
						}
						i--;
						ConstraintFactory.dupStmt.add(si);
						continue;
					}
				}
				dupPrimes(si);
			}
			return;
		}
		if (s instanceof StmtDoWhile) {
			dupPrimes(((StmtDoWhile) s).getBody());
			return;
		}
		if (s instanceof StmtFor) {
			dupPrimes(((StmtFor) s).getBody());
			return;
		}
		if (s instanceof StmtIfThen) {
			if (((StmtIfThen) s).getCons() instanceof StmtBlock) {
				System.err.println("cons is a block");
			} else {
				System.err.println("not a block");
			}
			dupPrimes(((StmtIfThen) s).getCons());
			dupPrimes(((StmtIfThen) s).getAlt());
			return;
		}
		/*if (s instanceof StmtVarDecl) {
			System.err.println("into StmtVarDecl");
			list.add(s);
			list.add(s);
			s = new StmtBlock(list);
			System.err.println(s);
			return;
		}*/
		if (s instanceof StmtWhile) {
			dupPrimes(((StmtWhile) s).getBody());
			return;
		}
		if (s instanceof StmtReturn) {
			((StmtReturn) s).setValue(new ExprConstInt(0));
		}
	}
	
	private static void dupVars(Map<String, Type> allvars) {
		Map<String, Type> dup = new HashMap<>();
		for (Map.Entry<String, Type> entry : allvars.entrySet()) {
			for (int i : Global.primes) {
				Global.replicatedVars.add(entry.getKey() + Integer.toString(i));
				dup.put(entry.getKey() + Integer.toString(i), entry.getValue());
			}
		}
		allvars.putAll(dup);
	}
	
	private static void changeExpr(Expression e, int p) {
		if (e instanceof ExprBinary) {
			changeExpr(((ExprBinary) e).getLeft(), p);
			changeExpr(((ExprBinary) e).getRight(), p);
			return;
		}
		if (e instanceof ExprVar) {
			((ExprVar) e).setName(((ExprVar) e).getName() + Integer.toString(p));
		}
	}

	private static ExprBinary modExpr(Expression e, int p) {
		Expression left = e;
		int op = ExprBinary.BINOP_MOD;
		Expression right = new ExprConstInt(p);
		return new ExprBinary(op, left, right, left.lineNumber);
	}
	
	private static Function addLCConstFun(int i, Type type) {
		Expression condition_2 = new ExprStar();
		StmtReturn return_2 = new StmtReturn(new ExprConstInt(0), 0);
		StmtReturn return_3 = new StmtReturn(new ExprVar("coeff" + i + "change"), 0);

		Statement ifst_2 = new StmtIfThen(condition_2, return_2, null, 0);
		StmtBlock sb = new StmtBlock();
		sb.addStmt(ifst_2);
		sb.addStmt(return_3);
		return new Function("Coeff" + i, type, new ArrayList<Parameter>(), sb, FcnType.Static);
	}

	private static Function addCoeffFun(int index, int value, Type type) {
		Expression condition = new ExprBinary(new ExprVar("coeff" + index + "change"), "==", new ExprConstInt(0), 0);
		StmtReturn return_1 = new StmtReturn(new ExprConstInt(value), 0);
		Expression condition_2 = new ExprStar();
		StmtReturn return_2 = new StmtReturn(new ExprConstInt(1 - value), 0);
		Statement ifst = new StmtIfThen(condition, return_1, null, 0);
		Statement ifst_2 = new StmtIfThen(condition_2, return_2, null, 0);
		StmtReturn return_3 = new StmtReturn(new ExprConstInt(-1), 0);
		StmtBlock sb = new StmtBlock();
		sb.addStmt(ifst);
		sb.addStmt(ifst_2);
		sb.addStmt(return_3);
		return new Function("Coeff" + index, type, new ArrayList<Parameter>(), sb, FcnType.Static);
	}

	private static Statement coeffChangeDecl(int index, TypePrimitive typePrimitive) {
		return new StmtVarDecl(typePrimitive, "coeff" + index + "change", new ExprStar(), 0);
	}

	/*
	 * private static Statement replaceLinearCombination(Statement s,
	 * List<Integer> repair_range2) { List<Statement> list = new
	 * ArrayList<Statement>(); Stack<SketchObject> stmtStack = new
	 * Stack<SketchObject>(); int index = 0; stmtStack.push(s); while
	 * (!stmtStack.empty()) { SketchObject target = stmtStack.pop(); ConstData
	 * data = null; if (ConstraintFactory.sign_limited_range) { data =
	 * target.replaceLinearCombination(index, ConstraintFactory.repair_range); }
	 * else { data = target.replaceLinearCombination(index); } if
	 * (data.getType() != null) { while (index <= data.getPrimaryCoeffIndex()) {
	 * list.add(coeffChangeDecl(index, new TypePrimitive(1))); list.add(new
	 * StmtFunDecl(addCoeffFun(index, 1, data.getType()))); index++; } if
	 * (data.getLiveVarsIndexSet() != null) { for (int ii :
	 * data.getLiveVarsIndexSet()) { list.add(coeffChangeDecl(ii, new
	 * TypePrimitive(1))); list.add(new StmtFunDecl(addCoeffFun(ii, 0,
	 * data.getType()))); }
	 * 
	 * } index = data.getIndex(); list.add(coeffChangeDecl(index - 1, new
	 * TypePrimitive(4))); list.add(new StmtFunDecl(addLCConstFun(index - 1,
	 * data.getType()))); } index = data.getIndex(); pushAll(stmtStack,
	 * data.getChildren()); } constNumber = index; //System.out.println(s);
	 * return new StmtBlock(list); }
	 */

	// ------------ Auxiliary functions

	static public Statement constChangeDecl(int index, Type t) {
		return new StmtVarDecl(t, "const" + index + "change", new ExprStar(), 0);
	}

	static public Statement constChangeDecls(int number, Type t) {
		StmtBlock result = new StmtBlock();
		for (int i = 0; i < number; i++) {
			result.addStmt(new StmtVarDecl(t, "const" + i + "change", new ExprStar(), 0));
		}
		return result;
	}
	
	// added 11/19
	static public void get_final_var_2dArray() {
		int bound = originalLength;
		String v = ConstraintFactory.final_var;
		List<Integer> arrayInit = new ArrayList<>();
		List<Integer> level = new ArrayList<>();
		for (int i = 0; i < bound; i++) {
			level.add(oriTrace.getTraces().get(i).getRstack().getFrams().size());
			if (oriTrace.getTraces().get(i).getOrdered_locals().contains(v)) {
				if (oriTrace.getTraces().get(i).getLocals().find(v).getType() == 0) {
					arrayInit.add(oriTrace.getTraces().get(i).getLocals().find(v).getValue());
				}
			} else {
				// TODO check if int can be null in Sketch
				arrayInit.add(0);
			}
		}
		System.err.println("final_var is " + v + "----------------------------------------9");
		int minLevel = 100;
		int maxLevel = 0;
		for (int i = 0; i < arrayInit.size(); i++) {
			int curLevel = level.get(i);
			if (curLevel < minLevel)
				minLevel = curLevel;
			if (curLevel > maxLevel)
				maxLevel = curLevel;
		}
		int numEachLevel [] = new int[maxLevel - minLevel + 1];
		
		for (int i = 0; i < arrayInit.size(); i++) {
			int curLevel = level.get(i);
			numEachLevel[curLevel - minLevel]++;
		}
		int max_num = 0;
		for (int i = 0; i < maxLevel - minLevel + 1; i++) {
			if (numEachLevel[i] > max_num)
				max_num = numEachLevel[i];
			System.err.println("numEachLevel is " + numEachLevel[i]);
		}
		
		int matrix [][] = new int[maxLevel - minLevel + 1][max_num];
		int lineIndex [] = new int[maxLevel - minLevel + 1];
		for (int i = 0; i < arrayInit.size(); i++) {
			matrix[level.get(i) - minLevel][lineIndex[level.get(i) - minLevel]] =  arrayInit.get(i);
			lineIndex[level.get(i) - minLevel]++;
		}
		
		for (int i = 0; i < matrix.length; i++) {
		    for (int j = 0; j < matrix[i].length; j++) {
		        System.out.print(matrix[i][j] + " ");
		    }
		    System.out.println();
		}
		
		for (int i = 0; i < arrayInit.size(); i++) {
			System.err.println("each value is " + arrayInit.get(i));
			System.err.println("each level is " + level.get(i));
		}
	}
	
	private Function constraintFunction_linearCombination() {
		List<Statement> stmts = new ArrayList<Statement>();

		int bound = (length < originalLength) ? length : originalLength;

		// added get stack trace
		List<Expression> arrayStack = new ArrayList<>();
		int firstDepth = 0;
		firstDepth = oriTrace.getTraces().get(0).getRstack().getFrams().size();
		for (int i = 0; i < bound; i++) {
			int depth = oriTrace.getTraces().get(i).getRstack().getFrams().size();
			arrayStack.add(new ExprConstInt(depth - firstDepth));
		}
		stmts.add(new StmtVarDecl(new TypeArray(new TypePrimitive(4), new ExprConstInt(originalLength)),
				"oringianlStackArray", new ExprArrayInit(arrayStack), 0));
		
		// load original trace to array
		for (int h = 0;h < varList.size();h++) {
			String v = varList.get(h);
			String funcV = funcVarList.get(h);
			List<Expression> arrayInit = new ArrayList<>();
			for (int i = 0; i < bound; i++) {
				if (oriTrace.getTraces().get(i).getOrdered_locals().contains(v)) {
					if (oriTrace.getTraces().get(i).getLocals().find(v).getType() == 0)
						arrayInit.add(
								new ExprConstInt((int) oriTrace.getTraces().get(i).getLocals().find(v).getValue()));
				} else {
					// TODO check if int can be null in Sketch
					// added need to improve
					if (Global.prime_mod) {
						String ori = v.substring(0, v.length() - 1);
						if (oriTrace.getTraces().get(i).getOrdered_locals().contains(ori)) {
							if (oriTrace.getTraces().get(i).getLocals().find(ori).getType() == 0)
								arrayInit.add(
										new ExprConstInt((int) oriTrace.getTraces().get(i).getLocals().find(ori).getValue()));
						}
						else
							arrayInit.add(new ExprConstInt(0));
					}
					else
						arrayInit.add(new ExprConstInt(0));
				}
			}
			for (int i = bound; i < originalLength; i++) {
				arrayInit.add(new ExprConstInt(0));
			}
			if (ConstraintFactory.prime_mod) {
			    stmts.add(new StmtVarDecl(new TypeArray(new TypePrimitive(4), new ExprConstInt(originalLength)),
					"@3oringianl" + v + "Array", new ExprArrayInit(arrayInit), 0));
			    
			}
			else
				stmts.add(new StmtVarDecl(new TypeArray(new TypePrimitive(4), new ExprConstInt(originalLength)),
					"oringianl" + funcV + v + "Array", new ExprArrayInit(arrayInit), 0));
		}




		List<Expression> arrayInit = new ArrayList<>();
		for (int i = 0; i < bound; i++) {
			arrayInit.add(new ExprConstInt((int) oriTrace.getTraces().get(i).getLine()));
		}
		for (int i = bound; i < originalLength; i++) {
			arrayInit.add(new ExprConstInt(0));
		}
		stmts.add(new StmtVarDecl(new TypeArray(new TypePrimitive(4), new ExprConstInt(originalLength)),
				"oringianllineArray", new ExprArrayInit(arrayInit), 0));

		for (String v : finalState.getOrdered_locals()) {
			// added 11/19
			ConstraintFactory.final_var = v;
			/*if (ConstraintFactory.prime_mod)
				stmts.add(new StmtVarDecl(new TypePrimitive(4), "@4correctFinal_" + v,
					new ExprConstInt(finalState.getLocals().find(v).getValue()), 0));
			else*/
			stmts.add(new StmtVarDecl(new TypePrimitive(4), "correctFinal_" + v,
					new ExprConstInt(finalState.getLocals().find(v).getValue()), 0));
		}

		// f(args), call the target function
		stmts.add(new StmtExpr(new ExprFunCall(fh.getName(), args, fh.getName()), 0));

		// distance initialization
		stmts.add(new StmtVarDecl(new TypePrimitive(4), "SyntacticDistance", new ExprConstInt(0), 0));
		stmts.add(new StmtVarDecl(new TypePrimitive(4), "SemanticDistance", new ExprConstInt(0), 0));

		// syntactic distance
		// added 1125
		//editDistance(5,3,"ori","tar");
		
		
//		if (sign_distance == 0)
//			stmts.add(HammingDistance(bound));
		
		if (Global.rec_mod) {
			if (sign_distance == 0)
				stmts.add(semanticDistance(bound));
		}
		
		List<Expression> ex = new ArrayList<>();
		ex.add(new ExprString("oringianlStackArray"));
		ex.add(new ExprString("stackArray"));
		stmts.add(new StmtAssign(new ExprVar("SemanticDistance"),
				new ExprFunCall("getDistance",ex,"getDistance"),
				1, 1));

		// semantic distance
		StmtBlock editsb = new StmtBlock();
		for (int i = 0; i < constNumber; i++) {
			// if (!ConstraintFactory.noWeightCoeff.contains(i))
			editsb.addStmt(new StmtAssign(new ExprVar("SyntacticDistance"), new ExprVar("coeff" + i + "change"), 1, 1));
		}
		stmts.add(editsb);

		// hard constrain
		for (String v : finalState.getOrdered_locals()) {
			if (ConstraintFactory.prime_mod) {
				//stmts.add(new StmtAssert(
				//	new ExprBinary(new ExprVar(v + "@5final"), "==", new ExprVar("correctFinal_" + v), 0)));
				ExprBinary rightExpr = new ExprBinary(new ExprVar("correctFinal_" + v), "%", new ExprConstInt(2),0);
				stmts.add(new StmtAssert(
						new ExprBinary(new ExprVar(v + "2"), "==", rightExpr, 0)));
			}
			else
				stmts.add(new StmtAssert(
						new ExprBinary(new ExprVar(v + "final"), "==", new ExprVar("correctFinal_" + v), 0)));
		}
		//assertion--------added

		if (mod == 1) {
			stmts.add(oneLineConstraint());
		}
		// constrain on # of change
		Expression sumOfConstxchange = new ExprVar("const" + 0 + "change");
		// minimize cost statement
		stmts.add(new StmtMinimize(new ExprVar("SemanticDistance+5*SyntacticDistance"), true));

		// stmts.add(new StmtMinimize(new ExprVar("HammingDistance"), true));
		
		// added 11/19
		ConstraintFactory.get_final_var_2dArray();

		return new Function("Constraint", new TypeVoid(), new ArrayList<Parameter>(), new StmtBlock(stmts),
				FcnType.Harness);
	}

	private Statement oneLineConstraint() {
		StmtBlock result = new StmtBlock();
		Map<Integer, Expression> assign = new HashMap<Integer, Expression>();
		List<Integer> appeared = new ArrayList<Integer>();
		for (int line : ConstraintFactory.coeffIndex_to_Line.values()) {
			if (appeared.contains(line))
				continue;
			assign.put(line, null);
			appeared.add(line);
		}

		for (int coeff : ConstraintFactory.coeffIndex_to_Line.keySet()) {
			int line = ConstraintFactory.coeffIndex_to_Line.get(coeff);
			Expression old = assign.get(line);
			if (old == null)
				old = new ExprBinary(new ExprVar("coeff" + coeff + "change"), "!=", new ExprConstInt(0), -1);
			else
				old = new ExprBinary(old, "||",
						new ExprBinary(new ExprVar("coeff" + coeff + "change"), "!=", new ExprConstInt(0), -1), -1);
			assign.remove(line);
			assign.put(line, old);
		}
		Expression sum = new ExprConstInt(0);
		for (int line : appeared) {
			sum = new ExprBinary(sum, "+", new ExprVar("line" + line + "change"), -1);
			result.addStmt(new StmtAssign(new ExprVar("line" + line + "change"), assign.get(line), -1));
		}
		result.addStmt(new StmtAssert(new ExprBinary(sum, "==", new ExprConstInt(1), -1)));
		return result;
	}

	private Statement semanticDistance(Integer bound) {
		List<Statement> forBody = new ArrayList<Statement>();
		for (int i = 0;i<varList.size();i++)
		{
			String v = varList.get(i);
			String funcV = funcVarList.get(i);
			
			ExprBinary expBinary1 = new ExprBinary(new ExprArrayRange(funcV + v + "Array", "i", 0), "!=",
					new ExprArrayRange("oringianl" + funcV+ v + "Array", "i", 0), 0);
			ExprBinary expBinary2 = new ExprBinary(new ExprArrayRange("stackArray", "i", 0), "!=",
					new ExprArrayRange("oringianlStackArray", "i", 0), 0);
			
			StmtAssign sa = new StmtAssign(new ExprVar("SemanticDistance"),
					new ExprBinary(expBinary1, "||",
							expBinary2, 0),
					1, 1);

			
			forBody.add(sa);
		}
		
		StmtBlock sb = new StmtBlock(forBody);
		StmtIfThen ifth = new StmtIfThen(new ExprBinary(new ExprString("i"), "<=", new ExprString("count"), 0),
				sb , null);
		
		
		Statement forinit = new StmtVarDecl(new TypePrimitive(4), "i", new ExprConstInt(0), 0);
		Expression forcon = new ExprBinary(new ExprVar("i"), "<", new ExprConstInt(bound), 0);
		Statement forupdate = new StmtExpr(new ExprUnary(5, new ExprVar("i"), 0), 0);

		return new StmtFor(forinit, forcon, forupdate, ifth, false, 0);
	}

	
	private Statement HammingDistance(Integer bound) {
		List<Statement> forBody = new ArrayList<Statement>();
		for (String v : varList) {
			if (namesToType.get(v) instanceof TypeArray)
				continue;
			if (constMap.containsKey(v)) {
				List<Expression> subCondition = new ArrayList<Expression>();
				for (Integer indexOfv : constMap.get(v)) {
					subCondition.add(new ExprBinary(new ExprVar("const" + (indexOfv - 1) + "change"), "==",
							new ExprConstInt(0), 0));
				}
				Expression ifCondition;
				ifCondition = subCondition.get(0);
				if (subCondition.size() > 1) {
					for (int i = 1; i < subCondition.size(); i++) {
						ifCondition = new ExprBinary(ifCondition, "&&", subCondition.get(i), 0);
					}
				}
				forBody.add(
						new StmtIfThen(ifCondition,
								new StmtAssign(new ExprVar("SemanticDistance"),
										new ExprBinary(new ExprArrayRange(v + "Array", "i", 0), "!=",
												new ExprArrayRange("oringianl" + v + "Array", "i", 0), 0),
										1, 1),
								null, 0));

			} else {
				forBody.add(new StmtAssign(new ExprVar("SemanticDistance"),
						new ExprBinary(new ExprArrayRange(v + "Array", "i", 0), "!=",
								new ExprArrayRange("oringianl" + v + "Array", "i", 0), 0),
						1, 1));
			}

		}

		forBody.add(
				new StmtAssign(new ExprVar("SemanticDistance"), new ExprBinary(new ExprArrayRange("lineArray", "i", 0),
						"!=", new ExprArrayRange("oringianllineArray", "i", 0), 0), 1, 1));
		Statement forinit = new StmtVarDecl(new TypePrimitive(4), "i", new ExprConstInt(0), 0);
		Expression forcon = new ExprBinary(new ExprVar("i"), "<", new ExprConstInt(bound), 0);
		Statement forupdate = new StmtExpr(new ExprUnary(5, new ExprVar("i"), 0), 0);

		// ----------- out of range
		List<Statement> out_forBody = new ArrayList<Statement>();
		for (String v : varList) {
			if (constMap.containsKey(v)) {
				List<Expression> subCondition = new ArrayList<Expression>();
				for (Integer indexOfv : constMap.get(v)) {
					subCondition.add(new ExprBinary(new ExprVar("const" + (indexOfv - 1) + "change"), "==",
							new ExprConstInt(0), 0));
				}
				Expression ifCondition;
				ifCondition = subCondition.get(0);
				if (subCondition.size() > 1) {
					for (int i = 1; i < subCondition.size(); i++) {
						ifCondition = new ExprBinary(ifCondition, "&&", subCondition.get(i), 0);
					}
				}
				out_forBody.add(new StmtIfThen(ifCondition,
						new StmtAssign(new ExprVar("SemanticDistance"), new ExprConstInt(1), 1, 1), null, 0));

			} else {
				out_forBody.add(new StmtAssign(new ExprVar("SemanticDistance"), new ExprConstInt(1), 1, 1));
			}

		}

		out_forBody.add(new StmtAssign(new ExprVar("SemanticDistance"), new ExprConstInt(1), 1, 1));
		Statement out_forinit = new StmtVarDecl(new TypePrimitive(4), "i", new ExprVar("count"), 0);
		Expression out_forcon = new ExprBinary(new ExprVar("i"), "<", new ExprConstInt(correctionIndex), 0);
		Statement out_forupdate = new StmtExpr(new ExprUnary(5, new ExprVar("i"), 0), 0);

		return new StmtFor(forinit, forcon, forupdate, new StmtBlock(forBody), false, 0);
		// return new StmtBlock( new StmtFor(forinit, forcon, forupdate, new
		// StmtBlock(forBody), false, 0),new StmtAssign(new
		// ExprVar("SemanticDistance"), new ExprBinary(new ExprBinary(new
		// ExprConstInt(correctionIndex), "-", new ExprVar("count")), "*", new
		// ExprConstInt(varList.size())), 1, 1));
	}

	private String getAugFunctions(){
		StringBuilder result = new StringBuilder();
		result.append("int getMin(int a, int b, int c)\n" +
			"{\n" +
			    "if(a <= b)\n" +
			    "{\n" +
			        "if(a <= c)\n" +
			        "{\n" +
			            "return a;\n" +
			        "}\n" +
			        "else\n" +
			        "{\n" +
			            "return c;\n" +
			        "}\n" +
			    "}\n" +
			    "else\n" +
			    "{\n" + 
			        "if(b <= c)\n" +
			        "{\n" +
			            "return b;\n" +
			        "}\n" +
			        "else\n" +
			        "{\n" +
			            "return c;\n" +
			        "}\n" +
			    "}\n" +
			"}\n");
		result.append("int abs(int a, int b)\n" +
			"{\n" +
			    "int c = (a - b);\n" +
			    "if(c > 0)\n" +
			    "{\n" +
			        "return c;\n" +
			    "}\n" +
			    "else\n" +
			    "{\n" +
			        "return -c;\n" +
			    "}\n" +
			"}\n");
		result.append("int getDistance(int[" + originalLength + "] ori, int[" + 
			length + "] tar)\n" +
			"{\n" +   
			    "int n = " + Integer.toString(originalLength) + ";\n" +
			    "int m =" + Integer.toString(length) + ";\n" +
			    "int[m+1][n+1] f;\n" +
			    
			    "for(int i = 0;i<= n;i++)\n" +
			    "{\n" +
			        "f[i][0] = i;\n" +
			    "}\n" +
			    
			    "for(int i = 0;i<= m;i++)\n" +
			    "{\n" +
			        "f[0][i] = i;\n" +
			    "}\n" +
			
			    
			    "for(int j = 1;j<=m;j++)\n" +
			    "{\n" +
			        "if (tar[j-1] == 0)\n" +
			        "{\n" +
			            "return f[n][j-1];\n" +
			        "}\n" +
			        "for(int i = 1; i<=n;i++)\n" +
			        "{\n" +
			            "if(ori[i-1] == tar[j-1])\n" +
			            "{\n" +   
			                "f[i][j] = f[i-1][j-1];\n" +
			            "}\n" +
			            "else\n" +
			            "{\n" +   
			                "int add = f[i][j-1] + 1;\n" +
			                "int del = f[i-1][j] + 1;\n" +
			                "int rep = f[i-1][j-1] + 1;\n" + 
			                "f[i][j] = getMin(add, del, rep);\n" +
			            "}\n" +
			        "}\n" +
			    "}\n" +
			    
			    "return f[n][m];\n" +
			"}\n");
		return result.toString();
	}
	
	/* unfinished editDistance, don't think necessary, add string directly instead
	private Function editDistance(int len1, int len2, String name1, String name2) {
		List<Parameter> listP = new ArrayList<>();
		listP.add(new Parameter(new TypeArray(new TypePrimitive(TypePrimitive.TYPE_INT), new ExprConstInt(len1)),
				name1, Parameter.IN, false));
		listP.add(new Parameter(new TypeArray(new TypePrimitive(TypePrimitive.TYPE_INT), new ExprConstInt(len2)),
				name2, Parameter.IN, false));
		
		List<Statement> fnBody = new ArrayList<Statement>();
		
		Statement n = new StmtVarDecl(new TypePrimitive(4), "n", new ExprConstInt(len1), 0);
		fnBody.add(n);
		Statement m = new StmtVarDecl(new TypePrimitive(4), "m", new ExprConstInt(len2), 0);
		fnBody.add(m);
		
		Statement f = new StmtVarDecl(new TypeArray(new TypeArray(new TypePrimitive(4), new ExprConstInt(len2+1)),
				new ExprConstInt(len1+1)), "f", null, 0);
		fnBody.add(f);
		
		Statement forinit = new StmtVarDecl(new TypePrimitive(4), "i", new ExprConstInt(0), 0);
		Expression forcon = new ExprBinary(new ExprVar("i"), "<=", new ExprVar("n"), 0);
		Statement forupdate = new StmtExpr(new ExprUnary(5, new ExprVar("i"), 0), 0);
		
		Statement forBody = new StmtAssign(new ExprVar("f[i][0]"), );
		
		forBody.add(
				new StmtAssign(new ExprVar("SemanticDistance"), new ExprBinary(new ExprArrayRange("lineArray", "i", 0),
						"!=", new ExprArrayRange("oringianllineArray", "i", 0), 0), 1, 1));		
				
				
		Function fn = new Function ("editDistance", new TypePrimitive(TypePrimitive.TYPE_INT), listP,
				new StmtBlock(fnBody));
		
		System.err.println(fn.toString());
		return fn;
	}*/
	
	public Function constraintFunction() {
		List<Statement> stmts = new ArrayList<Statement>();

		int bound = (length < originalLength) ? length : originalLength;

		for (String v : varList) {
			if (ConstraintFactory.namesToType.get(v) instanceof TypeArray)
				continue;
			List<Expression> arrayInit = new ArrayList<>();
			boolean vIsOriginal = false;
			for (int i = 0; i < bound; i++) {
				if (oriTrace.getTraces().get(i).getOrdered_locals().contains(v)) {
					vIsOriginal = true;
					if (oriTrace.getTraces().get(i).getLocals().find(v).getType() == 0)
						arrayInit.add(
								new ExprConstInt((int) oriTrace.getTraces().get(i).getLocals().find(v).getValue()));
				} else {
					// TODO check if int can be null in Sketch
					arrayInit.add(new ExprConstInt(0));
				}
			}
			for (int i = bound; i < originalLength; i++) {
				arrayInit.add(new ExprConstInt(0));
			}
			if (vIsOriginal)
				stmts.add(new StmtVarDecl(new TypeArray(new TypePrimitive(4), new ExprConstInt(originalLength)),
					"oringianl" + v + "Array", new ExprArrayInit(arrayInit), 0));
		}

		for (String v : finalState.getOrdered_locals()) {
			if (finalState.getLocals().find(v) != null)
				stmts.add(new StmtVarDecl(new TypePrimitive(4), "correctFinal_" + v,
						new ExprConstInt(finalState.getLocals().find(v).getValue()), 0));
		}

		// f(args)
		stmts.add(new StmtExpr(new ExprFunCall(fh.getName(), args, fh.getName()), 0));

		// TODO int distance = |finalcount-originalLength|;
		stmts.add(new StmtVarDecl(new TypePrimitive(4), "SyntacticDistance", new ExprConstInt(0), 0));
		stmts.add(new StmtVarDecl(new TypePrimitive(4), "SemanticDistance", new ExprConstInt(0), 0));

		List<Statement> forBody = new ArrayList<Statement>();
		for (String v : varList) {
			if (constMap.containsKey(v)) {
				List<Expression> subCondition = new ArrayList<Expression>();
				for (Integer indexOfv : constMap.get(v)) {
					subCondition.add(new ExprBinary(new ExprVar("const" + (indexOfv - 1) + "change"), "==",
							new ExprConstInt(0), 0));
				}
				Expression ifCondition;
				ifCondition = subCondition.get(0);
				if (subCondition.size() > 1) {
					for (int i = 1; i < subCondition.size(); i++) {
						ifCondition = new ExprBinary(ifCondition, "&&", subCondition.get(i), 0);
					}
				}
				forBody.add(
						new StmtIfThen(ifCondition,
								new StmtAssign(new ExprVar("SyntacticDistance"),
										new ExprBinary(new ExprArrayRange(v + "Array", "i", 0), "!=",
												new ExprArrayRange("oringianl" + v + "Array", "i", 0), 0),
										1, 1),
								null, 0));

			} else {
				if (Global.prime_mod) {
					if (!Global.replicatedVars.contains(v))
						forBody.add(genSemDisPrime(v));
				} 
				else
					forBody.add(new StmtAssign(new ExprVar("SyntacticDistance"),
						new ExprBinary(new ExprArrayRange(fh.getName() + v + "Array", "i", 0), "!=",
								new ExprArrayRange("oringianl" + v + "Array", "i", 0), 0),
						1, 1));
			}

		}
		Statement forinit = new StmtVarDecl(new TypePrimitive(4), "i", new ExprConstInt(0), 0);
		Expression forcon = new ExprBinary(new ExprVar("i"), "<", new ExprConstInt(bound), 0);
		Statement forupdate = new StmtExpr(new ExprUnary(5, new ExprVar("i"), 0), 0);
		stmts.add(new StmtFor(forinit, forcon, forupdate, new StmtBlock(forBody), false, 0));
		
		// semantic distance
		StmtBlock editsb = new StmtBlock();
		for (int i = 0; i < constNumber; i++) {
			// if (!ConstraintFactory.noWeightCoeff.contains(i))
			editsb.addStmt(new StmtAssign(new ExprVar("SyntacticDistance"), new ExprVar("coeff" + i + "change"), 1, 1));
		}
		stmts.add(editsb);

		// hard constrain
		for (String v : finalState.getOrdered_locals()) {
			if (ConstraintFactory.prime_mod) {
				for (int i : Global.primes) {
					String num = Integer.toString(i);
					stmts.add(new StmtAssert( new ExprBinary(new ExprVar(v + num), "==", 
							new ExprVar("correctFinal_" + v + " % " + num), 0)));
				}
				//stmts.add(new StmtAssert(this.genAssertPrime(v)));
			}
			else
				stmts.add(new StmtAssert(
						new ExprBinary(new ExprVar(v + "final"), "==", new ExprVar("correctFinal_" + v), 0)));
		}
		//assertion--------added

		if (mod == 1) {
			stmts.add(oneLineConstraint());
		}
		// constrain on # of change
		Expression sumOfConstxchange = new ExprVar("const" + 0 + "change");
		// minimize cost statement
		stmts.add(new StmtMinimize(new ExprVar("SemanticDistance+5*SyntacticDistance"), true));

		// stmts.add(new StmtMinimize(new ExprVar("HammingDistance"), true));
		
		// added 11/19
		ConstraintFactory.get_final_var_2dArray();

		return new Function("Constraint", new TypeVoid(), new ArrayList<Parameter>(), new StmtBlock(stmts),
				FcnType.Harness);
		
		/*for (String v : finalState.getOrdered_locals()) {
			stmts.add(new StmtAssert(
					new ExprBinary(new ExprVar(v + "final"), "==", new ExprVar("correctFinal_" + v), 0)));
		}

		Expression sumOfConstxchange = new ExprVar("const" + 0 + "change");
		for (int i = 1; i < constMap.size(); i++)
			sumOfConstxchange = new ExprBinary(sumOfConstxchange, "+", new ExprVar("const" + i + "change"), 0);
		stmts.add(new StmtAssert(new ExprBinary(sumOfConstxchange, "==", new ExprConstInt(numberOfChange), 0)));

		stmts.add(new StmtMinimize(new ExprVar("SyntacticDistance"), true));

		return new Function("Constrain", new TypeVoid(), new ArrayList<Parameter>(), new StmtBlock(stmts),
				FcnType.Harness);*/
	}
	
	private Statement genSemDisPrime(String v){
		StringBuilder tmp = new StringBuilder();
		for (int i : Global.primes) {
			if (i == Global.primes[0]) {
				tmp.append("((" + fh.getName() + v + Integer.toString(i) + "Array[i]) != (");
				tmp.append("oringianl" + v + "Array[i] % " + Integer.toString(i) + "))");
			} else {
				tmp.append("||\n((" + fh.getName() + v + Integer.toString(i) + "Array[i]) != (");
				tmp.append("oringianl" + v + "Array[i] % " + Integer.toString(i) + "))");
			}
		}
		Statement res = new StmtAssign(new ExprVar("SemanticDistance"),
				new ExprString(tmp.toString()), 1, 1);
		return res;
	}

	private Expression genAssertPrime(String v){
		StringBuilder tmp = new StringBuilder();
		for (int i : Global.primes) {
			String num = Integer.toString(i);
			if (i == Global.primes[0])
				tmp.append("(" + v + num + " == " + "correctFinal_" + v + " % " + num + ")");
			else 
				tmp.append("&& \n " + "(" +  v + num + " == " + "correctFinal_" + v + " % " + num + ")");
		}
		return new ExprString(tmp.toString());
	}
	
	static public Statement constChangeDecl(int number) {
		return constChangeDecls(number, new TypePrimitive(4));
	}

	static public Statement varArrayDecl(String name, int length, Type type) {
		Type t = new TypeArray(type, new ExprConstInt(length));
		return new StmtVarDecl(t, name + "Array", null, 0);
	}

	// added function name
	static public StmtBlock varArrayDecls(List<String> names, List<Type> types, String funcName) {
		List<Statement> stmts = new ArrayList<Statement>();
		for (int i = 0; i < names.size(); i++) {
			if (types.get(i) instanceof TypeArray)
				continue;
			Type tarray = new TypeArray(types.get(i), new ExprConstInt(length));

			List<Expression> arrayinit = new ArrayList<>();
			for (int j = 0; j < length; j++) {
				arrayinit.add(new ExprConstInt(0));
			}

			stmts.add(new StmtVarDecl(tarray, funcName + names.get(i) + "Array", new ExprArrayInit(arrayinit), 0));
		}
		return new StmtBlock(stmts);
	}

	static public Function addConstFun(int index, int ori, Type t) {
		Expression condition = new ExprBinary(new ExprVar("const" + index + "change"), "==", new ExprConstInt(1), 0);
		StmtReturn return_1 = new StmtReturn(new ExprStar(), 0);
		StmtReturn return_2 = new StmtReturn(new ExprConstInt(ori), 0);
		Statement ifst = new StmtIfThen(condition, return_1, return_2, 0);

		return new Function("Const" + index, t, new ArrayList<Parameter>(), ifst, FcnType.Static);
	}

	static public Statement recordState(int lineNumber, Map<String, Type> allVars) {
		List<String> Vars = new ArrayList<String>(allVars.keySet());
		StmtBlock result = new StmtBlock();
		// count ++
		result.addStmt(new StmtExpr(new ExprUnary(5, new ExprVar("count"), 0), 0));
		// varToUpdateArray[count] = varToUpdate;
		if(global.Global.rec_mod)
			result.addStmt(
				new StmtAssign(
						new ExprArrayRange(new ExprVar("stackArray"),
								new ExprArrayRange.RangeLen(new ExprVar("count"), null), 0),
						new ExprVar("funcCount"), 0));

		
		result.addStmt(
				new StmtAssign(
						new ExprArrayRange(new ExprVar("lineArray"),
								new ExprArrayRange.RangeLen(new ExprVar("count"), null), 0),
						new ExprConstInt(lineNumber), 0));

		for (int h = 0;h<Vars.size();h++)
		{
			String s = Vars.get(h);
			if (Global.prime_mod) {
				if (!Global.replicatedVars.contains(s))
					continue;
			}
			if (allVars.get(s) instanceof TypeArray)
				continue;
			result.addStmt(new StmtAssign(new ExprArrayRange(new ExprVar(Global.curFunc + s + "Array"),
					new ExprArrayRange.RangeLen(new ExprVar("count"), null), 0), new ExprVar(s), 0));
		}
		if (lineNumber == hitline) {
			result.addStmt(new StmtExpr(new ExprUnary(5, new ExprVar("linehit"), 0), 0));
			List<Statement> consStmts = new ArrayList<>();
			for (String v : finalState.getOrdered_locals()) {
				if (allVars.get(v) instanceof TypeArray)
					continue;
				consStmts.add(new StmtAssign(new ExprVar(v + "final"), new ExprVar(v), 0));
			}
			consStmts.add(new StmtAssign(new ExprVar("finalcount"), new ExprVar("count"), 0));
			if (ConstraintFactory.fh.getReturnType() instanceof TypeVoid) {
				consStmts.add(new StmtReturn(0));
			}
			consStmts.add(new StmtReturn(new ExprConstInt(0), 0));
			Statement cons = new StmtBlock(consStmts);
			// added
			
			Statement iflinehit = new StmtIfThen(
					new ExprBinary(new ExprVar("linehit"), "==", new ExprString("??"), 0),
					cons, null, 0);
			//added 11/26
			
//			Statement iflinehit = new StmtIfThen(
//					new ExprBinary(new ExprVar("linehit"), "==", new ExprConstInt(1), 0),
//					cons, null, 0);
//			
			//Statement iflinehit = new StmtIfThen(
			//		new ExprBinary(new ExprVar("linehit"), "==", new ExprConstInt(ConstraintFactory.hitnumber), 0),
			//		cons, null, 0);
			result.addStmt(iflinehit);
		}
		return result;
	}

	static public Statement replaceConst(Statement source) {
		List<Statement> list = new ArrayList<Statement>();
		Stack<SketchObject> stmtStack = new Stack<SketchObject>();
		int index = 0;
		stmtStack.push(source);
		while (!stmtStack.empty()) {
			SketchObject target = stmtStack.pop();
			ConstData data = null;
			if (ConstraintFactory.sign_limited_range) {
				data = target.replaceConst(index, ConstraintFactory.repair_range);
			} else {
				data = target.replaceConst(index);
			}
			if (data.getType() != null) {
				addToConstMap(data);
				addToConstMapLine(data);
				list.add(constChangeDecl(index, new TypePrimitive(1)));
				list.add(new StmtFunDecl(addConstFun(index, data.getValue(), data.getType())));
			}
			index = data.getIndex();
			pushAll(stmtStack, data.getChildren());
		}
		return new StmtBlock(list);
	}

	private static void addToConstMapLine(ConstData data) {
		constMapLine.put(data.getIndex(), data.getOriline());
	}

	private static void addToConstMap(ConstData data) {
		if (constMap.containsKey(data.getName())) {
			Set<Integer> s = constMap.get(data.getName());
			s.add(data.getIndex());
		} else {
			Set<Integer> s = new HashSet<Integer>();
			s.add(data.getIndex());
			constMap.put(data.getName(), s);
		}
	}

	static public void buildContext(StmtBlock sb) {
		sb.buildContext(new Context(), 0);
	}

	static public Map<String, Type> addRecordStmt(StmtBlock sorce) {
		return sorce.addRecordStmt(null, 0, new HashMap<String, Type>());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static public void pushAll(Stack s, List l) {
		for (int i = l.size() - 1; i >= 0; i--) {
			s.push(l.get(i));
		}
	}

	public static Map<Integer, Integer> getconstMapLine() {
		return constMapLine;
	}
}
