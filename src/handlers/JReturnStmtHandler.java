package handlers;

import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.PointsToGraph;
import soot.Local;
import soot.PrimType;
import soot.Unit;
import soot.Value;
import soot.jimple.NullConstant;
import soot.jimple.internal.JReturnStmt;
import soot.SootMethod;
import java.util.Map;

public class JReturnStmtHandler {
	public static void handle(SootMethod m, Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		Value op = ((JReturnStmt) u).getOp();
		if (op instanceof NullConstant) return;
			// TODO: is this correct?
		else if (!(op instanceof Local)) return;
		Local l = (Local) ((JReturnStmt) u).getOp();
		if (l.getType() instanceof PrimType) return;
		ptg.setAsReturn(m, l, summary);
	}
}
