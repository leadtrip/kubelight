package wood.mike.sbetcd.model;

public record KlPutResponse(String message) {
    public static KlPutResponse success() {return new KlPutResponse("success");}
}
