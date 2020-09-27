package handlers;

import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.PointsToGraph;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.ClassConstant;
import soot.jimple.MonitorStmt;

import java.util.Map;

public class MonitorStmtHandler {
	public static void handle(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		MonitorStmt stmt = (MonitorStmt) u;
		Value op = stmt.getOp();
		if (op instanceof Local) {
			if (ptg.vars.containsKey(op)) {
				ptg.vars.get(op).forEach(obj -> summary.get(obj).setEscape());
			} else {
				System.out.println("[MonitorStmtHandler] Warning: No ptset for " + u.toString() + " found!");
//				throw new IllegalArgumentException("No ptset for "+u.toString()+" found!");
			}
		} else if (op instanceof ClassConstant) {
			// Nothing to do here!
		} else {
			throw new IllegalArgumentException("[MonitorStmthandler] op is not recognised: " + op.getClass() + " in " + u.toString());
		}
	}

}
