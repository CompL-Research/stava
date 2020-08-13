package main;

public class GetSootArgs {
	public String[] get(String[] args) {
		// correct one
		/*
		 * args[0] -> java install path (for rt.jar and jce.jar)
		 * args[1] -> whether benchmark or not
		 * args[2] -> relevant directory
		 * args[3] -> main class
		 * args[4] -> output directory
		 */
		
		// ignore this
		/*
		 * args[0] -> class path 
		 * args[1] -> directory to be processed/analysed
		 * args[2] -> main class
		 * args[3] -> sootoutput directory
		 */
		
//		String classpath = ".:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar";

		/*
		String[] sootArgs = {
//				"-whole-program",
				"-coffi",
				"-app",
				"-allow-phantom-refs",
				"-keep-bytecode-offset",
				"-keep-offset",
				"-soot-classpath", args[0], "-prepend-classpath",
				"-keep-line-number",
				"-process-dir", args[1],
				"-main-class", args[2],
				"-output-dir", args[3],
				"-output-format", "jimple"
			};
		*/
		
		if(args[3].contains("Harness")) {
			// the benchmark is dacapo
			String dir = new String(args[2]+"/out");
			String refl_log = new String("reflection-log:"+dir+"/refl.log");
			String cp = new String(args[0]+"/jre/lib/rt.jar:"+args[0]+"/jre/lib/jce.jar:"+dir+":"+args[2]+"/dacapo-9.12-MR1-bach.jar");
			String[] sootArgs = {
					// "-whole-program",
					// "-coffi",
					"-app",
					"-allow-phantom-refs",
					"-keep-bytecode-offset",
					"-keep-offset",
					"-soot-classpath", cp, "-prepend-classpath",
					"-keep-line-number",
					"-main-class", args[3],
					"-process-dir", dir,
					"-p", "cg", refl_log,
					"-output-dir", args[4],
					"-output-format", "jimple",
					"-include", "org.apache.",
					"-include", "org.w3c."
			};
			return sootArgs;			
		}
		return null;
	}
}
