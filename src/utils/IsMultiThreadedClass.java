package utils;

import soot.SootClass;

public class IsMultiThreadedClass {
	public static boolean check(SootClass klass) {
		do {
			// To resolve the error: No superclass for java.lang.Object
			if (klass.getName().equals("java.lang.Object")) {
				return false;
			}

			// No need to check the second condition; "Thread" itself implements "Runnable".
			if (klass.implementsInterface("java.lang.Runnable")/* || klass.getSuperclass().getName().equals("java.lang.Thread")*/) {
				return true;
			}

			// TODO: Check if removing this works (added because of NPE when klass = 'avrora.syntax.objdump.ObjDumpReformatter'
			if (klass.hasSuperclass() == false || klass.getSuperclass() == null) {
				// For the above mentioned 'avrora.syntax.objdump.ObjDumpReformatter', the superclass is 'Object' (as seen in avrora's javadocs)
				return false;
			}

			klass = klass.getSuperclass();
		} while (klass.hasSuperclass());
		return false;
	}
}
