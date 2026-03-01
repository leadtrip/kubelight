package wood.mike.sbetcd.model;

public record PutResponse(String message) {
    public static PutResponse success() {return new PutResponse("success");}
}
