package es;

// Singleton Class
public class NoEscape extends EscapeState {
	private static final NoEscape instance = new NoEscape();

	private NoEscape() {
	}

	public static NoEscape getInstance() {
		return instance;
	}

	public int hashCode() {
		return 1;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NoEscape;
	}

	@Override
	public String toString() {
		return "NoEscape";
	}
}
