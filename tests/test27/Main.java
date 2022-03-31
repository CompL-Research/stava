public class Main {
	public static Node glbl;
	public static void main(String[] args) {
        A x = new A();
        func(x);
	}

    public static void func(A p1) {
        Node a = new Node();
        p1.foo();
    }

}

class A {
    public static Node glbl;
    public static void foo(){
        Node f = new Node();
    }
}

class B extends A {
    public static void foo() {
        Node b = new Node();
        // glbl = b;
    }
}

class C extends A {
    public static void foo() {
        Node c = new Node();
        //noescape;
    }
}