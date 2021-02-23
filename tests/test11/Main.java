public class Main {
	public static void main(String[] args) {
		Node A = new Node();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
		A.n = B;
		B.n = C;
		func(A);
	}

	public static void func(Node p1) {
		// p2.n = p3;
		p1.n.n.n = new Node();
	}
}
