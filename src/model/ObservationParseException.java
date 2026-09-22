package model;

//собственное исключение с информацией о строке
public class ObservationParseException extends Exception {

    private final int lineNumber;
    private final String rawLine;

    public ObservationParseException(int lineNumber, String rawLine, String message, Throwable cause) {
        super("Line %d (%s): %s".formatted(lineNumber, rawLine, message), cause);
        this.lineNumber = lineNumber;
        this.rawLine = rawLine;
    }

    public int lineNumber() { return lineNumber; }
    public String rawLine()  { return rawLine; }
}