package es;

// Singleton Class
public class Escape extends EscapeState {
	private static final Escape instance = new Escape();

	private Escape() {
	}

	public static Escape getInstance() {
		return instance;
	}

	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Escape;
	}

	@Override
	public String toString() {
		return "Escape";
	}
}
