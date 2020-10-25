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
			if(!ub.getClass().toString().equals("class soot.jimple.internal.JAssignStmt$LinkedRValueBox"))
                continue;
            Value v = ub.getValue();
            if(v==null)
                continue;
            if(v instanceof JNewArrayExpr || v instanceof JNewMultiArrayExpr )
                ;
			else continue;

			BytecodeOffsetTag tg = (BytecodeOffsetTag) ub.getTag("BytecodeOffsetTag");
			if(tg != null)
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
