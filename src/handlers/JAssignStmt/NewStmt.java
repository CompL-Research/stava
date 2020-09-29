package handlers.JAssignStmt;

import config.AssignStmtHandler;
import config.UpdateType;
import es.Escape;
import es.EscapeStatus;
import es.NoEscape;
import ptg.InvalidBCIObjectNode;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNewMultiArrayExpr;
import utils.AnalysisError;
import utils.IsMultiThreadedClass;
import utils.getBCI;

import java.security.InvalidParameterException;
import java.util.HashMap;

/*
 * Meant to be only called by JAssignStmtHandler.
 * The sanitation check to ensure the appropriate types has been skipped for performance.
 */
public class NewStmt {
	public static void handle(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Value rhs = ((JAssignStmt) u).getRightOp();
		if (rhs instanceof JNewExpr) {
			JNewStmt(u, ptg, summary);
		} else if (rhs instanceof JNewArrayExpr || rhs instanceof JNewMultiArrayExpr) {
			JNewArrayStmt(u, ptg, summary);
		} else AnalysisError.unidentifiedAssignStmtCase(u);
	}

	/*
	 * Undoubtedly a new object creation. Object will be internal and ES will be does not escape.
	 */
	private static void JNewStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {

		// check if rhs is runnable
		Value rhs = ((JAssignStmt) u).getRightOp();
		Value lhs = ((JAssignStmt) u).getLeftOp();
		EscapeStatus es = new EscapeStatus();
		if (IsMultiThreadedClass.check(((JNewExpr) rhs).getBaseType().getSootClass())) es.setEscape();

		ObjectNode obj;
		try {
			obj = new ObjectNode(getBCI.get(u), ObjectType.internal);
		} catch (Exception e1) {
			obj = InvalidBCIObjectNode.getInstance(ObjectType.internal);
			es.setEscape();
		}
		if (AssignStmtHandler.NEW == UpdateType.STRONG){
			ptg.forcePutVar((Local) lhs, obj);
		} else {
			ptg.addVar((Local) lhs, obj);
		}
		if (!summary.containsKey(obj)) summary.put(obj, es);
	}

	private static void JNewArrayStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		// check if rhs is runnable
		Value rhs = ((JAssignStmt) u).getRightOp();
		Value lhs = ((JAssignStmt) u).getLeftOp();

		EscapeStatus es;
		es = new EscapeStatus(NoEscape.getInstance());
		ObjectNode obj = new ObjectNode(getBCI.get(u), ObjectType.internal);
		try {
			ptg.forcePutVar((Local) lhs, obj);
		} catch (Exception e) {
			System.out.println(lhs + " may not be a local. Typecast must have failed!");
			throw new InvalidParameterException(lhs.toString() + " may not be a local. Typecast must have failed!");
		}
		if (!summary.containsKey(obj)) summary.put(obj, es);
	}

}
