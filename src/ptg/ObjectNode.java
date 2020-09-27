package ptg;

import soot.Unit;
import utils.getBCI;

public class ObjectNode {

	// an integer to store reference information
	public final int ref;
	// an enumeration to store the type of Object
	public final ObjectType type;
	// anyway the fields of ObjectNode are non modifiable. 
	public final int hashcode;

	public ObjectNode(int ref, ObjectType type) {
		this.ref = ref;
		this.type = type;
		int hashref = ref;
		int hashtype = type.hashCode();
		// one time calculation. Need not be performed later.
		this.hashcode = (hashref + hashtype) * hashtype + hashref;

	}

	public int hashCode() {
		return hashcode;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ObjectNode) {
			ObjectNode o = (ObjectNode) other;
			return this.ref == o.ref && this.type == o.type;
		}
		return false;
	}

	@Override
	public String toString() {
		return "<" + type.toString() + "," + ref + ">";
	}

	public static ObjectNode createObject(Unit u, ObjectType type){
		ObjectNode _ret = null;
		try {
			_ret = new ObjectNode(getBCI.get(u), type);
		} catch (Exception e){
			if(type == ObjectType.internal) _ret = InvalidBCIObjectNode.getInstance(type);
			else if(type == ObjectType.external) _ret = InvalidBCIObjectNode.getInstance(type);
		}
		return _ret;
	}
}
