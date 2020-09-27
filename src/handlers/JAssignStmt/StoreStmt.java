package handlers.JAssignStmt;

import es.Escape;
import es.EscapeStatus;
import ptg.*;
import soot.Local;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.ClassConstant;
import soot.jimple.NullConstant;
import soot.jimple.StaticFieldRef;
import soot.jimple.StringConstant;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import utils.AnalysisError;

import java.util.*;

/*
 * Meant to be only called by JAssignStmtHandler.
 * The sanitation check to ensure the appropriate types has been skipped for performance.
 */
public class StoreStmt {
	public static void handle(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JAssignStmt stmt = (JAssignStmt) u;
		Value lhs = stmt.getLeftOp();
		Value rhs = stmt.getRightOp();
		if (lhs instanceof JInstanceFieldRef) {
			lhsIsJInstanceFieldRef(rhs, u, ptg, summary);
		} else if (lhs instanceof StaticFieldRef) {
			if (rhs instanceof StringConstant || rhs instanceof NullConstant) {
				// Nothing to do!
			} else if (rhs instanceof Local) {
				StaticStoreStmt(u, ptg, summary);
			} else AnalysisError.unidentifiedAssignStmtCase(u);
		} else if (lhs instanceof JArrayRef) {
			if (rhs instanceof StringConstant) {
				storeStringConstantToArrayRefStmt(u, ptg, summary);
			} else if (rhs instanceof ClassConstant) {
				storeClassConstantToArrayRef(u, ptg, summary);
			} else if (rhs instanceof Local) {
				lhsArrayRef(u, ptg, summary);
			} else if (rhs instanceof NullConstant) {
				// nothing to do here as we have only weak updates for an arrayref!
			} else AnalysisError.unidentifiedAssignStmtCase(u);
		} else AnalysisError.unidentifiedAssignStmtCase(u);
	}

	private static void lhsIsJInstanceFieldRef(Value rhs, Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		if (rhs instanceof StringConstant) {
			storeStringConstantToInstanceFieldRefStmt(u, ptg, summary);
		} else if (rhs instanceof NullConstant) {
//			eraseFieldRefStmt(u, ptg, summary);
		} else if (rhs instanceof Local) {
			storeStmt(u, ptg, summary);
		} else AnalysisError.unidentifiedAssignStmtCase(u);
	}

