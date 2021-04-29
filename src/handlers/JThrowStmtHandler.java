package handlers;

import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.PointsToGraph;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JThrowStmt;

import java.util.Map;

public class JThrowStmtHandler {
	public static void handle(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		JThrowStmt stmt = (JThrowStmt) u;
		Value op = stmt.getOp();
		if (op instanceof Local) {
			if (ptg.vars.containsKey(op)) {
				ptg.vars.get(op).forEach(obj -> summary.get(obj).setEscape());
			} else {
				// throw new IllegalArgumentException("No ptset for " + u.toString() + " found!");
			}
		} else {
			throw new IllegalArgumentException("op is not a local: " + op.getClass() + " in " + u.toString());
		}
	}
}
