package utils;

import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewMultiArrayExpr;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;

public class getBCI {
	public static int get(Unit u) {
		int _ret = -1;

		/*
			We are splitting the unit into "boxes" and for semi-boxes we check if any one of 
			them is JNewArray ot JNewMultiArray, then return BCI of the expression.

			I don't know if this will always work. It is based on the data I got by printing
			a lot of stuff. 

			Do We need to include JNew in this?
		*/
		for(ValueBox ub: u.getUseBoxes() ) {
			/*
				This loop is to get the BCI specifically in the case of JNewArrayExpr and JNewMultiArrayExpr.
				Each Unit in soot is constructed of multiple sub-Units. So, we reverse engineer the problematic
				statements. We were getting problems only in AssignStmts and we need to get BCI of statement
				on the right side of "=". This below line filters only the right side of an Assignment Statement.
			*/
			if ( !ub.getClass().toString().equals("class soot.jimple.internal.JAssignStmt$LinkedRValueBox") )
				continue;
				
			/*
				Next get the value contained in this box. If this value is a New Array/MultiArray Expr only
				then we need to move further.
			 */
            Value v = ub.getValue();
            if (v == null)
                continue;
            if ( ! (v instanceof JNewArrayExpr || v instanceof JNewMultiArrayExpr ) )
                continue;

			/*
				Because multiple statements in the Java classfile can be combined together to form 
				one JimpleStatment, BCI is linked to each box rather than the Unit. BCI of a Unit is 
				the BCI of last box processed in the Unit. Now, we get the BCI of this
				required box and return it. If this BCI is null, we can fall back to the BCI associated with 
				the Unit.
			*/
			BytecodeOffsetTag tg = (BytecodeOffsetTag) ub.getTag("BytecodeOffsetTag");
			if (tg != null)
				return tg.getBytecodeOffset();
		}
		
		Tag t = u.getTag("BytecodeOffsetTag");
		if (t == null) {
			String error = u.toString() + " doesn't have bytecodeoffset!";
			System.out.println("[utils.getBCI] [Warn] " + error);
			throw new IllegalArgumentException(error);
		}
		try {
			_ret = ((BytecodeOffsetTag) u.getTag("BytecodeOffsetTag")).getBytecodeOffset();
		} catch (Exception e) {
			System.out.println(u);
			System.out.println(e);
			throw e;
		}
		return _ret;
	}
}
