package handlers.JAssignStmt;

import config.AssignStmtHandler;
import config.UpdateType;
import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.PointsToGraph;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.NullConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import utils.AnalysisError;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/*
 * Meant to be only called by JAssignStmtHandler.
 * The sanitation check to ensure the appropriate types has been skipped for performance.
 */
public class CopyStmt {
	public static void handle(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		Value rhs = ((JAssignStmt)u).getRightOp();
		if(rhs instanceof Local){
			CopyStmt(u, ptg, summary);
		} else if(rhs instanceof JCastExpr){
			rhsCastExpr(u, ptg, summary);
		} else AnalysisError.unidentifiedAssignStmtCase(u);
	}

	private static void rhsCastExpr(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		// TODO: put a check for cast to a runnable class.
		// a = (Cast?)b
		Local lhs = (Local)((JAssignStmt)u).getLeftOp();
		Value op = ((JCastExpr) ((JAssignStmt) u).getRightOp()).getOp();
		// a = (Cast) null;
		if (op instanceof NullConstant) {
			if(AssignStmtHandler.ERASE == UpdateType.STRONG || !ptg.vars.containsKey(lhs)){
				ptg.vars.put((Local)lhs, new HashSet<ObjectNode>());
			}
			return;
		}
		Local rhs;
		try {
			rhs = (Local) op;
		} catch (Exception e) {
			System.out.println("Unable to cast rhs to Local at: " + u.toString());
			throw e;
		}
		CopyStmtHelper(u, (Local) ((JAssignStmt) u).getLeftOp(), rhs, ptg, summary);
	}

	private static void CopyStmt(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		Local lhs = (Local) ((JAssignStmt) u).getLeftOp();
		Local rhs = (Local) ((JAssignStmt) u).getRightOp();
		CopyStmtHelper(u, lhs, rhs, ptg, summary);
	}

	private static void CopyStmtHelper(Unit u, Local lhs, Local rhs, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		Set<ObjectNode> ptSet = null;
		if (ptg.vars.containsKey(rhs)) {
			ptSet = (Set<ObjectNode>) ((HashSet<ObjectNode>) ptg.vars.get(rhs)).clone();
		} else {
			// rhs is a field variable
			// rhs could be null
			System.out.println("[Copystmthelper] " + u.toString());
			System.out.println("[Copystmthelper] " + ptg.toString());
			// throw new IllegalArgumentException("");
//			ObjectNode obj = new ObjectNode(utils.getBCI.get(u), ObjectType.external);
//			ptSet = new HashSet<ObjectNode>();
//			summary.put(obj, new EscapeStatus(Escape.getInstance()));
		}
		if(AssignStmtHandler.COPY == UpdateType.WEAK && ptg.vars.containsKey(lhs)) {
			ptg.vars.get(lhs).addAll(ptSet);
		} else{
			ptg.vars.put(lhs, ptSet);
		}
	}

}
