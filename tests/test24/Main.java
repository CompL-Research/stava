// Check handling of inlining case

public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); //NoEscape + recap
		Node C = new Node(); //NoEscape + recap
		Node D = func(B, C); //Noescape + recap
	}

	public static Node func(Node p1, Node p2) {
		Node E = new Node(); //NoEscape + recap
		Node F = new Node(); //NoEscape + recap
		Node G = new Node(); //Escape + norecap
		Node H = new Node(); //Escape + recap
		p1.n = E;	
		p2.n = F;
		A.n = G;
		return H;
	}
}