	/*
	 * Has a field reference in lhs. will have a local on rhs.
	 * A field object will NOT be created as there will already
	 * be a points-to set of rhs.
	 * This needs to be added as a field object set for every
	 * parent object.
	 *
	 * pseudocode:
	 * for (object in parent object set){
	 * 				field_name
	 * 		object ------------> rhs object set copy.
	 * 		es(rhs) U= es(object).field_name
	 * }
	 */
	private static void storeStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		// Store case
		JInstanceFieldRef lhs = (JInstanceFieldRef) ((JAssignStmt) u).getLeftOp();
		Local rhs = (Local) ((JAssignStmt) u).getRightOp();
		HashSet<ObjectNode> ptSet = (HashSet<ObjectNode>) ptg.vars.get(lhs.getBase());
		if (ptSet == null) {
			// the lhs.base must be a field variable.
			// simply set rhs to escape
			ptg.cascadeEscape(rhs, summary);
			return;
//			throw new IllegalArgumentException("[JAssignStmtHandler] ptset for "+lhs.getBase().toString()+" of "+u.toString()+" not found!");
		}
		// add field object for every parent object.
		Set<ObjectNode> objSet = ptg.vars.get(rhs);
		if (objSet == null) {
			// rhs was probably set to null before this. Hence the points to set is not found.
			// hence it needs to be set to an empty set!
			// throw new IllegalArgumentException("[JAssignStmtHandler] ptset for "+rhs.toString()+" of "+u.toString()+" not found!");
			ptSet.forEach(parent -> {
				ptg.makeField(parent, lhs.getField(), new HashSet<>());
			});
		} else {
			ptSet.forEach(parent -> {
				ptg.makeField(parent, lhs.getField(), (Set<ObjectNode>) ((HashSet<ObjectNode>) objSet).clone());
			});
			ptg.propagateES((Local) lhs.getBase(), rhs, summary);
		}
	}

	private static void StaticStoreStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Iterator<ObjectNode> it = ptg.reachables((Local) ((JAssignStmt) u).getRightOp()).iterator();
		while (it.hasNext()) {
			summary.get(it.next()).setEscape();
		}
	}

	private static void storeClassConstantToArrayRef(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JArrayRef lhs = (JArrayRef) ((JAssignStmt) u).getLeftOp();
		ObjectNode obj = ObjectFactory.getObj(u);
		if (obj.type != ObjectType.internal) {
			System.out.println("at unit:" + u);
			throw new IllegalArgumentException("Object received from factory is not of required type: external");
		}
		ptg.storeStmtArrayRef((Local) lhs.getBase(), obj);
		summary.put(obj, new EscapeStatus(Escape.getInstance()));
	}

	private static void storeStringConstantToInstanceFieldRefStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JInstanceFieldRef lhs = (JInstanceFieldRef) ((JAssignStmt) u).getLeftOp();
		ObjectNode obj = ObjectFactory.getObj(u);
		if (obj.type != ObjectType.internal) {
			System.out.println("At unit:" + u);
			throw new IllegalArgumentException("Object received from factory is not of required type: internal");
		}
		ptg.makeField((Local) lhs.getBase(), lhs.getField(), obj);
		EscapeStatus es = new EscapeStatus();
		if (obj instanceof InvalidBCIObjectNode) es.setEscape();
		ptg.vars.get(lhs.getBase()).forEach(parent -> es.addEscapeStatus(summary.get(parent)));
		summary.put(obj, es);
	}

	private static void storeStringConstantToArrayRefStmt(Unit u, PointsToGraph ptg,
														  HashMap<ObjectNode, EscapeStatus> summary) {
		JArrayRef lhs = (JArrayRef) ((JAssignStmt) u).getLeftOp();
		ObjectNode obj = ObjectFactory.getObj(u);
		if (obj.type != ObjectType.internal) {
			throw new IllegalArgumentException("Object received from factory is not of required type: internal");
		}
		ptg.storeStmtArrayRef((Local) lhs.getBase(), obj);
		EscapeStatus es = new EscapeStatus();
		if (obj instanceof InvalidBCIObjectNode) es.setEscape();
		ptg.vars.get(lhs.getBase()).forEach(parent -> es.addEscapeStatus(summary.get(parent)));
		summary.put(obj, es);
	}

	private static void lhsArrayRef(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JArrayRef lhs = (JArrayRef) ((JAssignStmt) u).getLeftOp();
		Local rhs = (Local) ((JAssignStmt) u).getRightOp();

		try {
			ptg.storeStmtArrayRef((Local) lhs.getBase(), rhs);
			ptg.propagateES((Local) lhs.getBase(), rhs, summary);
		} catch (Exception e) {
			System.out.println(e);
			throw new IllegalArgumentException(lhs.getBase().toString() + " probably could not be cast to Local at " + u.toString());
		}
	}

	private static void eraseFieldRefStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JInstanceFieldRef lhs = (JInstanceFieldRef) ((JAssignStmt) u).getLeftOp();
		if (!ptg.vars.containsKey(lhs.getBase())) return;
		ptg.vars.get(lhs.getBase()).forEach(obj -> {
			if (ptg.fields.containsKey(obj)) {
				Map<SootField, Set<ObjectNode>> map = ptg.fields.get(obj);
				if (map.containsKey(lhs.getField())) {
					map.get(lhs.getField()).clear();
					map.remove(lhs.getField());
				}
			}
		});
	}
}
