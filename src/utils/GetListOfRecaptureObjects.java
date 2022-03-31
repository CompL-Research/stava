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
import java.util.HashSet;
import java.util.Map;

public class GetListOfRecaptureObjects {
	public static String get(HashMap<InvokeSite, HashSet<StandardObject>> recaptureSummary) {
        StringBuilder _ret = new StringBuilder();
        HashMap<InvokeSite, ArrayList<Integer>> summaryMap = new HashMap<>();

        for(Map.Entry<InvokeSite, HashSet<StandardObject>> entry: recaptureSummary.entrySet()){
            // if(stObj.getObject().type != ObjectType.internal)
			// 	continue;
            // SootMethod methodInfo = stObj.getMethod();
            // if(!summaryMap.containsKey(methodInfo))
            //     summaryMap.put(methodInfo, new ArrayList<>());
            // summaryMap.get(methodInfo).add(stObj.getObject().ref);
            summaryMap.put(entry.getKey(), new ArrayList<>());
            for(StandardObject o : entry.getValue()) {
                if(o.getObject().type != ObjectType.internal)
				    continue;
                summaryMap.get(entry.getKey()).add(o.getObject().ref);
            }
        }
        _ret.append('[');
        for(Map.Entry<InvokeSite, ArrayList<Integer>> entry: summaryMap.entrySet()){
            Collections.sort(entry.getValue());
            _ret.append(transformFuncSignature(entry.getKey().getMethod().getBytecodeSignature()));
            _ret.append(" <");
            _ret.append(entry.getKey().getSite());
            _ret.append("> ");
            _ret.append(entry.getValue().toString());
            _ret.append(", ");
        }
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
