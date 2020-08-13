package ptg;

public class FlowSet {
	public PointsToGraph in, out;
	public FlowSet() {
		in = new PointsToGraph();
		out = new PointsToGraph();
	}
	public FlowSet(PointsToGraph in, PointsToGraph out){
		this.in = in;
		this.out = out;
	}
	@Override
	public String toString() {
		return "in: " + in.toString() + "\nout:" + out.toString();
	}
	public PointsToGraph getIn() {
		return in;
	}
	public void setIn(PointsToGraph in) {
		this.in = in;
	}
	public PointsToGraph getOut() {
		return out;
	}
	public void setOut(PointsToGraph out) {
		this.out = out;
	}
	
	public boolean isEmpty() {
		return this.in.vars.isEmpty() && this.in.fields.isEmpty() && this.out.vars.isEmpty() && this.out.fields.isEmpty();
	}
}
