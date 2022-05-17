package utils;

import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.StandardObject;
import soot.SootMethod;
import recapturer.InvokeSite;

import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.TreeMap;

public class GetListOfRecaptureObjects {
	public static String get(HashMap<Integer, HashMap<SootMethod, HashSet<Integer>>> recaptureSummary) {
        StringBuilder _ret = new StringBuilder();
        TreeMap<Integer, HashMap<SootMethod, ArrayList<Integer>>> summaryMap = new TreeMap<>();

        for(Map.Entry<Integer, HashMap<SootMethod, HashSet<Integer>>> entry: recaptureSummary.entrySet()){
            if(entry.getValue().isEmpty())
                continue;
            HashMap<SootMethod, ArrayList<Integer>> summary = new HashMap<>();
            for(Map.Entry<SootMethod, HashSet<Integer>> e : entry.getValue().entrySet()) {
                if(e.getValue().isEmpty())
                    continue;
                summary.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
            summaryMap.put(entry.getKey(), summary);
        }
        _ret.append('[');

        for(Map.Entry<Integer, HashMap<SootMethod, ArrayList<Integer>>> entry : summaryMap.entrySet()) {
            if(entry.getValue().isEmpty())
                continue;
            _ret.append(entry.getKey().toString());
            _ret.append(" [");
            for(Map.Entry<SootMethod, ArrayList<Integer>> e : entry.getValue().entrySet()) {
                if(e.getValue().isEmpty())
                    continue;
                _ret.append(transformFuncSignature(e.getKey().getBytecodeSignature()));
                Collections.sort(e.getValue());
                _ret.append(" ");
                _ret.append(e.getValue().toString());
                _ret.append(", ");
            }
            if(_ret.length()>1)
                _ret.delete(_ret.length()-2, _ret.length());
            _ret.append("], ");
        }

        // for(Map.Entry<InvokeSite, ArrayList<Integer>> entry: summaryMap.entrySet()){
        //     Collections.sort(entry.getValue());
        //     _ret.append(transformFuncSignature(entry.getKey().getMethod().getBytecodeSignature()));
        //     _ret.append(" <");
        //     _ret.append(entry.getKey().getSite());
        //     _ret.append("> ");
        //     _ret.append(entry.getValue().toString());
        //     _ret.append(", ");
        // }
        if(_ret.length()>1)
            _ret.delete(_ret.length()-2, _ret.length());
        _ret.append(']');

		return _ret.toString();
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
}
