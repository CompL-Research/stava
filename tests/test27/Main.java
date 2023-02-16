public class Main {
	public static void main(String[] args) {
		// A recursive and multiple params test
	}

	static void foo(Node node1, Node node2) {
		Node A = new Node();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
		Node E = new Node();
		Node F = new Node();
		Node G = new Node();

		Main obj = new Main();
		obj.bar(A, B, C, D, E, F, G);
	}

	void bar(Node n1, Node n2, Node n3, Node n4, Node n5, Node n6, Node n7) {
		if (n7 == null) {
			return;
		}

		n1.n = n3;
		n4.n = n5;
		n5.n = n6;
		n6.n = n2;
		n2.n = n3;

		bar(n2, n3, n4, n5, n6, n7, null);
	}
}
