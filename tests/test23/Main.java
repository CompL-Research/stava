public class Main {
	public static void main(String[] args) {
		Node A = new Node();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
		Node E  = new Node();
		A.n = B;
		B.n = D;
		B.m = E;
		D.n = C;
	}
}
