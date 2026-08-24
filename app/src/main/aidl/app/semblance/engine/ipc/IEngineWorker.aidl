package app.semblance.engine.ipc;

import app.semblance.engine.ipc.IEngineCallback;

interface IEngineWorker {
    void openProfile(in Bundle profileData);
    void closeProfile(boolean saveState);
    void loadUrl(String url);
    void requestThumbnail();
    void executeAction(String actionJson);
    void maximize();
    void minimize();
    void simulateAppSwitch(long durationMs);
    void registerCallback(IEngineCallback cb);
    void unregisterCallback(IEngineCallback cb);
}
