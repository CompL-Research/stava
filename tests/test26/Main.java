public class Main {
	public static void main(String[] args) {
		Node x = new Node();
		Node y = new Node();
		foo(x, y); // There should come a linking for x and y from baz
	}

	static void foo(Node node1, Node node2) {
		bar(node1, node2);
	}

	static void bar(Node n1, Node n2) {
		baz(n1, n2);
	}

	static void baz(Node p, Node q) {
		p.n = q; // Linking the objects here
	}
}
