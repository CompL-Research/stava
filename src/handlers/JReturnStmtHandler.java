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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JReturnStmtHandler {
	public static final ConcurrentHashMap<SootMethod, HashSet<ObjectNode> > returnedObjects = new ConcurrentHashMap<>(); 
	public static void handle(SootMethod m, Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		// System.out.println("Processing Return STMT: "+u);
		Value op = ((JReturnStmt) u).getOp();
		if (op instanceof NullConstant) return;
			// TODO: is this correct?
		else if (!(op instanceof Local)) return;
		Local l = (Local) ((JReturnStmt) u).getOp();
		if (l.getType() instanceof PrimType) return;
		// System.out.println("Return: "+u+" Method: "+m);
		if (returnedObjects.get(m) == null)
			returnedObjects.put(m, new HashSet<>());
		if (ptg.vars.get(l) != null)
			returnedObjects.get(m).addAll(ptg.vars.get(l));
		ptg.setAsReturn(m, l, summary);
	}
}
