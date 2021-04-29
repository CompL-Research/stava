import java.util.HashMap;

class GlobalVars {
	public static Node n1;
	public static Node n2;
}

public class Main {
	static Node x;
	public static void main(String[] args) {
		C1 tmp = new C1();
		tmp.t2();
		System.out.println(tmp.n1);
	}

	public static Node func(Node p1, Node p2, Node p3) {
		// p2.n = p3;
		// p1.n.n = p3;
		// p1 = new Node();
		// System.out.println(p1);
		// int x = 1;
		// if (x == 0)
			
		// else 
			// return p1;
		
		return p1;
	}
}

class C1 {
	Node n1;

	// public void t1(Node x) {
	// 	n1 = new Node();
	// 	x.n = new Node();
	// }

	public void t2() {
		Node t = new Node();
		t.n = new Node();
		// HashMap<String,Node> hm = new HashMap<>();
		// hm.put("Hi", t);
		// t1(t);
		// System.out.println(n1);
		// n1 = t.n;
		t3(t);
	}
	public void t3(Node a) {
		n1 = a.n;
	}
}