class Node {
    Node n;

    Node getObject() {
        return this;
    }
}

// check Handling of multi-threaded case
// disconnected cfgs

public class Main{
    static Node p;
    public static void main(String[] args) {
        Node x = test();
    }
    public static Node test() {
        Node a = new Node();
        // a -> <func, <param,0> >
        Node x = new Node();
        // p = x;
        Node b;
        int y = 5;
        int z = 6;
        if (true) b = x.getObject();
        else b = a.getObject();

        return b;
        // Node b = func(a);
        /*
         * <external, 2> = [<func,returnVal>]
         */
        // return b;
    }
    // public static Node func(Node a) {
    //     if (a == null)
    //         a = new Node();
    //     return a;
    //     // ret -> <param,0>, <internal, 1>
    // }
}
