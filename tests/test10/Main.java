public class Main {
	public static void main(String[] args) {
		Node A = new Node();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
		A.n = B;
		B.n = C;
		func(A, C, D);
	}

	public static void func(Node p1, Node p2, Node p3) {
		// p2.n = p3;
		p1.n.n.n = new Node();
	}
}
