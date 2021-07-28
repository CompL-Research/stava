package main;

import java.util.*;

import soot.*;
import soot.util.*;
import soot.SceneTransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;

public class CHATransform extends SceneTransformer{
    static CallGraph cha;
    static CallGraph spark;
    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        spark = Scene.v().getCallGraph();
        CallGraphBuilder cgg = new CallGraphBuilder(DumbPointerAnalysis.v());
        cgg.build();
        cha = cgg.getCallGraph();
        Scene.v().setCallGraph(spark);
    }
    public static CallGraph getCHA() {
        return cha;
    }
    public static CallGraph getSpark() {
        return spark;
    }
}
