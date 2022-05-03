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

public class InlineRecapture {
    public Map<SootMethod, HashMap<ObjectNode, HashSet<StandardObject>>> recaptureSummaries;
    public Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> escapeSummaries;
    public static LinkedHashMap<Body, Analysis> analysis;
    Map<SootMethod, PointsToGraph> ptgs;

    public InlineRecapture(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> existingSummaries,
            Map<SootMethod, PointsToGraph> ptgs,
            LinkedHashMap<Body, Analysis> analysis) {
        
        this.escapeSummaries = existingSummaries;
        this.ptgs = ptgs;
        this.analysis = analysis;

        recaptureSummaries = new HashMap<>();
        
        AddRecaptureSummaries();
        System.out.println("RECSUM::" + recaptureSummaries);
    }

    void AddRecaptureSummaries() {
        HashMap<SootMethod, HashSet<ObjectNode>> externalReachables = new HashMap<>();
        for (Map.Entry<SootMethod, PointsToGraph> entry : ptgs.entrySet()) {
            SootMethod method = entry.getKey();
            PointsToGraph ptg = entry.getValue();
            if(!this.recaptureSummaries.containsKey(method))
                this.recaptureSummaries.put(method, new HashMap<>());
            HashMap<ObjectNode, HashSet<StandardObject>>  recaptureSummary = this.recaptureSummaries.get(method);
            HashMap<ObjectNode, EscapeStatus> escapeSummary = this.escapeSummaries.get(method);
            
            // HashSet<ObjectNode> set = new HashSet<>();
            HashMap<ObjectNode, HashSet<ObjectNode>> hmap = new HashMap<>();
            HashSet<ObjectNode> externalReachSet = new HashSet<>();
            for(Map.Entry<Local, Set<ObjectNode>> var : ptg.vars.entrySet()) {
                for(ObjectNode o : var.getValue()) {
                    if(o.type == ObjectType.parameter) {
                        if(!hmap.containsKey(o)) {
                            hmap.put(o, new HashSet<>());
                        }
                        hmap.get(o).addAll((HashSet<ObjectNode>) ptg.reachables(o));
                    }
                    // if(o.type == ObjectType.returnValue) {
                    //     set.add(o);
                    //     set.addAll((HashSet<ObjectNode>) ptg.reachables(o));
                    // }
                    if(o.type == ObjectType.external) {
                        externalReachSet.addAll((HashSet<ObjectNode>) ptg.reachables(o));
                    }
                }
            }
            if(JReturnStmtHandler.returnedObjects.containsKey(method)){
                HashSet<ObjectNode> retObjs = JReturnStmtHandler.returnedObjects.get(method);
                for(ObjectNode o : retObjs) {
                    if(!hmap.containsKey(o)) {
                        hmap.put(o, new HashSet<>());
                    }
                    hmap.get(o).addAll((HashSet<ObjectNode>) ptg.reachables(o));
                }
            }

            if(!externalReachables.containsKey(method)) {
                externalReachables.put(method, externalReachSet);
            }

            for(Map.Entry<ObjectNode, HashSet<ObjectNode>> t : hmap.entrySet()) {
                HashSet<StandardObject> set = new HashSet<>();
                for(ObjectNode o : t.getValue()) {
                    if(o.type != ObjectType.parameter && o.type != ObjectType.external && !externalReachSet.contains(o))
                        set.add(new StandardObject(method, o));
                }
                if(!recaptureSummary.containsKey(t.getKey())) {
                    recaptureSummary.put(t.getKey(), set);
                }
                else {
                    recaptureSummary.get(t.getKey()).addAll(set);
                }
            }
            // System.out.println("INREC: " + method + ": " + recaptureSummary);
        }

        // for(Map.Entry<Body, Analysis> entry : analysis.entrySet()) {
        //     SootMethod method = entry.getKey().getMethod();
        //     Map<Unit, FlowSet> flowsets = entry.getValue().getFlowSets();
        //     if(!this.recaptureSummaries.containsKey(method))
        //         this.recaptureSummaries.put(method, new HashMap<>());
        //     HashMap<Integer, HashSet<StandardObject>>  recaptureSummary = this.recaptureSummaries.get(method);
        //     HashMap<ObjectNode, EscapeStatus> escapeSummary = this.escapeSummaries.get(method);
        //     HashSet<ObjectNode> set = new HashSet<>();
        //     for(Map.Entry<Unit, FlowSet> e : flowsets.entrySet()) {
        //         Unit unit = e.getKey();
        //         PointsToGraph unitPtg = e.getValue().getOut();
        //         if(unit instanceof JReturnStmt) {
        //             Value v = ((JReturnStmt)unit).getOp();
        //             if (v instanceof NullConstant) continue;
	    //         	else if (!(v instanceof Local)) continue;
        //             set.addAll((HashSet<ObjectNode>) unitPtg.reachables((Local)v));
        //         }
        //     }
        //     set.removeAll(externalReachables.get(method));
        //     if(!recaptureSummary.containsKey(-2)) {
        //         recaptureSummary.put(-2, new HashSet<>());
        //     }
        //     for(ObjectNode o : set) {
        //         recaptureSummary.get(-2).add(new StandardObject(method, o));
        //     }
        // }
    }

}