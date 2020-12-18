public class Main {
	public static void main(String[] args) {
		test();
	}

	public static Node test() {
		Node A = new Node();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
		func(A,B);
		return A;
	}
	public static void func(Node p1, Node p2) {
		p1.n = p2;
	}
}
