package ptg;

import es.EscapeStatus;
import soot.Unit;

import java.util.Map;

public class Analysis {
	private Map<Unit, FlowSet> flowSets;
	private Map<ObjectNode, EscapeStatus> summary;

	public Analysis(Map<Unit, FlowSet> flowSets, Map<ObjectNode, EscapeStatus> summary) {
		super();
		this.flowSets = flowSets;
		this.summary = summary;
	}

	public Map<Unit, FlowSet> getFlowSets() {
		return flowSets;
	}

	public void setFlowSets(Map<Unit, FlowSet> flowSets) {
		this.flowSets = flowSets;
	}

	public Map<ObjectNode, EscapeStatus> getSummary() {
		return summary;
	}

	public void setSummary(Map<ObjectNode, EscapeStatus> summary) {
		this.summary = summary;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Points-to Graph: FlowSets\n");
		for (Map.Entry<Unit, FlowSet> entry : flowSets.entrySet()) {
			str.append("---\nUnit: " + entry.getKey() + ":\n" + entry.getValue() + "\n---\n");
		}
		str.append("Summary:\n");
		for (Map.Entry<ObjectNode, EscapeStatus> entry : summary.entrySet()) {
			str.append(entry.getKey() + ": " + entry.getValue() + "\n");
		}
		return str.toString();
	}
}


