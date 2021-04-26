class Node {
	static Node a;

	public Node() {
		a = this;
	}
}


public class Main {
	public static void main(String[] args) {
		Node A = new Node();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
	}
}
