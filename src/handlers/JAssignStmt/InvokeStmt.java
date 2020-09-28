package handlers.JAssignStmt;

import config.AssignStmtHandler;
import config.UpdateType;
import es.ConditionalValue;
import es.EscapeStatus;
import handlers.JInvokeStmtHandler;
import ptg.InvalidBCIObjectNode;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.jimple.internal.JAssignStmt;
import utils.getBCI;

import java.util.HashMap;

/*
 * Meant to be only called by JAssignStmtHandler.
 * The sanitation check to ensure the appropriate types has been skipped for performance.
 */
public class InvokeStmt {
	public static void handle(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Local lhs = (Local) ((JAssignStmt) u).getLeftOp();
		Value rhs = ((JAssignStmt) u).getRightOp();
		AbstractInvokeExpr expr = (AbstractInvokeExpr) rhs;
		SootMethod m = expr.getMethod();
		JInvokeStmtHandler.handleExpr(expr, ptg, summary);
		EscapeStatus es = new EscapeStatus(new ConditionalValue(m, new ObjectNode(0, ObjectType.returnValue), Boolean.TRUE));
		ObjectNode n;
		try {
			n = new ObjectNode(getBCI.get(u), ObjectType.external);
		} catch (Exception e) {
			System.out.println("Making it an invalid obj at:" + u);
			n = InvalidBCIObjectNode.getInstance(ObjectType.external);
		}
		if (AssignStmtHandler.INVOKE == UpdateType.STRONG) {
			ptg.forcePutVar(lhs, n);
		} else {
			ptg.addVar(lhs, n);
		}
		summary.put(n, es);
//		if(!m.isJavaLibraryMethod()) {
//			System.out.println(m.toString()+" is not a library method");
//		}
//		else {
//			summary.put(n, new EscapeStatus());
//		}
	}
}
