// Check handling of inlining case

public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node();
		foo(B);
	}

	public static void foo(Node p1){
		bar(p1);
	}

	public static void bar(Node p2) {
		Node F = new Node();
		p2.n = F;
	}
	
}

/*

*/ 
