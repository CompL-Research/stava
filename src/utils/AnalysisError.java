package utils;

import soot.Unit;
import soot.Value;
import soot.jimple.internal.JAssignStmt;

public class AnalysisError {
	public static void unidentifiedAssignStmtCase(Unit u) {
		JAssignStmt stmt = (JAssignStmt) u;
		Value lhs = stmt.getLeftOp();
		Value rhs = stmt.getRightOp();
		String error = "Unidentified assignstmt case with " + u.toString() + " " + lhs.getClass() + "," + rhs.getClass();
		System.out.println(error);
		throw new IllegalArgumentException(error);
	}
}
