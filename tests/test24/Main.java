// Check handling of inlining case

public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); 
		Node C = new Node(); 
		Node D = func(B, C); 
		D.n = bar(C);
		A.n = D.n;
	}

	public static Node func(Node p1, Node p2) {
		Node E = new Node(); 
		Node F = new Node(); 
		Node G = new Node(); 
		Node H = new Node(); 
		p1.n = E;	
		p2.n = F;
		A.n = G;
		return H;
	}

	public static Node bar(Node p3) {
		Node I = new Node();
		return I;
	}
}
