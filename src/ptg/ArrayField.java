package ptg;

import soot.Scene;
import soot.SootField;
import soot.Type;

public class ArrayField extends SootField {
	
	public static ArrayField instance = new ArrayField("ArrayObject", Scene.v().getObjectType());
	
	public ArrayField(String name, Type type) {
		super(name, type);
	}
	
	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof ArrayField && ((ArrayField)o)==ArrayField.instance) return true;
		else return false;
	}
	@Override
	public String toString() {
		return name.toString();
	}
}
