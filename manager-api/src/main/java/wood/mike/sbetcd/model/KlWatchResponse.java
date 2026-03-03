package wood.mike.sbetcd.model;

public record KlWatchResponse(String message, boolean keyExists) {
    public static KlWatchResponse success(boolean keyExists) {return new KlWatchResponse("success",  keyExists);}
}
