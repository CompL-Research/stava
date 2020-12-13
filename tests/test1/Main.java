public class Main {
	public static void main() {
		Node A = new Node();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
		A.n = B;
		B.n = C;
		C.n = D;
		D.n = B;
	}
}
