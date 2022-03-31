// Check handling of inlining case

public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node();
		B.n = new Node();
		foo(B);
		A = B.n;
		foo(B.n);
	}

	public static void foo(Node p1){
        p1.n = new Node();
	}
	
}
