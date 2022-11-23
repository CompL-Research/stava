public class Main {
	public static void main(String[] args) {
	}

	static void foo(Node node1, Node node2) {
		Node A = new Node();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
		Node E = new Node();
		// bar(A, B);
		A.n = B;

		// if I make channges in staticAnalyser?

		Node x = A.n;

		// r10=[<external,52>, <internal, 8>]
		// Assumption 1
		// we do replacement of assignment
		// statements after the whole pass
		// How to consider for if else branch?
		// For different branches?

		bar(x, C);
		bar(D, E);
		bar(node1, node2);
	}

	static void bar(Node n1, Node n2) {
		n1.n = n2;
	}
}
