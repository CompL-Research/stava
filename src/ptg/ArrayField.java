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
		return o instanceof ArrayField && o == ArrayField.instance;
	}

	@Override
	public String toString() {
		return name;
	}
}
