package wood.mike.sbetcd.model;

public record KlDeleteResponse(String message) {
    public static KlDeleteResponse success() {
        return new KlDeleteResponse("success");
    }
}
