package analyser;

import es.EscapeStatus;
import es.Escape;
import es.NoEscape;
import handlers.JAssignStmt.JAssignStmtHandler;
import handlers.*;
import ptg.*;
import soot.*;
import utils.IllegalBCIException;
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
	private boolean allNonEscaping;
	public static Map<SootMethod, PointsToGraph> ptgs;
	public static Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries;
	public static Map<SootMethod, ArrayList<ObjectNode>> stackOrders;
	public static LinkedHashMap<Body, Analysis> analysis;
	public static List<SootMethod> noBCIMethods; 
	String[] ignoreFuncs = {
							// "<java.util.ArrayPrefixHelpers$CumulateTask: compute()V>",
							// "<java.util.ArrayPrefixHelpers$DoubleCumulateTask: compute()V>",
							// "<java.util.ArrayPrefixHelpers$IntCumulateTask: compute()V>",
							// "<java.util.ArrayPrefixHelpers$LongCumulateTask: compute()V>",
							// "<java.util.concurrent.ConcurrentHashMap: readObject(Ljava/io/ObjectInputStream;)V>",
							// "<java.util.concurrent.ConcurrentHashMap$TreeBin: <init>(Ljava/util/concurrent/ConcurrentHashMap$TreeNode;)V>",
							// "<java.util.HashMap$TreeNode: treeify([Ljava/util/HashMap$Node;)V>",
							// "<java.util.concurrent.ConcurrentHashMap: transfer([Ljava/util/concurrent/ConcurrentHashMap$Node;[Ljava/util/concurrent/ConcurrentHashMap$Node;)V>",
							// "<java.util.concurrent.ConcurrentHashMap$TreeBin: removeTreeNode(Ljava/util/concurrent/ConcurrentHashMap$TreeNode;)Z>",
							// "<java.util.HashMap$TreeNode: putTreeVal(Ljava/util/HashMap;[Ljava/util/HashMap$Node;ILjava/lang/Object;Ljava/lang/Object;)Ljava/util/HashMap$TreeNode;>",
							// "<java.util.HashMap$TreeNode: removeTreeNode(Ljava/util/HashMap;[Ljava/util/HashMap$Node;Z)V>"



							// "<org.apache.jasper.servlet.JspCServletContext: getResource(Ljava/lang/String;)Ljava/net/URL;>"
							// "<org.apache.jasper.compiler.ImplicitTagLibraryInfo: <init>(Lorg/apache/jasper/JspCompilationContext;Lorg/apache/jasper/compiler/ParserController;Lorg/apache/jasper/compiler/PageInfo;Ljava/lang/String;Ljava/lang/String;Lorg/apache/jasper/compiler/ErrorDispatcher;)V>"
							// "<org.apache.crimson.util.MessageCatalog: getMessage(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;>",
							// "<org.eclipse.osgi.internal.loader.BundleLoader: createBCL(Lorg/eclipse/osgi/framework/adaptor/BundleProtectionDomain;[Ljava/lang/String;)Lorg/eclipse/osgi/framework/adaptor/BundleClassLoader;>"
							// "<org.dacapo.harness.H2: prepare()V>",
							// "<jdk.internal.org.objectweb.asm.ClassWriter: newUTF8(Ljava/lang/String;)I>"
							// "<sun.invoke.util.BytecodeDescriptor: unparse(Ljava/lang/Class;)Ljava/lang/String;>",
							// "<java.util.ArrayList$Itr: <init>(Ljava/util/ArrayList;)V>",
							// "<sun.misc.IOUtils: readNBytes(Ljava/io/InputStream;I)[B>",
							// "<java.util.ArrayList: iterator()Ljava/util/Iterator;>",
							// "<sun.util.locale.UnicodeLocaleExtension: <init>(Ljava/util/SortedSet;Ljava/util/SortedMap;)V>",
							// "<java.lang.reflect.Parameter: toString()Ljava/lang/String;>",
							// "<jdk.internal.org.objectweb.asm.ClassWriter: newUTF8(Ljava/lang/String;)I>",
							// "<org.eclipse.jdt.internal.core.index.DiskIndex: addQueryResult(Lorg/eclipse/jdt/internal/compiler/util/HashtableOfObject;[CLorg/eclipse/jdt/internal/compiler/util/HashtableOfObject;Lorg/eclipse/jdt/internal/core/index/MemoryIndex;)Lorg/eclipse/jdt/internal/compiler/util/HashtableOfObject;>"
							// "<sun.security.util.DisabledAlgorithmConstraints$Constraints: <init>(Ljava/lang/String;Ljava/util/List;)V>",
							// "<java.util.HashMap: getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;>"
							// "<java.lang.String: substring(II)Ljava/lang/String;>",
							"<org.sunflow.image.Color: blend(Lorg/sunflow/image/Color;Lorg/sunflow/image/Color;Lorg/sunflow/image/Color;)Lorg/sunflow/image/Color;>",
							"<org.sunflow.image.Color: blend(Lorg/sunflow/image/Color;Lorg/sunflow/image/Color;Lorg/sunflow/image/Color;Lorg/sunflow/image/Color;)Lorg/sunflow/image/Color;>"
						};

	// Handle where returned parameters are further marked as escaping. This value needs to be propagated.
	
	List<String> sArrays = Arrays.asList(ignoreFuncs);

	public StaticAnalyser() {
		super();
		analysis = new LinkedHashMap<>();
		ptgs = new ConcurrentHashMap<>();
		summaries = new ConcurrentHashMap<>();
		stackOrders = new ConcurrentHashMap<>();
		noBCIMethods = new ArrayList<>();
		allNonEscaping = false;
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

		if (body.getMethod().isJavaLibraryMethod()) {
			return;
		}

		// System.out.println("Method Name: "+ body.getMethod().getBytecodeSignature() + ":"+body.getMethod().getName());
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
		// String dataString = body.toString();
		// Matcher m = Pattern.compile("\n").matcher(dataString);
		// int lines = 1;
		// while (m.find()) 
		// 	lines++;
		
		// System.out.println(body.getMethod()+" "+lines);
		// System.out.println(body);
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
			if(i == 1000) {
				return;
			}
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
			// ObjectNode scrutinyObject = new ObjectNode(17, ObjectType.internal);
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
				// System.err.println("--------------------------------");
				for (Unit pred : cfg.getPredsOf(u)) {
					// if(u.toString().contains("$r14 := @caughtexception")){
						// System.err.println("Predecessor of our unit is:"+pred+ " "+flowSets.get(pred).getOut());
						// System.err.println("Pred of pred: "+cfg.getPredsOf(pred) );
						// for (Unit t: cfg.getPredsOf(pred)) {
						// 	System.err.println(t+"::::"+flowSets.get(t).getOut());
						// }
					// }
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
					// System.err.println(u);
					// System.err.println("outNew:"+outNew);
					apply(body.getMethod(), u, outNew, summary);
					// System.err.println("outNew:"+outNew);
					// int sz = getSummarySize(summary);
					// System.out.println("Final Summary Size: "+ sz);
					// if (sz > 10000)
					// 	return;
				} catch (IllegalBCIException e) {
					noBCIMethods.add(body.getMethod());
					setParamsAsEscaping(body.getMethod(),summary);
					summaries.put(body.getMethod(), summary);
					String s = "->*** Error at: " + u.toString() + " of " + body.getMethod().getBytecodeSignature();
					System.err.println(s);
					System.err.println(e);
					return;
				} catch (Exception e) {
					String s = "->*** Error at: " + u.toString() + " of " + body.getMethod().getBytecodeSignature();
					System.err.println(s);
					System.err.println("inNew:"+inNew);
					System.err.println("outNew:"+outNew);
					System.err.println("body:"+body);
					// System.err.println("summary:"+summary);
//					System.out.println(workList);
					throw e;
				}
				if(verboseFlag) {
					System.out.println("at: "+u.toString());
					System.out.println("inNew:"+inNew.toString());
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
					// System.err.println("Succ of "+u+" "+cfg.getSuccsOf(u));
					// flowSet.setOut(outNew);
					flowSet.getOut().union(outNew);
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
		if (allNonEscaping) {
			markAsNonEscaping(summary);
			markAsEscaping(JInvokeStmtHandler.nativeLocals.get(body.getMethod()), summary, ptg);
		}
		summaries.put(body.getMethod(), summary);
		System.out.println("Method Name: "+ body.getMethod().getBytecodeSignature() + ":"+body.getMethod().getName());
	}

	private void markAsEscaping(List<Local> nativeList, Map<ObjectNode, EscapeStatus> summary, PointsToGraph ptg) {
		if (nativeList == null) 
			return;
		for (Local obj: nativeList) {
			// System.out.println(ptg);
			// System.out.println(summary);
			// System.out.println("Escap: "+obj);
			ptg.cascadeEscape(obj, summary);
		}
	}

	private void markAsNonEscaping(Map<ObjectNode, EscapeStatus> summary) {
		for (ObjectNode obj: summary.keySet()) {
			EscapeStatus es = new EscapeStatus(NoEscape.getInstance());
			summary.put(obj, es);
		}
	}

	private void setParamsAsEscaping(SootMethod m, Map<ObjectNode, EscapeStatus> summary) {
		summary.clear();
		for (int i=0; i< m.getParameterCount(); i++) {
			summary.put(new ObjectNode(i, ObjectType.parameter), new EscapeStatus(Escape.getInstance()));
		}
	}

	/*
	 * apply will apply the changes of the current unit on the provided
	 * points-to graph. Note that this will NOT make a copy to make
	 * changes on.
	 */

	public void apply(SootMethod m, Unit u, PointsToGraph ptg, Map<ObjectNode, EscapeStatus> summary) {
		// System.err.println(u+" "+u.getClass().getName());

		// System.out.println("PRIYAM soot unit " + u);
		// System.out.println("PRIYAM soot ptg " + ptg);

		if (u instanceof JAssignStmt) {
			JAssignStmtHandler.handle(m, u, ptg, summary);
		} else if (u instanceof JIdentityStmt) {
			JIdentityStmtHandler.handle(m, u, ptg, summary);
		} else if (u instanceof JInvokeStmt) {
			JInvokeStmtHandler.handle(m, u, ptg, summary);
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
