package main;

import analyser.StaticAnalyser;
import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.PointsToGraph;
import resolver.SummaryResolver;
import resolver.ReworkedResolver;
import soot.PackManager;
import soot.Scene;
import soot.options.Options;
import soot.SootMethod;
import soot.Transform;
import soot.*;
import utils.GetListOfNoEscapeObjects;
import utils.Stats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import static utils.KillCallerOnly.kill;


public class Main {

	public static void main(String[] args) {

		GetSootArgs g = new GetSootArgs();
		String[] sootArgs = g.get(args);
		if (sootArgs == null) {
			System.out.println("Unable to generate args for soot!");
			return;
		}
		StaticAnalyser staticAnalyser = new StaticAnalyser();
		PackManager.v().getPack("jtp").add(new Transform("jtp.sample", staticAnalyser));
		long analysis_start = System.currentTimeMillis();
		Options.v().parse(sootArgs);
		Scene.v().loadNecessaryClasses();
		SootClass s = Scene.v().getSootClass("org.xml.sax.helpers.SecuritySupport");
		System.err.println(s.getMethods());
		System.out.println("Application Classes: "+Scene.v().getApplicationClasses().size());
		PackManager.v().runPacks();
		// soot.Main.main(sootArgs);
		long analysis_end = System.currentTimeMillis();
		System.out.println("Static Analysis is done!");
		System.out.println("Time Taken:"+(analysis_end-analysis_start)/1000F);

		
		boolean useNewResolver = true;
		long res_start = System.currentTimeMillis();
		// Resolver sr;
		if(useNewResolver) {
			ReworkedResolver sr = new ReworkedResolver(staticAnalyser.summaries,
											staticAnalyser.ptgs,
											staticAnalyser.noBCIMethods);
			long res_end = System.currentTimeMillis();
			System.out.println("Resolution is done");
			System.out.println("Time Taken:"+(res_end-res_start)/1000F);
	
			// System.out.println(staticAnalyser.summaries.size()+ " "+staticAnalyser.ptgs.size());
			
			
			HashMap<SootMethod, HashMap<ObjectNode, EscapeStatus>> resolved = (HashMap) kill(sr.solvedSummaries);
			// printAllInfo(StaticAnalyser.ptgs, staticAnalyser.summaries, args[4]);
			
			printAllInfo(StaticAnalyser.ptgs, resolved, args[4]);
	
			saveStats(sr.existingSummaries, resolved, args[4]);
	
			printResForJVM(sr.solvedSummaries, args[2], args[4]);
		}
		else {
			SummaryResolver sr = new SummaryResolver();
			sr.resolve(staticAnalyser.summaries, staticAnalyser.ptgs);
			long res_end = System.currentTimeMillis();
			System.out.println("Resolution is done");
			System.out.println("Time Taken:"+(res_end-res_start)/1000F);
	
			// System.out.println(staticAnalyser.summaries.size()+ " "+staticAnalyser.ptgs.size());
			
			
			HashMap<SootMethod, HashMap<ObjectNode, EscapeStatus>> resolved = (HashMap) kill(sr.solvedSummaries);
			printAllInfo(StaticAnalyser.ptgs, staticAnalyser.summaries, args[4]);
			
			printAllInfo(StaticAnalyser.ptgs, resolved, args[4]);
	
			saveStats(sr.existingSummaries, resolved, args[4]);
	
			printResForJVM(sr.solvedSummaries, args[2], args[4]);
		}
		
		
		
	}

	private static void printAllInfo(Map<SootMethod, PointsToGraph> ptgs,
									 Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries, String opDir) {

		Path p_opDir = Paths.get(opDir);
		for (Map.Entry<SootMethod, PointsToGraph> entry : ptgs.entrySet()) {
			SootMethod method = entry.getKey();
			PointsToGraph ptg = entry.getValue();
			Path p_opFile = Paths.get(p_opDir.toString() + "/" + method.getDeclaringClass().toString() + ".info");
//			System.out.println("Method "+method.toString()+" appends to "+p_opFile);
			StringBuilder output = new StringBuilder();
			output.append(method.toString() + "\n");
			output.append("PTG:\n");
			output.append(ptg.toString());
			output.append("\nSummary\n");
			output.append(summaries.get(method).toString() + "\n");
			try {
				Files.write(p_opFile, output.toString().getBytes(StandardCharsets.UTF_8),
						Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("Unable to write info of " + method.toString() + " to file " + p_opFile.toString());
				e.printStackTrace();
			}
		}
	}
	static String transformFuncSignature(String inputString) {
		StringBuilder finalString = new StringBuilder();
		for(int i=1;i<inputString.length()-1;i++) {
			if(inputString.charAt(i) == '.')
				finalString.append('/');
			else if(inputString.charAt(i) == ':')
				finalString.append('.');
			else if(inputString.charAt(i) == ' ')
				continue;
			else finalString.append(inputString.charAt(i));
		}
		return finalString.toString();
	}
	static void printResForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries, String ipDir, String opDir) {
		// Open File
		Path p_ipDir = Paths.get(ipDir);
		Path p_opDir = Paths.get(opDir);

		Path p_opFile = Paths.get(p_opDir.toString() + "/" + p_ipDir.getFileName() + ".res");

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : summaries.entrySet()) {
			SootMethod method = entry.getKey();
			HashMap<ObjectNode, EscapeStatus> summary = entry.getValue();
			sb.append(transformFuncSignature(method.getBytecodeSignature()));
			sb.append(" ");
			sb.append(GetListOfNoEscapeObjects.get(summary));
			sb.append("\n");
		}
		try {
			System.out.println("Trying to write to:" + p_opFile);
			Files.write(p_opFile, sb.toString().getBytes(StandardCharsets.UTF_8),
					Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			System.out.println("Results have been written.");
		} catch (IOException e) {
			System.out.println("There is an exception"+e);
			e.printStackTrace();
		}
	}

	static void saveStats(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> unresolved,
						  Map<SootMethod, HashMap<ObjectNode, EscapeStatus>>resolved,
						  String opDir) {
		Stats beforeResolution = new Stats(unresolved);
		System.out.println("calculating stats for solvedsummaries");
		Stats afterResolution = new Stats(resolved);
		Path p_opFile = Paths.get(opDir + "/stats.txt");
		StringBuilder sb = new StringBuilder();
		sb.append("Before resolution:\n"+beforeResolution);
		sb.append("\nAfter resolution:\n"+afterResolution);
		sb.append("\n");
		try {
			System.out.println("Trying to write to:" + p_opFile);
			Files.write(p_opFile, sb.toString().getBytes(StandardCharsets.UTF_8),
					Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			System.out.println("Stats have been written.");
		} catch (IOException e) {
			System.out.println("There is an exception"+e);
			e.printStackTrace();
		}

	}

}
