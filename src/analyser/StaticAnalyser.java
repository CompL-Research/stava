package analyser;

import es.EscapeStatus;
import handlers.JAssignStmt.JAssignStmtHandler;
import handlers.*;
import ptg.*;
import soot.*;
import soot.jimple.MonitorStmt;
import soot.jimple.internal.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.*;


public class StaticAnalyser extends BodyTransformer {
	public static Map<SootMethod, PointsToGraph> ptgs;
	public static Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries;
	public static LinkedHashMap<Body, Analysis> analysis;

	String[] ignoreFuncs = {
							// "<java.util.ArrayPrefixHelpers$CumulateTask: compute()V>",
							// "<java.util.ArrayPrefixHelpers$DoubleCumulateTask: compute()V>",
							// "<java.util.ArrayPrefixHelpers$IntCumulateTask: compute()V>",
							// "<java.util.ArrayPrefixHelpers$LongCumulateTask: compute()V>",
							// "<java.util.concurrent.ConcurrentHashMap: readObject(Ljava/io/ObjectInputStream;)V>",
							// "<java.util.concurrent.ConcurrentHashMap$TreeBin: <init>(Ljava/util/concurrent/ConcurrentHashMap$TreeNode;)V>",
							"<java.util.HashMap$TreeNode: treeify([Ljava/util/HashMap$Node;)V>",
							// "<java.util.concurrent.ConcurrentHashMap: transfer([Ljava/util/concurrent/ConcurrentHashMap$Node;[Ljava/util/concurrent/ConcurrentHashMap$Node;)V>",
							// "<java.util.concurrent.ConcurrentHashMap$TreeBin: removeTreeNode(Ljava/util/concurrent/ConcurrentHashMap$TreeNode;)Z>",
							// "<java.util.HashMap$TreeNode: putTreeVal(Ljava/util/HashMap;[Ljava/util/HashMap$Node;ILjava/lang/Object;Ljava/lang/Object;)Ljava/util/HashMap$TreeNode;>",
							// "<java.util.HashMap$TreeNode: removeTreeNode(Ljava/util/HashMap;[Ljava/util/HashMap$Node;Z)V>"
							 };
	
	List<String> sArrays = Arrays.asList(ignoreFuncs);

	public StaticAnalyser() {
		super();
		analysis = new LinkedHashMap<>();
		ptgs = new ConcurrentHashMap<>();
		summaries = new ConcurrentHashMap<>();
	}

	private int getSummarySize(HashMap<ObjectNode, EscapeStatus> summary)
	{
		return summary.toString().length();
	}

