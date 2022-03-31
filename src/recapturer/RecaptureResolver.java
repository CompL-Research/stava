package recapturer;

import config.StoreEscape;
import es.*;
import ptg.Analysis;
import ptg.FlowSet;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import ptg.RetLocal;
import ptg.StandardObject;
import handlers.*;
import handlers.JAssignStmt.InvokeStmt;
import soot.MethodOrMethodContext;
import soot.SootField;
import soot.SootMethod;
import soot.Scene;
import soot.Unit;
import soot.Value;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.NullConstant;
import java.util.*;
import java.io.*;

public class RecaptureResolver {
    public Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> escapeSummaries;
    public Map<SootMethod, HashMap<Integer, HashSet<StandardObject>>> existingRecaptureSummaries;
    public Map<SootMethod, HashMap<InvokeSite, HashSet<StandardObject>>> siteRecaptureSummaries;
    public static LinkedHashMap<Body, Analysis> analysis;
    // Map<SootMethod, HashSet<SootMethod>> adjCallGraph;
    public HashMap<StandardObject, Set<StandardObject> > graph;

    public RecaptureResolver(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> solvedSummaries,
            Map<SootMethod, HashMap<Integer, HashSet<StandardObject>>> recaptureSummaries,
            Map<SootMethod, PointsToGraph> ptgs,
            HashMap<StandardObject, Set<StandardObject> > graph,
            LinkedHashMap<Body, Analysis> analysis) {
        
        this.escapeSummaries = solvedSummaries;
        this.existingRecaptureSummaries = recaptureSummaries;
        this.analysis = analysis;
        this.siteRecaptureSummaries = new HashMap<>();
        this.graph = graph;
        for(Map.Entry<StandardObject, Set<StandardObject>> entry: this.graph.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getKey().hashCode() + " GRAPH " + entry.getValue());
        }
        // CallGraph cg = Scene.v().getCallGraph();
        // for(Map.Entry<SootMethod, HashSet<StandardObject>> entry : this.existingRecaptureSummaries.entrySet()) {
        //     SootMethod method = entry.getKey();
        //     Iterator<Edge> iter = cg.edgesInto(method);
        //     while(iter.hasNext()){
        //         Edge edge = iter.next();
        //         if(!this.adjCallGraph.containsKey(edge.src().method())){
        //             this.adjCallGraph.put(edge.src().method(),new HashSet<>());
        //         }
        //         this.adjCallGraph.get(edge.src().method()).add(edge.getTgt().method());
        //     }
        // }

        SolveSummaries();
    }

    void SolveSummaries() {
        for (Map.Entry<Body, Analysis> entry : this.analysis.entrySet()) {
            SootMethod method = entry.getKey().getMethod();
            Map<Unit, FlowSet> flowsets = entry.getValue().getFlowSets();
            HashMap<ObjectNode, EscapeStatus> escapeSummary = escapeSummaries.get(method);
            if(!siteRecaptureSummaries.containsKey(method)) {
                siteRecaptureSummaries.put(method, new HashMap<>());
            }
            HashMap<InvokeSite, HashSet<StandardObject>> siteRecaptureSummary = siteRecaptureSummaries.get(method);
            for(Map.Entry<Unit, FlowSet> e : flowsets.entrySet()) {
                Unit unit = e.getKey();
                SootMethod call;
                System.out.println("RECRE: " + method + ": " + e.getKey() + ": " + e.getValue().getOut());
                if(unit instanceof JInvokeStmt) {
                    InvokeSite invokeSite = new InvokeSite(((JInvokeStmt)unit).getInvokeExpr().getMethod(), unit);
                    if(!siteRecaptureSummary.containsKey(invokeSite)) {
                        siteRecaptureSummary.put(invokeSite, new HashSet<>());
                    }
                    HashSet<StandardObject> set = siteRecaptureSummary.get(invokeSite);
                    if(existingRecaptureSummaries.containsKey(invokeSite.getMethod())) {
                        HashMap<Integer, HashSet<StandardObject>> existingRecaptureSummary = existingRecaptureSummaries.get(invokeSite.getMethod());
                        PointsToGraph unitPtg = e.getValue().getOut();
                        InvokeExpr invokeExpr= ((JInvokeStmt)unit).getInvokeExpr();
                        for(int i=0; i<invokeExpr.getArgCount(); i++) {
                            Value v = invokeExpr.getArg(i);
                            if (v instanceof NullConstant) continue;
		                    else if (!(v instanceof Local)) continue;
                            if(unitPtg.vars.containsKey(v) && existingRecaptureSummary.containsKey(i)) {
                                Set<ObjectNode> paramObjs = unitPtg.vars.get(v);
                                for(ObjectNode paramObj : paramObjs) {
                                    if(escapeSummary.get(paramObj).containsNoEscape()) {
                                        set.addAll(existingRecaptureSummary.get(i));
                                    }
                                }
                            }
                        }
                        set.addAll(existingRecaptureSummary.get(-2));
                        // System.out.println("RECRE: " + method + "; " + set + "; " + unit + "; " + unitPtg);
                    }
                }
            }
        }
    }

}