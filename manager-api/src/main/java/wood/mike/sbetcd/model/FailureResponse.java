package wood.mike.sbetcd.model;

public record FailureResponse(String message) {
    public static FailureResponse failure(String message) {
        return new FailureResponse(message);
    }
}
