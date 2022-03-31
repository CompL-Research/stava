public class Main {
    public static void main(String[] args) {
        Node A = new Node();    //O3
        Node B = new Node();    //O4
        foo(A);
        bar(B);
        Node C = func(A);
    }
    public static void foo(Node p1){
        Node D = new Node();    //O10
        bar(D);
        p1.n = D;
    }
    public static void bar(Node p2) {
        Node E = new Node();    //O15
        p2.n = E;
    }
    public static Node func(Node p3){
        Node F = new Node();    //O19
        return F;
    }
}