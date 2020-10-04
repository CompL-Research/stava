package handlers.JAssignStmt;

import config.AssignStmtHandler;
import config.UpdateType;
import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.PointsToGraph;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.JAssignStmt;

import java.util.HashMap;
import java.util.HashSet;

/*
 * Meant to be only called by JAssignStmtHandler.
 * The sanitation check to ensure the appropriate types has been skipped for performance.
 */
public class EraseStmt {
	public static void handle(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Value lhs = ((JAssignStmt) u).getLeftOp();
		if (lhs instanceof StaticFieldRef) {
			// Ignore - [Verified]
		} else if (lhs instanceof Local) {
			if(AssignStmtHandler.ERASE == UpdateType.STRONG || !ptg.vars.containsKey(lhs)){
				ptg.vars.put((Local)lhs, new HashSet<ObjectNode>());
			}
		} else {
			utils.AnalysisError.unidentifiedAssignStmtCase(u);
		}
	}
}
