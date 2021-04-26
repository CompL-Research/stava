import java.lang.System;
import java.util.*;
class Native
{ 
    static 
    { 
        System.loadLibrary("native"); 
    } 
    public native int nativeFunc(Node a, Node b);
}
class Main
{ 
    public static void main(String[] args) 
    { 
        Native n = new Native(); 
        Node a = new Node(5);
        Node b = new Node(6);
        Node c = new Node(7);
        Node d = new Node(8);
        List<String> x = new ArrayList<>();
        x.add("Hello");
        d.escapeObject();
        System.out.println(n.nativeFunc(a, b)); 
    } 
}