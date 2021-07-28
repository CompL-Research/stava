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
		if(args[3].equals("JDK")) {
			return jdk(args);
		}
		if(args[3].equals("Refl")) {
			return normal_refl(args);
		}
		if (args[1].contains("true") || args[1].contains("True")) {
			// this is a benchmark
			if (args[3].contains("Harness")) {
				// the benchmark is dacapo
				return dacapo(args);
			} 
			else if (args[3].contains("JBB") ) {
				return jbb(args);
			}
			else if (args[3].contains("spec.harness.Launch") ) {
				return jvm(args);
			} 
			else {
				return normal(args);
			}
		} else {
			// this is a standard application
			return normal(args);
		}
	}
	private String[] dacapo(String[] args){
		String dir = args[2] + "/out";
		String refl_log = "reflection-log:" + dir + "/refl.log";
		// String cp = args[0] + "/jre/lib/rt.jar:" + args[0] + "/jre/lib/jce.jar:" + dir + ":" + args[2] + "/dacapo-9.12-MR1-bach.jar";
		String cp = dir + ":" + args[2] + "/dacapo-9.12-MR1-bach.jar";
		String[] sootArgs = {
				"-whole-program",
				"-app",
				"-allow-phantom-refs",
				"-keep-bytecode-offset",
				"-no-bodies-for-excluded",
				"-keep-offset",
				"-soot-classpath", cp, 
				"-prepend-classpath",
				"-keep-line-number",
				"-main-class", args[3],
				"-process-dir", dir,
				"-p","cg.spark","on",
				"-p", "cg", refl_log,
				"-output-dir", args[4],
				"-output-format", "jimple",
				"-ire",
				// "-x", "jdk.*",
				// "-x", "org.eclipse.jdt.internal.*",
				// "-include", "org.apache.batik.*",
				// "-x", "org.apache.crimson.*",
				// "-include", "org.w3c.*",
				// "-include", "org.apache.*",
				"-i", "jdt.*",
				"-i", "jdk.*",
				"-i", "java.*",
				"-i", "org.*",
				"-i", "com.*",
				"-i", "sun.*",
				// "-i", "javax.*",
		};
		for(String s: sootArgs) {
			System.out.print(s+" ");
		}
		System.out.println("");
		return sootArgs;
	}

	private String[] jbb(String[] args){
		String dir = args[2] ;
		// String refl_log = "reflection-log:" + dir + "/refl.log";
		// String cp = args[0] + "/jre/lib/rt.jar:" + args[0] + "/jre/lib/jce.jar:" + dir + ":" + args[2] + "/dacapo-9.12-MR1-bach.jar";
		String cp = dir + ":" + args[2] + "/jbb.jar"+":"+args[2]+"/check.jar";//+":"+args[0] + "/jre/lib/rt.jar:" + args[0] + "/jre/lib/jce.jar";
		String[] sootArgs = {
				"-whole-program",
				"-app",
				"-allow-phantom-refs",
				"-keep-bytecode-offset",
				"-no-bodies-for-excluded",
				"-keep-offset",
				"-cp", cp,
				"-prepend-classpath",
				"-keep-line-number",
				"-main-class", args[3],
				"-process-dir", dir+"/jbb.jar",
				"-p","cg.spark","on",
				"-output-dir", args[4],
				"-output-format", "jimple",
				// "-x", "jdk.*",
				// "-x", "org.eclipse.jdt.internal.*",
				// "-include", "org.apache.batik.*",
				// "-x", "org.apache.crimson.*",
				// "-include", "org.w3c.*",
				// "-include", "org.apache.*",
				// "-i", "spec.*",
				"-i", "jdk.*",
				"-i", "java.*",
				"-i", "org.*",
				"-i", "com.*",
				"-i", "sun.*",
				// "-i", "javax.*",
		};
		for(String s: sootArgs) {
			System.out.print(s+" ");
		}
		System.out.println("");
		return sootArgs;
	}

	private String[] jvm(String[] args) {
		String dir = args[2] ;
		// String refl_log = "reflection-log:" + dir + "/refl.log";
		// String cp = args[0] + "/jre/lib/rt.jar:" + args[0] + "/jre/lib/jce.jar:" + dir + ":" + args[2] + "/dacapo-9.12-MR1-bach.jar";
		String cp = args[0] + "/jre/lib/rt.jar:" + args[0] + "/jre/lib/jce.jar:" + dir + ":" + dir + "/SPECjvm2008.jar";
		String[] sootArgs = {
				"-whole-program",
				"-app",
				"-allow-phantom-refs",
				"-keep-bytecode-offset",
				"-no-bodies-for-excluded",
				"-keep-offset",
				"-cp", cp,
				"-prepend-classpath",
				"-keep-line-number",
				"-main-class", args[3],
				"-process-dir", dir+"/SPECjvm2008.jar",
				"-p","cg.spark","on",
				"-output-dir", args[4],
				"-output-format", "jimple",
				"-ire",
				// "-x", "jdk.*",
				// "-x", "org.eclipse.jdt.internal.*",
				// "-include", "org.apache.batik.*",
				// "-x", "org.apache.crimson.*",
				// "-include", "org.w3c.*",
				// "-include", "org.apache.*",
				"-i", "spec.*",
				"-i", "jdk.*",
				"-i", "java.*",
				"-i", "org.*",
				"-i", "com.*",
				"-i", "sun.*",
				// "-i", "javax.*",
		};
		for(String s: sootArgs) {
			System.out.print(s+" ");
		}
		System.out.println("");
		return sootArgs;
	}

	private String[] dacapoJava11(String[] args){
		String dir = args[2] + "/out";
		String refl_log = "reflection-log:" + dir + "/refl.log";
		String cp = args[0] + "/jre/lib/rt.jar:" + args[0] + "/jre/lib/jce.jar:" + dir + ":" + args[2] + "/dacapo-9.12-MR1-bach.jar";
		// String cp = dir + ":" + args[2] + "/dacapo-9.12-MR1-bach.jar";
		String modulePath = dir + ":" + args[2] + "/dacapo-9.12-MR1-bach.jar";
		String[] sootArgs = {
				"-whole-program",
				"-app",
				"-allow-phantom-refs",
				"-keep-bytecode-offset",
				"-no-bodies-for-excluded",
				"-keep-offset",
				"-soot-classpath", cp, 
				"-prepend-classpath",
				"-keep-line-number",
				"-main-class", args[3],
				"-process-dir", dir,
				"-p","cg.spark","on",
				"-p", "cg", refl_log,
				"-output-dir", args[4],
				"-output-format", "jimple",
				// "-x", "jdk.*",
				// "-x", "org.eclipse.jdt.internal.*",
				// "-include", "org.apache.batik.*",
				// "-x", "org.apache.crimson.*",
				// "-include", "org.w3c.*",
				// "-include", "org.apache.*",
				"-x", "jdk.*",
				// "-i", "java.*",
				// "-i", "org.*",
				// "-i", "com.*",
				// "-i", "javax.*",
		};
		for(String s: sootArgs) {
			System.out.print(s+" ");
		}
		System.out.println("");
		return sootArgs;
	}

	private String[] normal(String[] args){
		String cp = args[0] + "/jre/lib/rt.jar:" + args[0] + "/jre/lib/jce.jar";
		String[] sootArgs = {
				"-whole-program",
				"-app",
				"-allow-phantom-refs",
				"-keep-bytecode-offset",
				"-p","cg.spark","on",
				"-p","cg","all-reachable",
				"-keep-offset",
				// "-soot-classpath", cp, //"-prepend-classpath",
				"-keep-line-number",
				"-main-class", args[3],
				"-process-dir", args[2],
				"-output-dir", args[4],
				"-output-format", "jimple",
				"-x", "jdk.*",
				// "-i", "java.*",
				// "-i", "org.*",
				// "-i", "com.*",
				// "-i", "sun.*",
				// "-include", "java.util.HashMap"
		};
		for(String s: sootArgs) {
			System.out.print(s+" ");
		}
		System.out.println("");
		return sootArgs;
	}

	private String[] normal_refl(String[] args){
		String dir = args[2] + "/out";
		String refl_log = "reflection-log:" + dir + "/refl.log";
		String cp = args[0] + "/jre/lib/rt.jar:" + args[0] + "/jre/lib/jce.jar";
		String[] sootArgs = {
				"-whole-program",
				"-app",
				"-allow-phantom-refs",
				"-keep-bytecode-offset",
				"-p","cg.spark","on",
				// "-p","cg","all-reachable",
				"-p","cg", refl_log,
				"-keep-offset",
				"-soot-classpath", cp, 
				"-prepend-classpath",
				"-keep-line-number",
				"-main-class", args[3],
				"-process-dir", dir,
				"-output-dir", args[4],
				"-output-format", "jimple",
				"-i", "jdk.*",
				"-i", "java.*",
				"-i", "org.*",
				"-i", "com.*",
				"-i", "sun.*",
				// "-include", "java.util.HashMap"
		};
		for(String s: sootArgs) {
			System.out.print(s+" ");
		}
		System.out.println("");
		return sootArgs;
	}

	private String[] jdk(String[] args) {
		String cp = args[0] + "/jre/lib/rt.jar:" + args[0] + "/jre/lib/jce.jar";
		String[] sootArgs = {
				"-whole-program",
				"-app",
				"-allow-phantom-refs",
				"-keep-bytecode-offset",
				"-p","cg.spark","on",
				"-p","cg","all-reachable",
				"-keep-offset",
				"-soot-classpath", cp, "-prepend-classpath",
				"-keep-line-number",
				// "-main-class", args[3],
				// "-process-dir", args[2],
				// "-process-path", args[0]+"/jre/lib/rt.jar",
				// "-process-path", args[0]+"/jre/lib/jce.jar",
				"-output-dir", args[4],
				"-output-format", "jimple",
				"-i", "*",
				"-i", "java.*",
				"-x", "jdk.*"
		};
		for(String s: sootArgs) {
			System.out.print(s+" ");
		}
		System.out.println("");
		return sootArgs;
	}
}
