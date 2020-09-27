package ptg;

public class InvalidBCIObjectNode extends ObjectNode {

	private static final InvalidBCIObjectNode internal = new InvalidBCIObjectNode(ObjectType.internal);
	private static final InvalidBCIObjectNode external = new InvalidBCIObjectNode(ObjectType.external);

	private InvalidBCIObjectNode(ObjectType type) {
		super(-2, type);
	}

	public static InvalidBCIObjectNode getInstance(ObjectType t) {
		if (t == ObjectType.internal) {
			return internal;
		} else if (t == ObjectType.external) {
			return external;
		} else {
			throw new IllegalArgumentException("Only internal or external exists. Not " + t.toString());
		}
	}

}
