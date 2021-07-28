package handlers.JAssignStmt;

import config.AssignStmtHandler;
import config.UpdateType;
import config.StoreEscape;
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

import java.util.Map;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/*
 * Meant to be only called by JAssignStmtHandler.
 * The sanitation check to ensure the appropriate types has been skipped for performance.
 */
public class StoreStmt {
	public static void handle(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		JAssignStmt stmt = (JAssignStmt) u;
		Value lhs = stmt.getLeftOp();
		Value rhs = stmt.getRightOp();
		
		if (StoreEscape.MarkStoreEscaping) {
			if (rhs instanceof Local) {
				ptg.cascadeEscape((Local)rhs, summary);
				return;
			}
		}

		if (lhs instanceof JInstanceFieldRef) {
			lhsIsJInstanceFieldRef(rhs, u, ptg, summary);
		} else if (lhs instanceof StaticFieldRef) {
			if (rhs instanceof StringConstant || rhs instanceof NullConstant || rhs instanceof ClassConstant) {
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

	private static void lhsIsJInstanceFieldRef(Value rhs, Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		if (rhs instanceof StringConstant) {
			storeStringConstantToInstanceFieldRefStmt(u, ptg, summary);
		} else if (rhs instanceof NullConstant) {
			eraseFieldRefStmt(u, ptg, summary);
		} else if (rhs instanceof Local) {
			storeStmt(u, ptg, summary);
		} else if (rhs instanceof ClassConstant) {
			storeClassConstantToInstanceFieldRefStmt(u, ptg, summary);
		}
		else {
			AnalysisError.unidentifiedAssignStmtCase(u);
		}
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
	private static void storeStmt(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		// Store case
		JInstanceFieldRef lhs = (JInstanceFieldRef) ((JAssignStmt) u).getLeftOp();
		Local rhs = (Local) ((JAssignStmt) u).getRightOp();
		HashSet<ObjectNode> lhsObjSet = (HashSet<ObjectNode>) ptg.vars.get(lhs.getBase());
		if (lhsObjSet == null) {
			// the lhs.base must be a field variable.
			// simply set rhs to escape
			ptg.cascadeEscape(rhs, summary);
			return;
		}
		// add field object for every parent object.
		Set<ObjectNode> rhsObjSet = ptg.vars.get(rhs);
		if(AssignStmtHandler.STORE == UpdateType.STRONG){
			ptg.STRONG_makeField((Local)lhs.getBase(), lhs.getField(), rhs);
		} else {
			ptg.WEAK_makeField((Local)lhs.getBase(), lhs.getField(), rhs);
		}
		ptg.propagateES((Local) lhs.getBase(), rhs, summary);
	}

	private static void StaticStoreStmt(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		ptg.cascadeEscape((Local) ((JAssignStmt) u).getRightOp(), summary);
	}

	private static void storeClassConstantToArrayRef(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		JArrayRef lhs = (JArrayRef) ((JAssignStmt) u).getLeftOp();
		ObjectNode obj = ObjectFactory.getObj(u);
		if (obj.type != ObjectType.internal) {
			System.out.println("at unit:" + u);
			throw new IllegalArgumentException("Object received from factory is not of required type: external");
		}
		ptg.storeStmtArrayRef((Local) lhs.getBase(), obj);
		summary.put(obj, new EscapeStatus(Escape.getInstance()));
	}

	private static void storeClassConstantToInstanceFieldRefStmt(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		JInstanceFieldRef lhs = (JInstanceFieldRef) ((JAssignStmt) u).getLeftOp();
		ObjectNode obj = ObjectFactory.getObj(u);
		if (obj.type != ObjectType.internal) {
			System.out.println("At unit:" + u);
			throw new IllegalArgumentException("Object received from factory is not of required type: internal");
		}

		if(AssignStmtHandler.STORE == UpdateType.STRONG) {
			ptg.STRONG_makeField((Local) lhs.getBase(), lhs.getField(), obj);
		} else {
			ptg.WEAK_makeField((Local) lhs.getBase(), lhs.getField(), obj);
		}

		summary.put(obj, new EscapeStatus(Escape.getInstance()));
	}

	private static void storeStringConstantToInstanceFieldRefStmt(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		JInstanceFieldRef lhs = (JInstanceFieldRef) ((JAssignStmt) u).getLeftOp();
		ObjectNode obj = ObjectFactory.getObj(u);
		if (obj.type != ObjectType.internal) {
			System.out.println("At unit:" + u);
			throw new IllegalArgumentException("Object received from factory is not of required type: internal");
		}
		if(AssignStmtHandler.STORE == UpdateType.STRONG) {
			ptg.STRONG_makeField((Local) lhs.getBase(), lhs.getField(), obj);
		} else {
			ptg.WEAK_makeField((Local) lhs.getBase(), lhs.getField(), obj);
		}
		EscapeStatus es = new EscapeStatus();
		if (obj instanceof InvalidBCIObjectNode) es.setEscape();
		if(!es.containsNoEscape()) {
			for (ObjectNode parent : ptg.vars.get(lhs.getBase())) {
				es.addEscapeStatus(summary.get(parent));
			}
		}
		summary.put(obj, es);
	}

	private static void storeStringConstantToArrayRefStmt(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		JArrayRef lhs = (JArrayRef) ((JAssignStmt) u).getLeftOp();
		ObjectNode obj = ObjectFactory.getObj(u);
		if (obj.type != ObjectType.internal) {
			throw new IllegalArgumentException("Object received from factory is not of required type: internal");
		}
		ptg.storeStmtArrayRef((Local) lhs.getBase(), obj);
		EscapeStatus es = new EscapeStatus();
		if (obj instanceof InvalidBCIObjectNode) es.setEscape();
		if(!es.containsNoEscape()) {
			for (ObjectNode parent : ptg.vars.get(lhs.getBase())) {
				es.addEscapeStatus(summary.get(parent));
			}
		}
		summary.put(obj, es);
	}

	private static void lhsArrayRef(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		JArrayRef lhs = (JArrayRef) ((JAssignStmt) u).getLeftOp();
		Local rhs = (Local) ((JAssignStmt) u).getRightOp();
		ptg.storeStmtArrayRef((Local) lhs.getBase(), rhs);
		ptg.propagateES((Local) lhs.getBase(), rhs, summary);
	}

	private static void eraseFieldRefStmt(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		if(AssignStmtHandler.STORE==UpdateType.WEAK) return;
		JInstanceFieldRef lhs = (JInstanceFieldRef) ((JAssignStmt) u).getLeftOp();
		if (!ptg.vars.containsKey(lhs.getBase())) return;
		for (ObjectNode obj : ptg.vars.get(lhs.getBase())) {
			if (ptg.fields.containsKey(obj)) {
				Map<SootField, Set<ObjectNode>> map = ptg.fields.get(obj);
				if (map.containsKey(lhs.getField())) {
					map.get(lhs.getField()).clear();
				}
			}
		}
	}
}
