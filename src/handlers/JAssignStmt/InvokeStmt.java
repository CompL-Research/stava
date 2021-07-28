package handlers.JAssignStmt;

import main.CHATransform;
import config.AssignStmtHandler;
import config.UpdateType;
import es.*;
import es.EscapeStatus;
import handlers.JInvokeStmtHandler;
import ptg.InvalidBCIObjectNode;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import soot.*;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.jimple.internal.JAssignStmt;

import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Targets;


import utils.getBCI;
import java.util.Iterator;
import java.util.*;

/*
 * Meant to be only called by JAssignStmtHandler.
 * The sanitation check to ensure the appropriate types has been skipped for performance.
 */
public class InvokeStmt {
	private static int getSummarySize(Map<ObjectNode, EscapeStatus> summary)
	{
		return summary.toString().length();
	}
	public static void handle(SootMethod m, Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		Local lhs = (Local) ((JAssignStmt) u).getLeftOp();
		Value rhs = ((JAssignStmt) u).getRightOp();
		AbstractInvokeExpr expr = (AbstractInvokeExpr) rhs;
		//SootMethod m = expr.getMethod();	// Wrong
		JInvokeStmtHandler.handleExpr(m, u, expr, ptg, summary);

		// System.out.println("Size after handleexpr: "+ getSummarySize(summary));

		EscapeStatus es = new EscapeStatus();//(new ConditionalValue(m, new ObjectNode(0, ObjectType.returnValue), Boolean.TRUE));
		
		CallGraph cg = Scene.v().getCallGraph();

		// Iterator<MethodOrMethodContext> methods = new Targets(cg.edgesOutOf(u));

		Iterator<Edge> iedges = cg.edgesOutOf(u);
		List<Edge> edges = new ArrayList<>();
		if (!iedges.hasNext()) {
			iedges = CHATransform.getCHA().edgesOutOf(u);
		}
		while(iedges.hasNext()) {
			edges.add(iedges.next());
		}
		if (edges.size() == 0) {
			System.out.println("Empty edges: "+expr+", function incoming edges: "+cg.edgesInto(m).hasNext()+
								" Method: "+m.getBytecodeSignature());
			edges.add(new Edge(m, u, expr.getMethod(), Kind.SPECIAL));	
		}

		// System.out.println("Processing: "+expr);
		for (Edge edge: edges) {
			SootMethod method = edge.tgt();
			// EscapeState cv = new ConditionalValue (method, new ObjectNode(0, ObjectType.returnValue), Boolean.TRUE);
			EscapeState cv = ConditionalValue.getRetCV(method);
			// System.out.println("Method: "+m+" "+cv+" "+es);
			es.addEscapeState(cv);
		}

		ObjectNode n;
		try {
			n = new ObjectNode(getBCI.get(u), ObjectType.external);
			// ConditionalValue objectCV = new ConditionalValue(m,n);
			// for (EscapeState state: es.status) {
			// 	if (state instanceof ConditionalValue ) {
			// 		ObjectNode o = ((ConditionalValue)state).object;
			// 		// ptg.cascadeCV(o, objectCV, summary);
					
			// 	}
			// }
		} catch (Exception e) {
			System.out.println("Making it an invalid obj at:" + u);
			n = InvalidBCIObjectNode.getInstance(ObjectType.external);
		}
		if (AssignStmtHandler.INVOKE == UpdateType.STRONG) {
			ptg.forcePutVar(lhs, n);
		} else {
			ptg.addVar(lhs, n);
		}
		// System.out.println("Putting at "+n+": "+es+" Unit: "+u);
		summary.put(n, es);
//		if(!m.isJavaLibraryMethod()) {
//			System.out.println(m.toString()+" is not a library method");
//		}
//		else {
//			summary.put(n, new EscapeStatus());
//		}
	}
}