	@Override
	protected void internalTransform(Body body, String phasename, Map<String, String> options) {
		// if (body.getMethod().getBytecodeSignature().toString()
		// 	.compareTo("<java.util.HashMap$TreeNode: treeify([Ljava/util/HashMap$Node;)V>") != 0)
		// {
		// 	return;
		// }
		// if ( !this.sArrays.contains(body.getMethod().getBytecodeSignature()))
		// 	return;

		System.out.println("Method Name: "+ body.getMethod().getBytecodeSignature() + ":"+body.getMethod().getName());
//		if(body.getMethod().getName().contains("<clinit>")){
//			System.out.println("Skipping this method");
//			return;
//		}

		boolean verboseFlag = false;
//		if(body.getMethod().getBytecodeSignature().equals("<moldyn.md: <init>()V>")) {
//			verboseFlag = true;
//			System.out.println(body.getMethod().toString());
//		}
		// System.out.println(body);
		String dataString = body.toString();
		Matcher m = Pattern.compile("\n").matcher(dataString);
		int lines = 1;
		while (m.find()) 
			lines++;
		
		System.out.println(body.getMethod()+" "+lines);
		System.out.println(body);
		// verboseFlag = true;
		// if (true)
		// 	return;
		// System.out.println("func: "+body.getMethod().toString() +" "+ body.getMethod().isJavaLibraryMethod());
		
		// if (true) // Ignore Library Methods.
		// 	return;
		// if (body.getMethod().toString().compareTo("<jdk.internal.org.objectweb.asm.Label: void visitSubroutine(jdk.internal.org.objectweb.asm.Label,long,int)>") == 0)
		// {
		// 	return;
		// }
		
		String path = Scene.v().getSootClassPath();
//		System.out.println(path);
//		System.out.println("Package:"+body.getMethod().getDeclaringClass().getJavaPackageName());
		Path p = Paths.get(path.substring(0, path.indexOf(":")) + "/" + body.getMethod().getDeclaringClass().toString() + ".res");
//		System.out.println(".res file path:"+p);
		HashMap<ObjectNode, EscapeStatus> summary = new HashMap<>();
		if (verboseFlag) System.out.println(body);

		PatchingChain<Unit> units = body.getUnits();

		// The flowSets
		Map<Unit, FlowSet> flowSets = new LinkedHashMap<>(units.size());

		ExceptionalUnitGraph cfg = new ExceptionalUnitGraph(body);

		LinkedHashSet<Unit> workList = new LinkedHashSet<Unit>();
		LinkedHashSet<Unit> workListNext = new LinkedHashSet<Unit>(units);
		LinkedHashSet<Unit> temp = null;
		// initialize the flow sets with empty sets
		for (Unit u : units) {
			flowSets.put(u, new FlowSet());
		}

		int i = 0;
		while (!workListNext.isEmpty()) {
			if(i==1000) System.out.println("Crossed "+i+" loops");
//			if (verboseFlag) {
//				System.out.println("Loop " + i);
//				System.out.println("Worklist:");
//				workList.forEach(w -> System.out.println(w));
//				System.out.println("WorkListNext:");
//				workListNext.forEach(w -> System.out.println(w));
//			}
			/*
			 * Swap workList and workListNext
			 */
			temp = workList;
			workList = workListNext;
			workListNext = temp;
			workListNext.clear();

			/*
			 * Main Work Loop:
			 * for each Unit u in workList
			 * 1. inNew = union(out[predecessors])
			 * 2. outNew = apply(u, inNew)
			 * 3. if(outNew != out[u]):
			 * 		add successors to workListNext
			 * 		out[u] = outNew
			 */
			ObjectNode scrutinyObject = new ObjectNode(17, ObjectType.internal);
			Iterator<Unit> iterator = workList.iterator();
			while (iterator.hasNext()) {
				Unit u = iterator.next();
//				if(verboseFlag) System.out.println("Unit: "+u);
				iterator.remove();
				workListNext.remove(u);
				FlowSet flowSet = flowSets.get(u);
				/*
				 * 1. inNew = union(out[predecessors])
				 */
				PointsToGraph inNew = new PointsToGraph();
				for (Unit pred : cfg.getPredsOf(u)) {
//					if(u.toString().contains("$r14 := @caughtexception")){
//						System.out.println("Predecessor of our unit is:"+pred);
//					}
					inNew.union(flowSets.get(pred).getOut());
				}
				if (inNew.equals(flowSet.getIn()) && !inNew.isEmpty()) {
					workListNext.removeAll(cfg.getSuccsOf(u));
					continue;
				}
				flowSet.setIn(inNew);

				/*
				 * 2. outNew = apply(u, inNew)
				 */
				PointsToGraph outNew = new PointsToGraph(inNew);
				try {
					// if (verboseFlag) System.out.println("Applied changes to: " + u);
					// System.out.println("Initial Summary Size: "+ getSummarySize(summary));
					apply(body.getMethod(), u, outNew, summary);
					// int sz = getSummarySize(summary);
					// System.out.println("Final Summary Size: "+ sz);
					// if (sz > 10000)
					// 	return;
				} catch (Exception e) {
					String s = "->*** Error at: " + u.toString() + " of " + body.getMethod().getBytecodeSignature();
					System.out.println(s);
					System.out.println("inNew:"+inNew);
					System.out.println("outNew:"+outNew);
					System.out.println("body:"+body);
					System.out.println("summary:"+summary);
//					System.out.println(workList);
					throw e;
				}
				if(verboseFlag) {
					// System.out.println("at: "+u.toString());
					// System.out.println("inNew:"+inNew.toString());
					System.out.println("outNew:"+outNew.toString());
					// System.out.println("summary:"+summary);
				}
//				if(bci == 135) {
//					System.out.println("outNew:"+outNew);
//				}
//				if(verboseFlag && summary.containsKey(scrutinyObject)) {
//					System.out.println("after "+u.toString()+" summary["+scrutinyObject.toString()+"] = "+summary.get(scrutinyObject).toString());
//				}
				/*
				 * 3. if(outNew != out[u]):
				 * 		add successors to workList
				 * 		out[u] = outNew
				 */
				if (!outNew.equals(flowSet.getOut())) {
//					if(i>75 && body.getMethod().toString().contains("visitMaxs"))System.out.println("OutNew is new:"+outNew.toString());
					workListNext.addAll(cfg.getSuccsOf(u));
					flowSet.setOut(outNew);
				} else {
//					if(i>75 && body.getMethod().toString().contains("visitMaxs"))System.out.println("OutOld (remains same):"+flowSet.getOut().toString());
				}
			}
			i += 1;

		}
		if (verboseFlag) {
			System.out.println("Finished analysis for:" + body.getMethod().getBytecodeSignature());
		}
//		Analysis currentAnalysis = new Analysis(flowSets, summary);
//		analysis.put(body, currentAnalysis);
//		String output = body.getMethod().getSignature()+"\n"+currentAnalysis.toString();

		/*
		 *

		try {
			Files.write(p, output.getBytes(StandardCharsets.UTF_8),
					Files.exists(p) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		Iterator<Entry<Unit, FlowSet>> iterator = flowSets.entrySet().iterator();
		Entry<Unit, FlowSet> elem = iterator.next();
		while (iterator.hasNext()) elem = iterator.next();
		PointsToGraph ptg = elem.getValue().getOut();
		ptgs.put(body.getMethod(), ptg);
		summaries.put(body.getMethod(), summary);
		System.out.println("Method Name: "+ body.getMethod().getBytecodeSignature() + ":"+body.getMethod().getName());
	}

	/*
	 * apply will apply the changes of the current unit on the provided
	 * points-to graph. Note that this will NOT make a copy to make
	 * changes on.
	 */

	public void apply(SootMethod m, Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		if (u instanceof JAssignStmt) {
			JAssignStmtHandler.handle(u, ptg, summary);
		} else if (u instanceof JIdentityStmt) {
			JIdentityStmtHandler.handle(m, u, ptg, summary);
		} else if (u instanceof JInvokeStmt) {
			JInvokeStmtHandler.handle(u, ptg, summary);
		} else if (u instanceof JReturnVoidStmt) {
			// Nothing to do here!
		} else if (u instanceof JReturnStmt) {
			JReturnStmtHandler.handle(m, u, ptg, summary);
		} else if (u instanceof JThrowStmt) {
			JThrowStmtHandler.handle(u, ptg, summary);
		} else if (u instanceof MonitorStmt) {
			MonitorStmtHandler.handle(u, ptg, summary);
		} else if (u instanceof JIfStmt || u instanceof JGotoStmt ||
				u instanceof JTableSwitchStmt || u instanceof JLookupSwitchStmt) {
		} else {
			System.out.println("Unidentified class: " + u.getClass() + " with BCI " + utils.getBCI.get(u) + " at:\n" + u);
			throw new IllegalArgumentException(u.toString());
		}
	}

	public void printAnalysis() {
		for (Map.Entry<Body, Analysis> entry : analysis.entrySet()) {
			System.out.println("Class: " + entry.getKey().getMethod().getDeclaringClass());
			System.out.println("Method: " + entry.getKey().getMethod().getName());
			System.out.println("Analysis:\n" + entry.getValue());
		}
	}

	private String printList(LinkedHashSet<Unit> l) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		l.forEach(u -> {
			int ref = -1;
			try {
				ref = utils.getBCI.get(u);
			} catch (Exception e) {
				// do nothing
				System.out.println("[StaticAnalyser.printList] [Apology] I was trying to print list of BCI. Sorry!");
			}
			sb.append(ref);
			sb.append(",");
		});
		sb.append("]");
		return sb.toString();
	}

}
