package wood.mike.sbetcd.model;

public record WatchResponse(String message, boolean keyExists) {
    public static WatchResponse success(boolean keyExists) {return new WatchResponse("success",  keyExists);}
}
