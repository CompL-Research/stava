public class Main {
	public static void main(String[] args) {
		foo(null, null);
	}

	static void foo(Node node1, Node node2) {
		Node A = new Node();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
		Node E = new Node();
		bar(A, B);
		bar(A, C);
		bar(D, E);
		bar(node1, node2);

		// see next test28
	}

	static void bar(Node n1, Node n2) {
		n1.n = n2;
	}
}
