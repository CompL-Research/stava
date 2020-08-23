package handlers;

import java.util.List;
import java.util.Map;

import es.ConditionalValue;
import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import soot.Local;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;

public class JInvokeStmtHandler {
	
	public static void handle(Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		/*
		 * All method calls.
		 */
		JInvokeStmt stmt = (JInvokeStmt) u;
		InvokeExpr expr = stmt.getInvokeExpr();
		handleExpr(expr, ptg, summary);
	}
	public static void handleExpr(InvokeExpr expr, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
//		if(expr.getMethod().isJavaLibraryMethod()) {
//			return;
//		} 
		/*
		 * special: only constructors and initializers
		 * static: static method calls
		 * virtual: all normal calls
		 */
		if(expr instanceof JSpecialInvokeExpr) {
			/*
			 * Example of JSpecialInvokeExpr:
			 * specialinvoke r0.<java.lang.Object: void<init>()>()
			 */
			JSpecialInvokeExpr invokeExpr = (JSpecialInvokeExpr) expr;
			Value base = invokeExpr.getBase();
			ConditionalValue cv = new ConditionalValue( invokeExpr.getMethod(), new ObjectNode(-1,ObjectType.parameter), true);
//			System.out.println("CascadeCV "+cv.toString()+" on "+base.toString());
			ptg.cascadeCV((Local)base, cv, summary);
//			System.out.println(summary.get(new ObjectNode(17, ObjectType.internal)));
		} else if (expr instanceof JStaticInvokeExpr) {
			/*
			 * Has no base.
			 * Example of JStaticInvokeExpr:
			 * staticinvoke <Test: void foo(int)>(0)
			 */
			
//			JStaticInvokeExpr staticExpr = (JStaticInvokeExpr) expr;
//			System.out.println("Example of JStaticInvokeExpr:");
//			System.out.println(staticExpr);
			
		} else if (expr instanceof JVirtualInvokeExpr) {
			/*
			 * Example of JVirtualInvokeExpr:
			 * virtualinvoke $r0.<java.io.PrintStream: void println(int)>(i0)
			 */
			JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) expr;			
			Value base = invokeExpr.getBase();
			ConditionalValue cv = new ConditionalValue( invokeExpr.getMethod(), new ObjectNode(-1,ObjectType.parameter),true );
			ptg.cascadeCV((Local)base, cv, summary);
		} else if (expr instanceof JInterfaceInvokeExpr) {
			/*
			 * Example of JVirtualInvokeExpr:
			 * virtualinvoke $r0.<java.io.PrintStream: void println(int)>(i0)
			 */
			JInterfaceInvokeExpr invokeExpr = (JInterfaceInvokeExpr) expr;			
			Value base = invokeExpr.getBase();
			ConditionalValue cv = new ConditionalValue( invokeExpr.getMethod(), new ObjectNode(-1,ObjectType.parameter),true );
			ptg.cascadeCV((Local)base, cv, summary);
		} else {
			System.out.println("Unidentified invoke expr: "+expr.toString());
			throw new IllegalArgumentException(expr.toString());
		}
		SootMethod method = expr.getMethod();
		List<Value> args = expr.getArgs();
		for(int i=0; i<args.size(); i++) {
			Value arg = args.get(i);
			if(!(arg.getType() instanceof RefType)) continue;
			if(arg instanceof Constant) continue;
			ObjectNode obj = new ObjectNode(i, ObjectType.parameter);
			ConditionalValue cv = new ConditionalValue(method, obj, true);
			ptg.cascadeCV((Local)args.get(i), cv, summary);
		}		
		
	}
}
