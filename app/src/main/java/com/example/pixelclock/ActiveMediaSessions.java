package com.example.pixelclock;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

final class ActiveMediaSessions {
    interface Listener {
        void onMediaChanged(MediaController controller, PlaybackState state);
    }

    private final MediaSessionManager manager;
    private final ComponentName accessComponent;
    private final Handler handler;
    private final Listener listener;
    private final List<MediaController> controllers = new ArrayList<>();
    private boolean listening;

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChanged =
            this::replaceControllers;

    private final MediaController.Callback controllerChanged = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            publishCurrentSession();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            publishCurrentSession();
        }

        @Override
        public void onSessionDestroyed() {
            refresh();
        }
    };

    ActiveMediaSessions(Context context, Handler handler, Listener listener) {
        manager = context.getSystemService(MediaSessionManager.class);
        accessComponent = new ComponentName(context, MediaAccessService.class);
        this.handler = handler;
        this.listener = listener;
    }

    void start() {
        if (listening) {
            return;
        }
        try {
            manager.addOnActiveSessionsChangedListener(sessionsChanged, accessComponent, handler);
            listening = true;
            refresh();
        } catch (SecurityException accessNotGranted) {
            listener.onMediaChanged(null, null);
        }
    }

    void stop() {
        if (listening) {
            listening = false;
            manager.removeOnActiveSessionsChangedListener(sessionsChanged);
        }
        clearControllers();
    }

    private void refresh() {
        if (!listening) {
            return;
        }
        try {
            replaceControllers(manager.getActiveSessions(accessComponent));
        } catch (SecurityException accessRevoked) {
            stop();
            listener.onMediaChanged(null, null);
        }
    }

    private void replaceControllers(List<MediaController> sessions) {
        if (!listening) {
            return;
        }
        clearControllers();
        if (sessions != null) {
            for (MediaController controller : sessions) {
                controller.registerCallback(controllerChanged, handler);
                controllers.add(controller);
            }
        }
        publishCurrentSession();
    }

    private void clearControllers() {
        for (MediaController controller : controllers) {
            controller.unregisterCallback(controllerChanged);
        }
        controllers.clear();
    }

    private void publishCurrentSession() {
        MediaController pausedController = null;
        PlaybackState pausedState = null;
        for (MediaController controller : controllers) {
            PlaybackState state = controller.getPlaybackState();
            if (state == null) {
                continue;
            }
            switch (state.getState()) {
                case PlaybackState.STATE_PLAYING:
                case PlaybackState.STATE_BUFFERING:
                case PlaybackState.STATE_FAST_FORWARDING:
                case PlaybackState.STATE_REWINDING:
                case PlaybackState.STATE_SKIPPING_TO_NEXT:
                case PlaybackState.STATE_SKIPPING_TO_PREVIOUS:
                case PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM:
                    listener.onMediaChanged(controller, state);
                    return;
                case PlaybackState.STATE_PAUSED:
                    if (pausedController == null) {
                        pausedController = controller;
                        pausedState = state;
                    }
                    break;
                default:
                    break;
            }
        }
        listener.onMediaChanged(pausedController, pausedState);
    }
}
