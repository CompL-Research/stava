package handlers;

import es.ConditionalValue;
import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import main.CHATransform;
import soot.*;
import soot.jimple.Constant;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Targets;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class JInvokeStmtHandler {
	public static ConcurrentHashMap<SootMethod, List<Local> > nativeLocals = new ConcurrentHashMap<>();
	public static void handle(SootMethod m, Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		/*
		 * All method calls.
		 */
		JInvokeStmt stmt = (JInvokeStmt) u;
		InvokeExpr expr = stmt.getInvokeExpr();
		handleExpr(m, u, expr, ptg, summary);
	}

	private static int getSummarySize(Map<ObjectNode, EscapeStatus> summary)
	{
		return summary.toString().length();
	}

	public static void handleExpr(SootMethod m, Unit u, InvokeExpr expr, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
//		if(expr.getMethod().isJavaLibraryMethod()) {
//			return;
//		}
		/*
		 * Please note that expr.getMethod() is wrong. use cg.edgesOutOf() to get methods(). expr.getMethod() can also 
		 * point to functions which doesn't exist. This means that handleExpr should have a loop to 
		 */

		// System.out.println("Process expr: "+expr);
		CallGraph cg = Scene.v().getCallGraph();


		Iterator<Edge> iedges = cg.edgesOutOf(u);
		List<Value> args = expr.getArgs();

		List<Edge> edges = new ArrayList<>();
		if (!iedges.hasNext()) {
			iedges = CHATransform.getCHA().edgesOutOf(u);
		}
		
		// if (!iedges.hasNext()) {
		// 	edges.add(new Edge(m, u, expr.getMethod(), Kind.STATIC));
		// }

		while(iedges.hasNext()) {
			edges.add(iedges.next());
		}

		// System.out.println("Processing unit: "+u);
		// System.out.println("Edges: "+edges);

		// if (true)
		// 	return;

		/*
		 * special: only constructors and initializers
		 * static: static method calls
		 * virtual: all normal calls
		 */
// 		if (expr instanceof JSpecialInvokeExpr) {
// 			/*
// 			 * Example of JSpecialInvokeExpr:
// 			 * specialinvoke r0.<java.lang.Object: void<init>()>()
// 			 */
// 			JSpecialInvokeExpr invokeExpr = (JSpecialInvokeExpr) expr;
// 			Value base = invokeExpr.getBase();
// 			ConditionalValue cv = new ConditionalValue(invokeExpr.getMethod(), new ObjectNode(-1, ObjectType.parameter), true);
// //			System.out.println("CascadeCV "+cv.toString()+" on "+base.toString());
// 			ptg.cascadeCV((Local) base, cv, summary);
// //			System.out.println(summary.get(new ObjectNode(17, ObjectType.internal)));
// 		} else if (expr instanceof JStaticInvokeExpr) {
// 			/*
// 			 * Has no base.
// 			 * Example of JStaticInvokeExpr:
// 			 * staticinvoke <Test: void foo(int)>(0)
// 			 */

// //			JStaticInvokeExpr staticExpr = (JStaticInvokeExpr) expr;
// //			System.out.println("Example of JStaticInvokeExpr:");
// //			System.out.println(staticExpr);

// 		} else if (expr instanceof JVirtualInvokeExpr) {
// 			/*
// 			 * Example of JVirtualInvokeExpr:
// 			 * virtualinvoke $r0.<java.io.PrintStream: void println(int)>(i0)
// 			 */
// 			JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) expr;
// 			Value base = invokeExpr.getBase();
// 			ConditionalValue cv = new ConditionalValue(invokeExpr.getMethod(), new ObjectNode(-1, ObjectType.parameter), true);
// 			ptg.cascadeCV((Local) base, cv, summary);
// 			// System.out.println("Virtual call: "+u+" isNative; "+invokeExpr.getMethod().isNative()+" base: "+base);
// 		} else if (expr instanceof JInterfaceInvokeExpr) {
// 			/*
// 			 * Example of JVirtualInvokeExpr:
// 			 * virtualinvoke $r0.<java.io.PrintStream: void println(int)>(i0)
// 			 */
// 			JInterfaceInvokeExpr invokeExpr = (JInterfaceInvokeExpr) expr;
// 			Value base = invokeExpr.getBase();
// 			ConditionalValue cv = new ConditionalValue(invokeExpr.getMethod(), new ObjectNode(-1, ObjectType.parameter), true);
// 			ptg.cascadeCV((Local) base, cv, summary);
// 		// } else if (expr instanceof JDynamicInvokeExpr) {
// 		// 	throw new IllegalBCIException("JDynamicInvokeExpr");
// 		}else {
		// System.out.println("addEscapeState: "+this);
		// System.out.println("addEscapeState: "+this);
// 			System.err.println("Unidentified invoke expr: " + expr.toString());
// 			// CallGraph cg = Scene.v().getCallGraph();
// 			// Iterator<Edge> edges = cg.edgesOutOf(u);
// 			System.err.println("CG Empty: "+edges.hasNext());
// 			while (edges.hasNext()) {
// 				Edge edge = edges.next();
// 				System.err.println("Calling function: "+edge.tgt());
// 			}
// 			// throw new IllegalArgumentException(expr.toString());
// 		}
		if (expr instanceof JSpecialInvokeExpr) {
			edges = new ArrayList<>();
			edges.add(new Edge(m, u, expr.getMethod(), Kind.SPECIAL));	
		}
		if (expr instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) expr;
			Value base = invokeExpr.getBase();
			for(Edge edge: edges) {
				// Edge edge = edges.next();
				ConditionalValue cv = new ConditionalValue(edge.tgt(), new ObjectNode(-1, ObjectType.parameter), true);
				ptg.cascadeCV((Local) base, cv, summary);
			}
		}
		else if (expr instanceof JStaticInvokeExpr) {
		}
		else {
			System.err.println("Unidentified invoke expr: " + expr.toString());
			throw new IllegalArgumentException(expr.toString());
		}

		for(Edge edge: edges) {
			// Edge edge = edges.next();
			SootMethod method = edge.tgt();
			SootMethod srcMethod = edge.src();
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
								nativeLocals.putIfAbsent(srcMethod,new ArrayList<>());
								nativeLocals.get(srcMethod).add((Local)args.get(i));
							}
							else
								ptg.cascadeCV((Local) args.get(i), cv, summary);
						}
				}
			}
		}
	}
}