package com.example.pixelclock;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Random;

public final class ClockDreamService extends DreamService {
    private static final long MOVE_INTERVAL_MS = 60_000L;
    private static final int CLOCK_COLOR = Color.rgb(145, 178, 171);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private FrameLayout background;
    private TextClock clock;
    private LinearLayout mediaPanel;
    private ImageView artwork;
    private TextView title;
    private TextView artist;
    private Button previous;
    private Button playPause;
    private Button next;
    private ActiveMediaSessions sessions;
    private MediaController currentController;

    private final Runnable moveClock = new Runnable() {
        @Override
        public void run() {
            if (mediaPanel.getVisibility() == View.GONE) {
                // Move within the space left around the centered clock.
                float horizontalRoom = Math.max(0, background.getWidth() - clock.getWidth()) / 2f;
                float verticalRoom = Math.max(0, background.getHeight() - clock.getHeight()) / 2f;
                clock.setTranslationX(randomOffset(horizontalRoom));
                clock.setTranslationY(randomOffset(verticalRoom));
            }
            handler.postDelayed(this, MOVE_INTERVAL_MS);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setFullscreen(true);
        setInteractive(true);
        setScreenBright(false);

        background = new FrameLayout(this);
        background.setBackgroundColor(Color.BLACK);
        background.setOnClickListener(view -> finish());
        background.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams centered = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        background.addView(content, centered);

        clock = new TextClock(this);
        clock.setTextColor(CLOCK_COLOR);
        clock.setTextSize(48);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setGravity(Gravity.CENTER);

        content.addView(clock);
        content.addView(createMediaPanel(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        sessions = new ActiveMediaSessions(this, handler, this::showMedia);

        // Reset the offset when rotation or a change in clock width triggers layout.
        clock.addOnLayoutChangeListener((view, left, top, right, bottom,
                                         oldLeft, oldTop, oldRight, oldBottom) -> {
            clock.setTranslationX(0);
            clock.setTranslationY(0);
        });
        setContentView(background);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        sessions.start();
        handler.postDelayed(moveClock, MOVE_INTERVAL_MS);
    }

    @Override
    public void onDreamingStopped() {
        stopUpdates();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        stopUpdates();
        super.onDetachedFromWindow();
    }

    private void stopUpdates() {
        handler.removeCallbacks(moveClock);
        if (sessions != null) {
            sessions.stop();
            currentController = null;
        }
    }

    private LinearLayout createMediaPanel() {
        mediaPanel = new LinearLayout(this);
        mediaPanel.setOrientation(LinearLayout.VERTICAL);
        mediaPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        mediaPanel.setPadding(dp(16), dp(12), dp(16), 0);
        mediaPanel.setVisibility(View.GONE);

        LinearLayout track = new LinearLayout(this);
        track.setGravity(Gravity.CENTER_VERTICAL);
        mediaPanel.addView(track);

        artwork = new ImageView(this);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artwork.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams artSize = new LinearLayout.LayoutParams(dp(56), dp(56));
        artSize.setMarginEnd(dp(12));
        track.addView(artwork, artSize);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        track.addView(labels);
        title = trackLabel(18, Color.WHITE);
        artist = trackLabel(14, CLOCK_COLOR);
        labels.addView(title);
        labels.addView(artist);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        mediaPanel.addView(controls);
        previous = addControl(controls, R.string.previous_track);
        playPause = addControl(controls, R.string.play);
        next = addControl(controls, R.string.next_track);

        previous.setOnClickListener(view -> {
            if (currentController != null) {
                currentController.getTransportControls().skipToPrevious();
            }
        });
        playPause.setOnClickListener(view -> togglePlayback());
        next.setOnClickListener(view -> {
            if (currentController != null) {
                currentController.getTransportControls().skipToNext();
            }
        });
        return mediaPanel;
    }

    private TextView trackLabel(int textSize, int color) {
        TextView label = new TextView(this);
        label.setTextSize(textSize);
        label.setTextColor(color);
        label.setMaxLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);
        return label;
    }

    private Button addControl(LinearLayout row, int text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        row.addView(button);
        return button;
    }

    private void showMedia(MediaController controller, PlaybackState state) {
        currentController = controller;
        if (controller == null) {
            mediaPanel.setVisibility(View.GONE);
            artwork.setImageDrawable(null);
            return;
        }

        MediaMetadata metadata = controller.getMetadata();
        MediaDescription description = metadata == null ? null : metadata.getDescription();
        CharSequence trackTitle = description == null ? null : description.getTitle();
        CharSequence trackArtist = metadata == null ? null
                : metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
        if (TextUtils.isEmpty(trackArtist) && description != null) {
            trackArtist = description.getSubtitle();
        }
        if (TextUtils.isEmpty(trackTitle)) {
            title.setText(R.string.now_playing);
        } else {
            title.setText(trackTitle);
        }
        artist.setText(trackArtist);
        artist.setVisibility(TextUtils.isEmpty(trackArtist) ? View.GONE : View.VISIBLE);
        Bitmap albumArt = description == null ? null : description.getIconBitmap();
        artwork.setImageBitmap(albumArt);
        artwork.setVisibility(albumArt == null ? View.GONE : View.VISIBLE);

        long actions = state.getActions();
        boolean paused = state.getState() == PlaybackState.STATE_PAUSED;
        playPause.setText(paused ? R.string.play : R.string.pause);
        setControlAvailable(previous, actions, PlaybackState.ACTION_SKIP_TO_PREVIOUS);
        setControlAvailable(playPause, actions, PlaybackState.ACTION_PLAY_PAUSE
                | (paused ? PlaybackState.ACTION_PLAY : PlaybackState.ACTION_PAUSE));
        setControlAvailable(next, actions, PlaybackState.ACTION_SKIP_TO_NEXT);
        clock.setTranslationX(0);
        clock.setTranslationY(0);
        mediaPanel.setVisibility(View.VISIBLE);
    }

    private void setControlAvailable(Button button, long actions, long supportedActions) {
        button.setVisibility((actions & supportedActions) == 0 ? View.GONE : View.VISIBLE);
    }

    private void togglePlayback() {
        if (currentController == null) {
            return;
        }
        PlaybackState state = currentController.getPlaybackState();
        if (state == null) {
            return;
        }
        long actions = state.getActions();
        MediaController.TransportControls transport = currentController.getTransportControls();
        if (state.getState() == PlaybackState.STATE_PAUSED
                && (actions & PlaybackState.ACTION_PLAY) != 0) {
            transport.play();
        } else if (state.getState() != PlaybackState.STATE_PAUSED
                && (actions & PlaybackState.ACTION_PAUSE) != 0) {
            transport.pause();
        } else if ((actions & PlaybackState.ACTION_PLAY_PAUSE) != 0) {
            currentController.dispatchMediaButtonEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            currentController.dispatchMediaButtonEvent(
                    new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float randomOffset(float room) {
        // Leave a margin at each edge even at the most extreme offset.
        return (random.nextFloat() * 2f - 1f) * room * 0.8f;
    }
}
