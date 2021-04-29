package utils;
public class IllegalBCIException extends RuntimeException {
    public IllegalBCIException(String errorMessage) {
        super(errorMessage);
    }
}