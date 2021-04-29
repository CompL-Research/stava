class Node {
	static Node a;

	public void escapeObject() {
		a = this;
	}
}


public class Main {
	public static void main(String[] args) {
		Node A = new Node();
		A.escapeObject();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
	}
}
