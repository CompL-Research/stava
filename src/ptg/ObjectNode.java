package ptg;

public class ObjectNode {

	// an integer to store reference information
	public final int ref;
	// an enumeration to store the type of Object
	public final ObjectType type;
	// anyway the fields of ObjectNode are non modifiable. 
	public final int hashcode;

	public ObjectNode(int ref, ObjectType type){
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
		if(other instanceof ObjectNode) {
			ObjectNode o = (ObjectNode) other;
			if(this.ref == o.ref && this.type == o.type) return true;
			return false;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return new String("<"+type.toString()+","+Integer.toString(ref)+">");
	}
}
