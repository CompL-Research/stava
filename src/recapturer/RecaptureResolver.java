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
    public Map<SootMethod, HashMap<Integer, HashMap<SootMethod, HashSet<Integer>>>> siteSummaries;
    public static LinkedHashMap<Body, Analysis> analysis;

    public RecaptureResolver(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> solvedSummaries,
            Map<SootMethod, HashMap<ObjectNode, HashSet<StandardObject>>> recaptureSummaries,
            Map<SootMethod, PointsToGraph> ptgs,
            LinkedHashMap<Body, Analysis> analysis) {
        
        this.escapeSummaries = solvedSummaries;
        this.existingRecaptureSummaries = recaptureSummaries;
        this.analysis = analysis;
        this.siteRecaptureSummaries = new HashMap<>();
        this.siteSummaries = new HashMap<>();

        SolveSummaries();
        populateSummaries();
    }

    void populateSummaries() {
        for(Map.Entry<SootMethod, HashMap<InvokeSite, HashSet<StandardObject>>> entry : siteRecaptureSummaries.entrySet()) {
            if(!siteSummaries.containsKey(entry.getKey()))
                siteSummaries.put(entry.getKey(), new HashMap<>());
            Map<Integer, HashMap<SootMethod, HashSet<Integer>>> siteSummary = siteSummaries.get(entry.getKey());
            for(Map.Entry<InvokeSite, HashSet<StandardObject>> e : entry.getValue().entrySet()) {
                if(!siteSummary.containsKey(e.getKey().getSite()))
                    siteSummary.put(e.getKey().getSite(), new HashMap<>());
                HashMap<SootMethod, HashSet<Integer>> siteInfo = siteSummary.get(e.getKey().getSite());
                HashSet<Integer> recapObjRefs = new HashSet<>();
                for(StandardObject sObj : e.getValue()) {
                    recapObjRefs.add(sObj.getObject().ref);
                }
                siteInfo.put(e.getKey().getMethod(), recapObjRefs);
            }
        }
    }

    void SolveSummaries() {
        CallGraph cg = Scene.v().getCallGraph();
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
                if(unit instanceof JAssignStmt) {
                    Value lhs = ((JAssignStmt) unit).getLeftOp();
                    Value rhs = ((JAssignStmt) unit).getRightOp();
                    if(rhs instanceof InvokeExpr) {
                        Iterator<Edge> it = cg.edgesOutOf(unit);
                        while(it.hasNext()) {
                            SootMethod tgt = it.next().getTgt().method();
                            InvokeSite invokeSite = new InvokeSite(tgt, unit);
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
                                // for(Map.Entry<ObjectNode, EscapeStatus> esEntry : escapeSummaries.get(invokeSite.getMethod()).entrySet()) {
                                //     if(esEntry.getValue().containsNoEscape()) {
                                //         set.add(new StandardObject(invokeSite.getMethod(), esEntry.getKey()));
                                //     }
                                // }
                            }
                        }
                    }
                }
                else if(unit instanceof JInvokeStmt) {
                    Iterator<Edge> it = cg.edgesOutOf(unit);
                    while(it.hasNext()) {
                        SootMethod tgt = it.next().getTgt().method();
                        InvokeSite invokeSite = new InvokeSite(tgt, unit);
                        handleInvokeExpr(invokeSite, ((JInvokeStmt) unit).getInvokeExpr(), escapeSummary, siteRecaptureSummary, unitPtg);
                    }
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
        }
    }

}