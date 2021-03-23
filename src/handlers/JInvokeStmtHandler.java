package handlers;

import es.ConditionalValue;
import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import soot.*;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Targets;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JInvokeStmtHandler {

	public static void handle(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		/*
		 * All method calls.
		 */
		JInvokeStmt stmt = (JInvokeStmt) u;
		InvokeExpr expr = stmt.getInvokeExpr();
		handleExpr(u, expr, ptg, summary);
	}

	private static int getSummarySize(Map<ObjectNode, EscapeStatus> summary)
	{
		return summary.toString().length();
	}

	public static void handleExpr(Unit u, InvokeExpr expr, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
//		if(expr.getMethod().isJavaLibraryMethod()) {
//			return;
//		}
		/*
		 * Please note that expr.getMethod() is wrong. use cg.edgesOutOf() to get methods(). expr.getMethod() can also 
		 * point to functions which doesn't exist. This means that handleExpr should have a loop to 
		 */

		/*
		 * special: only constructors and initializers
		 * static: static method calls
		 * virtual: all normal calls
		 */
		if (expr instanceof JSpecialInvokeExpr) {
			/*
			 * Example of JSpecialInvokeExpr:
			 * specialinvoke r0.<java.lang.Object: void<init>()>()
			 */
			JSpecialInvokeExpr invokeExpr = (JSpecialInvokeExpr) expr;
			Value base = invokeExpr.getBase();
			ConditionalValue cv = new ConditionalValue(invokeExpr.getMethod(), new ObjectNode(-1, ObjectType.parameter), true);
//			System.out.println("CascadeCV "+cv.toString()+" on "+base.toString());
			ptg.cascadeCV((Local) base, cv, summary);
//			System.out.println(summary.get(new ObjectNode(17, ObjectType.internal)));
		} else if (expr instanceof JStaticInvokeExpr) {
			/*
			 * Has no base.
			 * Example of JStaticInvokeExpr:
			 * staticinvoke <Test: void foo(int)>(0)
			 */

//			JStaticInvokeExpr staticExpr = (JStaticInvokeExpr) expr;
//			System.out.println("Example of JStaticInvokeExpr:");
//			System.out.println(staticExpr);

		} else if (expr instanceof JVirtualInvokeExpr) {
			/*
			 * Example of JVirtualInvokeExpr:
			 * virtualinvoke $r0.<java.io.PrintStream: void println(int)>(i0)
			 */
			JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) expr;
			Value base = invokeExpr.getBase();
			ConditionalValue cv = new ConditionalValue(invokeExpr.getMethod(), new ObjectNode(-1, ObjectType.parameter), true);
			ptg.cascadeCV((Local) base, cv, summary);
			// System.out.println("Virtual call: "+u+" isNative; "+invokeExpr.getMethod().isNative()+" base: "+base);
		} else if (expr instanceof JInterfaceInvokeExpr) {
			/*
			 * Example of JVirtualInvokeExpr:
			 * virtualinvoke $r0.<java.io.PrintStream: void println(int)>(i0)
			 */
			JInterfaceInvokeExpr invokeExpr = (JInterfaceInvokeExpr) expr;
			Value base = invokeExpr.getBase();
			ConditionalValue cv = new ConditionalValue(invokeExpr.getMethod(), new ObjectNode(-1, ObjectType.parameter), true);
			ptg.cascadeCV((Local) base, cv, summary);
		} else {
			System.out.println("Unidentified invoke expr: " + expr.toString());
			throw new IllegalArgumentException(expr.toString());
		}

		CallGraph cg = Scene.v().getCallGraph();

		// Iterator<MethodOrMethodContext> methods = new Targets(cg.edgesOutOf(u));

		// while(methods.hasNext())
		// {
		// 	SootMethod method = methods.next().method();
		// 	List<Value> args = expr.getArgs();
		// 	System.out.println("Method invoke: "+method +" from: "+u);
		// 	for (int i = 0; i < args.size(); i++) {
		// 		System.out.println(i);
		// 		Value arg = args.get(i);
		// 		if ((arg.getType() instanceof RefType) || (arg.getType() instanceof ArrayType) ) {
		// 			System.out.println(i);
		// 			if (arg instanceof Constant) continue;
		// 			System.out.println(i);
		// 			ObjectNode obj = new ObjectNode(i, ObjectType.parameter);
		// 			ConditionalValue cv = new ConditionalValue(method, obj, true);
		// 			ptg.cascadeCV((Local) args.get(i), cv, summary);
		// 		}
		// 	}
		// }

		Iterator<Edge> edges = cg.edgesOutOf(u);
		List<Value> args = expr.getArgs();

		while(edges.hasNext()) {
			Edge edge = edges.next();
			SootMethod method = edge.tgt();
			// System.out.println("Method: "+method + "isNative: "+method.isNative());
			boolean isNative = method.isNative();
			int paramCount = method.getParameterCount();

			for (int i = 0; i < paramCount; i++) {
				ObjectNode obj = new ObjectNode(i, ObjectType.parameter);
				ConditionalValue cv = new ConditionalValue(method, obj, true);
				
				if (edge.kind() == Kind.REFL_INVOKE)
					ptg.cascadeCV((Local) args.get(1), cv, summary);
				else if(edge.kind() == Kind.REFL_CONSTR_NEWINSTANCE)
					ptg.cascadeCV((Local) args.get(0), cv, summary);
				else {
					Value arg = args.get(i);
					if (arg.getType() instanceof RefType || arg.getType() instanceof ArrayType)
						if ( !(arg instanceof Constant) )	{		// Notice the not(!) 
							if (isNative) {
								System.out.println("Escaping: "+args.get(i));
								ptg.cascadeEscape((Local) args.get(i), summary);
							}
							else
								ptg.cascadeCV((Local) args.get(i), cv, summary);
						}
				}
			}
		}
	}
}