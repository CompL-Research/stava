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
		func(C,B);
		return A;
	}

	public static Node func(Node p1, Node p2) {
		p1.n = p2;
		if(p2 != null) {
			p2 = bar();
		}
		return p1;
	}

	public static Node bar() {
		return new Node();
	}
}
