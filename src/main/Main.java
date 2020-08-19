package main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import analyser.StaticAnalyser;
import ptg.ObjectNode;
import ptg.PointsToGraph;
import utils.Stats;
import resolver.SummaryResolver;
import es.EscapeStatus;
import soot.PackManager;
import soot.SootMethod;
import soot.Transform;

import utils.GetListOfNoEscapeObjects;


public class Main {
	static HashMap<String, String> paths = new HashMap<>();
	public static void main(String[] args){
		
		GetSootArgs g = new GetSootArgs();
		String[] sootArgs = g.get(args);
		if(sootArgs==null) {
			System.out.println("Unable to generate args for soot!");
			return;
		}
		StaticAnalyser staticAnalyser = new StaticAnalyser();
		PackManager.v().getPack("jtp").add(new Transform("jtp.sample", staticAnalyser));
//		long analysis_start = System.currentTimeMillis();
		soot.Main.main(sootArgs);
//		long analysis_end = System.currentTimeMillis();
		System.out.println("Static Analysis is done!");
		
		
//		staticAnalyser.printAnalysis();
		
//		printResForJVM(StaticAnalyser.summaries, "/home/nikhil/MTP/benchmarks/out", "/home/nikhil/MTP/soot_output_dir/dacapo");
//		printResForJVM(StaticAnalyser.summaries, args[1], args[3]);
		printAllInfo(StaticAnalyser.ptgs, StaticAnalyser.summaries, args[4]);
		/*
		SummaryResolver sr = new SummaryResolver();
		long res_start = System.currentTimeMillis();
		sr.resolve(staticAnalyser.summaries, staticAnalyser.ptgs);
		long res_end = System.currentTimeMillis();
		System.out.println(args[3]);
		System.out.print("Unresolved: ");
		System.out.println("Time Taken:"+(analysis_end-analysis_start)/1000F);
		System.out.println(new Stats(StaticAnalyser.summaries));
		System.out.print("Resolved: ");
		System.out.println("Time Taken:"+(res_end-res_start)/1000F);
		System.out.println(new Stats(sr.solvedSummaries));
		*/
	}
	
	private static void printAllInfo(HashMap<SootMethod, PointsToGraph> ptgs,
			HashMap<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries, String opDir) {
		
		Path p_opDir = Paths.get(opDir);
		ptgs.forEach((method,ptg) -> {
			Path p_opFile = Paths.get(p_opDir.toString() + "/" + method.getDeclaringClass().toString()+".info");
//			System.out.println("Method "+method.toString()+" appends to "+p_opFile);
			StringBuilder output = new StringBuilder();
			output.append(method.toString()+"\n");
			output.append("PTG:\n");
			output.append(ptg.toString());
			output.append("\nSummary\n");
			output.append(summaries.get(method).toString()+"\n");
			try {
				Files.write(p_opFile, output.toString().getBytes(StandardCharsets.UTF_8), 
						Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);				
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("Unable to write info of "+method.toString()+" to file "+p_opFile.toString());
				e.printStackTrace();
			}
		});
	}

	static void printResForJVM(HashMap<SootMethod,HashMap<ObjectNode,EscapeStatus>> summaries, String ipDir, String opDir) {
		// Open File
		Path p_ipDir = Paths.get(ipDir);
		Path p_opDir = Paths.get(opDir);
		
		Path p_opFile = Paths.get(p_opDir.toString() + "/" + p_ipDir.getFileName() + ".res");
		
//		System.out.println(p_opFile);
		// String Builder
		StringBuilder sb = new StringBuilder();
		summaries.forEach((method, summary) ->{
			sb.append(method.getBytecodeSignature().toString());
			sb.append(GetListOfNoEscapeObjects.get(summary));
			sb.append("\n");
		});
		try {
			
			System.out.println("Trying to write to:"+p_opFile);
			Files.write(p_opFile, sb.toString().getBytes(StandardCharsets.UTF_8), 
					Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			System.out.println("Unresolved results have been written.");
		} catch (IOException e) {
			System.out.println("There is an IO exception");
			e.printStackTrace();
		}
	}
	
}
