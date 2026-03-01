package wood.mike.sbetcd.model;

public record DeleteResponse(String message) {
    public static DeleteResponse success() {
        return new DeleteResponse("success");
    }
}
