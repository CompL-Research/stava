package handlers.JAssignStmt;

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
			// Ignore - [Verified] - No!
			// TODO: Change this
		} else if (lhs instanceof Local) {
			if (ptg.vars.containsKey(lhs)) {
				// do nothing
			} else {
				// ensure empty set for this var
				ptg.vars.put((Local) lhs, new HashSet<ObjectNode>());
			}
		} else {
			System.out.println("Unidentified case at: " + u);
			throw new IllegalArgumentException(u.toString());
		}
	}
}
