package com.aghakhani.flappybirdgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.os.Handler;
import android.media.MediaPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {
    private MediaPlayer gameOverSound;
    private MediaPlayer gameMusic;
    private Thread gameThread;
    private boolean isPlaying;
    private SurfaceHolder holder;
    private Paint paint;
    private Handler colorChangeHandler = new Handler();
    private boolean isYellow = false;

    private int screenX, screenY;
    private int birdX, birdY;
    private int birdSize = 50;
    private int birdVelocity = 0;
    private int gravity = 2;

    private ArrayList<Rect> obstacles;
    private int obstacleWidth = 200;
    private int gapHeight = 400;
    private int obstacleSpeed = 10;
    private int obstacleSpawnTime = 200;

    private Random random;
    private int score = 0;
    private boolean gameOver = false;
    private int frameCount = 0;

    public GameView(Context context) {
        super(context);
        gameOverSound = MediaPlayer.create(this.getContext(), R.raw.lose_sound);
        gameMusic = MediaPlayer.create(this.getContext(), R.raw.game_music);
        holder = getHolder();
        paint = new Paint();
        random = new Random();

        screenX = getResources().getDisplayMetrics().widthPixels;
        screenY = getResources().getDisplayMetrics().heightPixels;

        birdX = screenX / 4;
        birdY = screenY / 2;

        obstacles = new ArrayList<>();
    }

    @Override
    public void run() {
        while (isPlaying) {
            update();
            draw();
            sleep();
            gameMusic.start();
        }
    }

    private void update() {
        if (gameOver) return;

        birdVelocity += gravity;
        birdY += birdVelocity;

        if (birdY < 0) birdY = 0;
        if (birdY + birdSize > screenY) birdY = screenY - birdSize;

        Iterator<Rect> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Rect obstacle = iterator.next();
            obstacle.left -= obstacleSpeed;
            obstacle.right -= obstacleSpeed;

            if (obstacle.right < 0) {
                iterator.remove();
                score++;
            }
        }

        if (frameCount % obstacleSpawnTime == 0) {
            int minHeight = screenY / 6;
            int maxHeight = screenY - gapHeight - minHeight;
            int topHeight = random.nextInt(maxHeight - minHeight) + minHeight;
            int bottomY = topHeight + gapHeight;

            topHeight = Math.max(topHeight, minHeight);
            bottomY = Math.min(bottomY, screenY - minHeight);

            obstacles.add(new Rect(screenX, 0, screenX + obstacleWidth, topHeight));
            obstacles.add(new Rect(screenX, bottomY, screenX + obstacleWidth, screenY));
        }

        frameCount++;

        Rect birdRect = new Rect(birdX, birdY, birdX + birdSize, birdY + birdSize);
        for (Rect obstacle : obstacles) {
            if (Rect.intersects(birdRect, obstacle)) {
                stopGame();
            }
        }
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(Color.WHITE);

            paint.setColor(Color.RED);
            canvas.drawRect(birdX, birdY, birdX + birdSize, birdY + birdSize, paint);

            paint.setColor(isYellow ? Color.YELLOW : Color.GREEN);
            for (Rect obstacle : obstacles) {
                canvas.drawRect(obstacle, paint);
            }

            paint.setColor(Color.BLACK);
            paint.setTextSize(50);
            canvas.drawText("Score: " + score, 50, 100, paint);

            if (gameOver) {
                paint.setTextSize(100);
                canvas.drawText("Game Over", screenX / 4, screenY / 2, paint);
            }

            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(16);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
        startColorChangeLoop();
    }

    public void pause() {
        try {
            isPlaying = false;
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopGame() {
        isPlaying = false;
        gameOver = true;
        gameOverSound.start();
        gameMusic.stop();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) {
            restartGame();
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            birdVelocity = -20;
        }
        return true;
    }

    private void restartGame() {
        score = 0;
        birdY = screenY / 2;
        birdVelocity = 0;
        obstacles.clear();
        gameOver = false;
        resume();
    }

    private void startColorChangeLoop() {
        colorChangeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isYellow = !isYellow;
                colorChangeHandler.postDelayed(this, 5000);
            }
        }, 5000);
    }
}
