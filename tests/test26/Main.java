// Check handling of inlining case

public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node();
        Node C = new Node();
		foo(B);
        bar(C);
        Node H = func(B);
	}

	public static void foo(Node p1){
        Node D = new Node();
		bar(D);
		p1.n = D;
	}

	public static void bar(Node p2) {
		Node F = new Node();
        Node I = new Node();
		p2.n = F;
        F.n = I;
        A.n = I;
	}

    public static Node func(Node p3){
        Node G = new Node();
        G.n = new Node();
        A.n = G.n;
        return G;
    }
	
}

/*

*/ 
