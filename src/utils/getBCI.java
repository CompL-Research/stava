package utils;

import soot.Unit;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;

public class getBCI {
	public static int get(Unit u) {
		int _ret = -1;
		Tag t = u.getTag("BytecodeOffsetTag");
		if(t==null) {
			String error = new String(u.toString()+" doesn't have bytecodeoffset!");
			System.out.println("[utils.getBCI] [Warn] "+error);
			throw new IllegalArgumentException(error);
		}
		try {
			_ret =  ((BytecodeOffsetTag)u.getTag("BytecodeOffsetTag")).getBytecodeOffset();
		} catch (Exception e) {
			System.out.println(u);
			System.out.println(e);
			throw e;
		}
		return _ret;		
	}
}
