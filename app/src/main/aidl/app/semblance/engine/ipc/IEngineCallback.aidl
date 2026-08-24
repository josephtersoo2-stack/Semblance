package app.semblance.engine.ipc;

interface IEngineCallback {
    void onStateChanged(int profileId, String status, String currentUrl);
    void onDomainVisited(int profileId, String host);
    void onError(int profileId, String message);
    void onThumbnailReady(int profileId, in byte[] jpegData);
}
