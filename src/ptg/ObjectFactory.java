package ptg;

import soot.Unit;
import soot.Value;
import soot.jimple.ClassConstant;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;

public class ObjectFactory {
	public static ObjectNode getObj(Unit u) {
		ObjectNode n = null;
		if(u instanceof JAssignStmt) {
			JAssignStmt stmt = (JAssignStmt) u;
			Value rhs = stmt.getRightOp();
			ObjectType t = null;
			if(rhs instanceof InvokeExpr || rhs instanceof InstanceFieldRef || 
					rhs instanceof JArrayRef) {
				t = ObjectType.external;
			} else {
				t = ObjectType.internal;
			}
			try {
				n = new ObjectNode(utils.getBCI.get(u), t);
			} catch(Exception e) {
				n = InvalidBCIObjectNode.getInstance(t);
			}
		}
		return n;
	}
}
