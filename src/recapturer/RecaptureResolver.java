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
import handlers.JAssignStmt.JAssignStmtHandler;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Ref;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.NullConstant;
import java.util.*;
import java.io.*;

public class RecaptureResolver {
    public Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> escapeSummaries;
    public Map<SootMethod, HashMap<ObjectNode, HashSet<StandardObject>>> existingRecaptureSummaries;
    public Map<SootMethod, HashMap<InvokeSite, HashSet<StandardObject>>> siteRecaptureSummaries;
    public static LinkedHashMap<Body, Analysis> analysis;
    // Map<SootMethod, HashSet<SootMethod>> adjCallGraph;
    public HashMap<StandardObject, Set<StandardObject> > graph;

    public RecaptureResolver(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> solvedSummaries,
            Map<SootMethod, HashMap<ObjectNode, HashSet<StandardObject>>> recaptureSummaries,
            Map<SootMethod, PointsToGraph> ptgs,
            HashMap<StandardObject, Set<StandardObject> > graph,
            LinkedHashMap<Body, Analysis> analysis) {
        
        this.escapeSummaries = solvedSummaries;
        this.existingRecaptureSummaries = recaptureSummaries;
        this.analysis = analysis;
        this.siteRecaptureSummaries = new HashMap<>();
        this.graph = graph;
        for(Map.Entry<StandardObject, Set<StandardObject>> entry: this.graph.entrySet()) {
            // System.out.println(entry.getKey() + " " + entry.getKey().hashCode() + " GRAPH " + entry.getValue());
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
                PointsToGraph unitPtg = e.getValue().getOut();
                // System.out.println("RECRE: " + method + ": " + e.getKey() + ": " + e.getValue().getOut());
                if(unit instanceof JAssignStmt) {
                    Value lhs = ((JAssignStmt) unit).getLeftOp();
                    Value rhs = ((JAssignStmt) unit).getRightOp();
                    if(rhs instanceof InvokeExpr) {
                        InvokeSite invokeSite = new InvokeSite(((InvokeExpr) rhs).getMethod(), unit);
                        handleInvokeExpr(invokeSite, ((InvokeExpr) rhs), escapeSummary, siteRecaptureSummary, unitPtg);
                        if(existingRecaptureSummaries.containsKey(invokeSite.getMethod())) {
                            HashSet<StandardObject> set = siteRecaptureSummary.get(invokeSite);
                            HashSet<StandardObject> retReachables = new HashSet<>(); 
                            HashMap<ObjectNode, HashSet<StandardObject>> existingRecaptureSummary = existingRecaptureSummaries.get(invokeSite.getMethod());
                            for(Map.Entry<ObjectNode, HashSet<StandardObject>> exRecap : existingRecaptureSummary.entrySet()) {
                                if(exRecap.getKey().type != ObjectType.parameter) {
                                    retReachables.addAll(exRecap.getValue());
                                }
                            }
                            if(lhs instanceof Local && unitPtg.vars.containsKey(lhs)) {
                                Set<ObjectNode> paramObjs = unitPtg.vars.get(lhs);
                                boolean flag = true;
                                for(ObjectNode paramObj : paramObjs) {
                                    if(escapeSummary.get(paramObj).doesEscape()) {
                                        flag = false;
                                        break;
                                    }
                                }
                                if(flag) {
                                    set.addAll(retReachables);
                                }
                            }
                        }
                    }
                }
                else if(unit instanceof JInvokeStmt) {
                    InvokeSite invokeSite = new InvokeSite(((JInvokeStmt) unit).getInvokeExpr().getMethod(), unit);
                    handleInvokeExpr(invokeSite, ((JInvokeStmt) unit).getInvokeExpr(), escapeSummary, siteRecaptureSummary, unitPtg);
                }
            }
        }
    }

    void handleInvokeExpr(InvokeSite invokeSite,
                        InvokeExpr expr,
                        HashMap<ObjectNode, EscapeStatus> escapeSummary, 
                        HashMap<InvokeSite, HashSet<StandardObject>> siteRecaptureSummary,
                        PointsToGraph unitPtg) {
        if(!siteRecaptureSummary.containsKey(invokeSite)) {
            siteRecaptureSummary.put(invokeSite, new HashSet<>());
        }
        if(existingRecaptureSummaries.containsKey(invokeSite.getMethod())) {
            HashSet<StandardObject> set = siteRecaptureSummary.get(invokeSite);
            HashMap<ObjectNode, HashSet<StandardObject>> existingRecaptureSummary = existingRecaptureSummaries.get(invokeSite.getMethod());
            for(Map.Entry<ObjectNode, HashSet<StandardObject>> recap : existingRecaptureSummary.entrySet()) {
                ObjectNode param = recap.getKey();
                if(param.type == ObjectType.parameter && param.ref > -1) {
                    Value v = expr.getArg(param.ref);
                    if (v instanceof NullConstant) continue;
                    else if (!(v instanceof Local)) continue;
                    Set<ObjectNode> paramObjs = unitPtg.vars.get(v);
                    boolean flag = true;
                    for(ObjectNode paramObj : paramObjs) {
                        if(escapeSummary.get(paramObj).doesEscape()) {
                            flag = false;
                            break;
                        }
                    }
                    if(flag) {
                        set.addAll(recap.getValue());
                    }
                }
            }
            // for(int i=0; i<invokeExpr.getArgCount(); i++) {
            //     Value v = invokeExpr.getArg(i);
            //     if (v instanceof NullConstant) continue;
            //     else if (!(v instanceof Local)) continue;
            //     if(unitPtg.vars.containsKey(v) && existingRecaptureSummary.containsKey(i)) {
            //         Set<ObjectNode> paramObjs = unitPtg.vars.get(v);
            //         for(ObjectNode paramObj : paramObjs) {
            //             if(escapeSummary.get(paramObj).containsNoEscape()) {
            //                 set.addAll(existingRecaptureSummary.get(i));
            //             }
            //         }
            //     }
            // }
            // set.addAll(existingRecaptureSummary.get(-2));
            // System.out.println("RECRE: " + method + "; " + set + "; " + unit + "; " + unitPtg);
        }
    }

}