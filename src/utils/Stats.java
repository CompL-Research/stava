package utils;

import java.util.HashMap;
import java.util.Map;

import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.ObjectType;
import soot.SootMethod;

public class Stats {
	int internal;
	int noEscape;
	int cv;
	int total;
	double percentageNE;
	double percentageCV;
	
	public Stats(HashMap<SootMethod,HashMap<ObjectNode,EscapeStatus>> summaries) {
		internal=0; noEscape=0; cv=0; percentageNE=0; percentageCV=0;
		for(Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> e : summaries.entrySet()) {
			for(Map.Entry<ObjectNode, EscapeStatus> ee : e.getValue().entrySet()) {
				if(ee.getKey().type==ObjectType.internal) {
					internal++;
					if(ee.getValue().containsNoEscape()) noEscape++;
					else if(ee.getValue().containsCV()) cv++;
				}
			}
		}
		total = internal;
		percentageNE = (noEscape*100.0/total);
		percentageCV = (cv*100.0/total);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Total Objects: "+total+" ");
		sb.append("NoEscape: "+noEscape+" ");
		sb.append(String.format("(%.2f%%) ", percentageNE));
		sb.append("CV: "+cv+" ");
		sb.append(String.format("(%.2f%%) ", percentageCV));
		return sb.toString();
	}
}
