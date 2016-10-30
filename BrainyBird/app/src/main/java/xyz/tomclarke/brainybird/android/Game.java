/**
 * The Game
 *
 * @author Lars Harmsen
 * Copyright (c) <2014> <Lars Harmsen - Quchen>
 */

package xyz.tomclarke.brainybird.android;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class Game extends FragmentActivity {

    /**
     * Name of the SharedPreference that saves the medals
     */
    public static final String coin_save = "coin_save";

    /**
     * Key that saves the medal
     */
    public static final String coin_key = "coin_key";

    /**
     * Will play things like mooing
     */
    public static SoundPool soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);

    /**
     * Will play songs like:
     * nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan
     * nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan
     * nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan
     * nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan nyan
     * Does someone know the second verse ???
     */
    public static MediaPlayer musicPlayer = null;

    /**
     * Whether the music should play or not
     */
    public boolean musicShouldPlay = false;

    /**
     * Time interval (ms) you have to press the backbutton twice in to exit
     */
    private static final long DOUBLE_BACK_TIME = 1000;

    /**
     * Saves the time of the last backbutton press
     */
    private long backPressed;

    /**
     * To do UI things from different threads
     */
    public MyHandler handler;

    /**
     * Hold all accomplishments
     */
    AccomplishmentBox accomplishmentBox;

    /**
     * The view that handles all kind of stuff
     */
    GameView view;

    /**
     * The amount of collected coins
     */
    int coins;

    /**
     * This will increase the revive price
     */
    public int numberOfRevive = 1;

    /**
     * The dialog displayed when the game is over
     */
    GameOverDialog gameOverDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accomplishmentBox = new AccomplishmentBox();
        Calibration calibration = (Calibration) getIntent().getSerializableExtra("calibration");
        view = new GameView(this, calibration);
        gameOverDialog = new GameOverDialog(this);
        handler = new MyHandler(this);
        setContentView(view);
        initMusicPlayer();
        loadCoins();
    }


    /**
     * Initializes the player with the nyan cat song
     * and sets the position to 0.
     */
    public void initMusicPlayer() {
        if (musicPlayer == null) {
            // to avoid unnecessary reinitialisation
            musicPlayer = MediaPlayer.create(this, R.raw.nyan_cat_theme);
            musicPlayer.setLooping(true);
            musicPlayer.setVolume(MainActivity.volume, MainActivity.volume);
        }
        musicPlayer.seekTo(0);    // Reset song to position 0
    }

    private void loadCoins() {
        SharedPreferences saves = this.getSharedPreferences(coin_save, 0);
        this.coins = saves.getInt(coin_key, 0);
    }

    /**
     * Pauses the view and the music
     */
    @Override
    protected void onPause() {
        view.pause();
        if (musicPlayer.isPlaying()) {
            musicPlayer.pause();
        }
        super.onPause();
    }

    /**
     * Resumes the view (but waits the view waits for a tap)
     * and starts the music if it should be running.
     * Also checks whether the Google Play Services are available.
     */
    @Override
    protected void onResume() {
        view.drawOnce();
        if (musicShouldPlay) {
            musicPlayer.start();
        }
        super.onResume();
    }

    /**
     * Prevent accidental exits by requiring a double press.
     */
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - backPressed < DOUBLE_BACK_TIME) {
            super.onBackPressed();
        } else {
            backPressed = System.currentTimeMillis();
            Toast.makeText(this, getResources().getString(R.string.on_back_press), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sends the handler the command to show the GameOverDialog.
     * Because it needs an UI thread.
     */
    public void gameOver() {
        handler.sendMessage(Message.obtain(handler, MyHandler.GAME_OVER_DIALOG));
    }

    /**
     * Increases the coin count of the player by 1.
     */
    public void increaseCoin() {
        this.coins++;
    }

    /**
     * What should happen, when an obstacle is passed?
     */
    public void increasePoints() {
        accomplishmentBox.points++;

        this.view.getPlayer().upgradeBitmap(accomplishmentBox.points);
    }

    /**
     * Shows the GameOverDialog when a message with code 0 is received.
     */
    static class MyHandler extends Handler {
        public static final int GAME_OVER_DIALOG = 0;
        public static final int SHOW_TOAST = 1;
        public static final int SHOW_AD = 2;

        private Game game;

        public MyHandler(Game game) {
            this.game = game;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GAME_OVER_DIALOG:
                    showGameOverDialog();
                    break;
                case SHOW_TOAST:
                    Toast.makeText(game, msg.arg1, Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        private void showGameOverDialog() {
            game.gameOverDialog.init();
            game.gameOverDialog.show();
        }
    }

}
