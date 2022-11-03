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

import analyser.StaticAnalyser;

import java.util.*;

public class JInvokeStmtHandler {
	public static ConcurrentHashMap<SootMethod, List<Local> > nativeLocals = new ConcurrentHashMap<>();

	public static ArrayList<String> whitelistedNatives = new ArrayList<> (
		Arrays.asList("<java.lang.System: arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V>")
	);

	public static ArrayList<String> blacklistedNatives = new ArrayList<> (
		Arrays.asList("<sun.misc.Unsafe: putObject(Ljava/lang/Object;JLjava/lang/Object;)V>",
						"<sun.misc.Unsafe: putObjectVolatile(Ljava/lang/Object;JLjava/lang/Object;)V>",
						"<sun.misc.Unsafe: compareAndSwapObject(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z>")
	);
	

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

		// System.out.println("Process expr: "+expr+" of unit: "+u);
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
		// if (expr instanceof JSpecialInvokeExpr) {
		if (edges.size() == 0) {
			// edges = new ArrayList<>();
			System.out.println("Empty edges: "+expr+", function incoming edges: "+cg.edgesInto(m).hasNext()+
								" Method: "+m.getBytecodeSignature());
			edges.add(new Edge(m, u, expr.getMethod(), Kind.SPECIAL));	
		}
		
		// System.out.println("Edges: "+edges);

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

			// Recursion to first process the method
            // if not already processed
            if (!StaticAnalyser.methodsProcessed.contains(method)) {
				// PRIYAM - Is correct?
                StaticAnalyser.processMethod(method.getActiveBody());
            }

			// System.out.println("Method: "+method + "isNative: "+method.isNative());
			boolean isNative = method.isNative();
			boolean iswhiteListed = !blacklistedNatives.contains(method.getBytecodeSignature());
			if (isNative) {
				System.out.println("Native Method: "+method.getBytecodeSignature()+" WhiteList: "+iswhiteListed);
			}
			int paramCount = method.getParameterCount();

			Map<ObjectNode, Set<ObjectNode>> paramMapping = new HashMap<ObjectNode,Set<ObjectNode>>();
			for (int i = 0; i < paramCount; i++) {
				ObjectNode obj = new ObjectNode(i, ObjectType.parameter);
				ConditionalValue cv = new ConditionalValue(method, obj, true);
				
				if (edge.kind() == Kind.REFL_INVOKE) {
					ptg.cascadeCV((Local) args.get(1), cv, summary);
					paramMapping.put(obj, ptg.vars.get((Local) args.get(1)));
				}
				else if(edge.kind() == Kind.REFL_CONSTR_NEWINSTANCE) {
					ptg.cascadeCV((Local) args.get(0), cv, summary);
                    paramMapping.put(obj, ptg.vars.get((Local) args.get(0)));
				}
				else {
					Value arg = args.get(i);
					if (arg.getType() instanceof RefType || arg.getType() instanceof ArrayType)
						if ( !(arg instanceof Constant) )	{		// Notice the not(!) 
							if (isNative && !iswhiteListed) {
								System.out.println("Escaping: "+args.get(i));
								ptg.cascadeEscape((Local) args.get(i), summary);
								nativeLocals.putIfAbsent(srcMethod,new ArrayList<>());
								nativeLocals.get(srcMethod).add((Local)args.get(i));
							}
							else
								ptg.cascadeCV((Local) args.get(i), cv, summary);
							
							paramMapping.put(obj, ptg.vars.get((Local) args.get(i)));
						}
				}
			}

			/* 2.
             * Now, loop in the callie method's ptg to find if there
             * exists any relationship/node between the params
             * If exists, add the realtion for corresponding values in
             * paramsMapping also
             */
            PointsToGraph calliePTG = StaticAnalyser.ptgs.get(method);
            System.out.println("PRIYAM METHOD: " + method);
            System.out.println("PRIYAM PTGS: " + StaticAnalyser.ptgs);
            System.out.println("PRIYAM calliePTG: " + calliePTG);
            // If ptg gives error, ensure StaticAnalysis has been done

            for (int i = 0; i < paramCount; i++) {
                ObjectNode obj = new ObjectNode(i, ObjectType.parameter);
                Map<SootField, Set<ObjectNode>> pointingTo = calliePTG.fields.get(obj);

                if (pointingTo == null) {
                    continue;
                }

                for (Map.Entry<SootField, Set<ObjectNode>> entry : pointingTo.entrySet()) {
                    for (ObjectNode fieldObj : entry.getValue()) {
                        System.out.println("There exists an edge from: " + obj + " to " + fieldObj + " by " + entry.getKey());
                        if (fieldObj.type != ObjectType.parameter) {
                            continue;                            
                        }

                        if (!paramMapping.containsKey(obj) || !paramMapping.containsKey(fieldObj)) {
                            // If paramsMapping does not have the object, it can happen if null is passed
                            continue;
                        }

                        // Find paramsMapping for obj
                        // Find paramsMapping for fieldObj
                        // Add an edge from objs to fieldObjs
                        for (ObjectNode objInCaller : paramMapping.get(obj)) {
                            for (ObjectNode fieldObjInCaller : paramMapping.get(fieldObj)) {
                                System.out.println("There should exists an edge from: " + objInCaller + " to " + fieldObjInCaller + " by " + entry.getKey());
                                ptg.WEAK_makeField(objInCaller, entry.getKey(), fieldObjInCaller);
                            }
                        }                        
                    }
                }
            }

		}
	}
}