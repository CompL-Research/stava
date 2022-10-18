public class Main {
	public static void main(String[] args) {
		foo();
	}

	static void foo() {
		Node A = new Node();
		Node B = new Node();
		bar(A, B);

		Node D = new Node();
		Node E = new Node();
		bar(D, E);
	}

	static void bar(Node n1, Node n2) {
		n1.n = n2;

		bas(n1);
	}

	static void bas(Node x) {
		Node x1 = new Node();

		x.n = x1;

		int i = bat();
	}

	static int bat() {
		return 5;
	}
}
