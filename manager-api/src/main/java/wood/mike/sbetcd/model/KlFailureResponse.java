package wood.mike.sbetcd.model;

public record KlFailureResponse(String message) {
    public static KlFailureResponse failure(String message) {
        return new KlFailureResponse(message);
    }
}
