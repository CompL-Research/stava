public class Main {
	public static void main(String[] args) {
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
	}

	static void bar(Node n1, Node n2) {
		Node F = new Node();
		n1.n = F; // we need to heapify F (address check)
		F.n = n2; // no heapfication

		// Assumption 2
		// Theory -- If two objects are directly connected
		// Then only we need stack order, else, we will not be needing any stack order
	}
}
