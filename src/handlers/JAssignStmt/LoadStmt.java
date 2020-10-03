package handlers.JAssignStmt;

import config.AssignStmtHandler;
import config.UpdateType;
import es.Escape;
import es.EscapeStatus;
import ptg.*;
import soot.Local;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import utils.AnalysisError;
import utils.getBCI;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/*
 * Meant to be only called by JAssignStmtHandler.
 * The sanitation check to ensure the appropriate types has been skipped for performance.
 */
public class LoadStmt {
	public static void handle(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JAssignStmt stmt = (JAssignStmt) u;
		Value rhs = stmt.getRightOp();
		if (rhs instanceof StaticFieldRef) {
			staticField(u, ptg, summary);
		} else if (rhs instanceof JInstanceFieldRef) {
			instanceField(u, ptg, summary);
		} else if(rhs instanceof JArrayRef){
			rhsArrayRef(u, ptg, summary);
		} else AnalysisError.unidentifiedAssignStmtCase(u);
	}

	private static void staticField(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Local lhs = (Local) ((JAssignStmt) u).getLeftOp();
		ObjectNode obj = new ObjectNode(getBCI.get(u), ObjectType.external);
		EscapeStatus es = new EscapeStatus(Escape.getInstance());
		if(AssignStmtHandler.LOAD == UpdateType.STRONG) {
			ptg.forcePutVar(lhs, obj);
		} else {
			ptg.addVar(lhs, obj);
		}
		summary.put(obj, es);
	}

	private static void instanceField(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JAssignStmt stmt = (JAssignStmt) u;
		Local lhs = (Local) stmt.getLeftOp();
		JInstanceFieldRef rhs = (JInstanceFieldRef) stmt.getRightOp();
		if (ptg.vars.containsKey(rhs.getBase())) {
			/*
			 * if(field already exists){
			 * 		assimilate and add
			 * } else {
			 * 		create new external object.
			 * 		add field to parent.
			 * 		make escape status as combination of all parents'
			 * 		escape status
			 * }
			 */
			if (ptg.containsField((Local) rhs.getBase(), rhs.getField())) {
				// lhs it exists already
				// assemble field objects
				ptg.vars.put(lhs, ptg.assembleFieldObjects((Local) rhs.getBase(), rhs.getField()));
				// TODO: that's all? no need for make field?
			} else {
				ObjectNode obj = ObjectNode.createObject(u, ObjectType.external);
				if(AssignStmtHandler.LOAD == UpdateType.STRONG){
					ptg.forcePutVar(lhs, obj);
				} else {
					ptg.addVar(lhs, obj);
				}
				// This is strong only as the field does not exist for the
				ptg.STRONG_makeField((Local) rhs.getBase(), rhs.getField(), obj);

				//assimilate parents' es
				EscapeStatus parentsES = new EscapeStatus();
				Set<ObjectNode> parentsObjSet = ptg.vars.get(rhs.getBase());
				Iterator<ObjectNode> it = parentsObjSet.iterator();
				while (it.hasNext()) {
					parentsES.addEscapeStatus(summary.get(it.next()));
				}
				// make field
				EscapeStatus es = parentsES.makeField(rhs.getField());
				if(obj instanceof InvalidBCIObjectNode) es.setEscape();
				summary.put(obj, es);
			}
		} else {
			// might be a field variable, and hence has no definition
			// no need to do rhs.base -field> obj
			// set to escape
			ObjectNode obj = ObjectNode.createObject(u, ObjectType.external);
			EscapeStatus es = new EscapeStatus(Escape.getInstance());
			if(AssignStmtHandler.LOAD==UpdateType.STRONG) ptg.forcePutVar(lhs, obj);
			else ptg.addVar(lhs, obj);
			summary.put(obj, es);
		}
	}

	/*
	 * Warning: This implementation might add 2 objects for an lhs based on the
	 * ObjectType of the base, as in a case where the points-to set of the
	 * base containing objects of both internal and external type.
	 */
	private static void rhsArrayRef(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Local lhs = (Local) ((JAssignStmt) u).getLeftOp();
		Value rhs = ((JAssignStmt) u).getRightOp();
		JArrayRef arrayRef = (JArrayRef) rhs;
		Value base = arrayRef.getBase();

		if (!ptg.vars.containsKey(base)) {
			// The base might be a field variable for the object.
			ObjectNode obj = new ObjectNode(utils.getBCI.get(u), ObjectType.external);
			ptg.forcePutVar(lhs, obj);
			summary.put(obj, new EscapeStatus(Escape.getInstance()));
			return;
		}

		Set<ObjectNode> objs = ptg.vars.get(base);
		ObjectNode internalobj = ObjectNode.createObject(u, ObjectType.internal);
		ObjectNode externalobj = ObjectNode.createObject(u, ObjectType.external);

		Iterator<ObjectNode> iterator = objs.iterator();
		Set<ObjectNode> s = new HashSet<>();
		SootField f = ArrayField.instance;
		while (iterator.hasNext()) {
			ObjectNode parent = iterator.next();
			ObjectNode child = null;

			switch (parent.type) {
				case internal:
					child = internalobj;
					break;
				case parameter:
				case external:
					child = externalobj;
					break;
				default:
					throw new InvalidParameterException("Array of argument makes no sense!");
			}

			ptg.WEAK_makeField(parent, f, child);
			EscapeStatus es = summary.get(parent).makeField(f);
			if (summary.containsKey(child)) {
				summary.get(child).status.addAll(es.status);
			} else {
				summary.put(child, es);
			}

			s.add(child);
		}
		if(AssignStmtHandler.LOAD == UpdateType.STRONG) {
			ptg.forcePutVar(lhs, s);
		} else {
			ptg.addVar(lhs, s);
		}
	}

}
