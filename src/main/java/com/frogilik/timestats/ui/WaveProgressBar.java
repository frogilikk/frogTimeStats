package com.frogilik.timestats.ui;

import com.frogilik.timestats.model.AppActivity;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class WaveProgressBar extends Pane {

    private final Canvas canvas;
    private final List<SegmentData> segments = new ArrayList<>();
    private double wavePhase = 0.0;
    private AnimationTimer waveTimer;

    public record SegmentData(String name, double percentage, Color color) {}

    public WaveProgressBar() {
        canvas = new Canvas();
        getChildren().add(canvas);

        widthProperty().addListener((obs, oldV, newV) -> draw());
        heightProperty().addListener((obs, oldV, newV) -> draw());

        startAnimation();
    }

    public void setSegments(List<SegmentData> newSegments) {
        this.segments.clear();
        this.segments.addAll(newSegments);
        draw();
    }

    private void startAnimation() {
        waveTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                wavePhase += 0.06;
                draw();
            }
        };
        waveTimer.start();
    }

    private void draw() {
        double w = getWidth();
        double h = getHeight();

        if (w <= 0 || h <= 0) return;

        canvas.setWidth(w);
        canvas.setHeight(h);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // 1. Фон
        gc.setFill(Color.web("#313244"));
        gc.fillRoundRect(0, 0, w, h, 10, 10);

        if (segments.isEmpty()) return;

        // 2. Клиппинг под закругленный прямоугольник
        gc.save();
        applyRoundRectClip(gc, 0, 0, w, h, 10);

        // 3. Отрисовка сегментов слева направо с волной
        double currentX = 0;
        double waveAmplitude = 3.5;
        double waveFrequency = 0.06;

        for (SegmentData seg : segments) {
            double segWidth = w * seg.percentage();
            if (segWidth <= 0) continue;

            double nextX = currentX + segWidth;

            gc.setFill(seg.color());
            gc.beginPath();

            // Левая граница
            for (double y = 0; y <= h; y += 2) {
                double waveX = currentX + Math.sin(y * waveFrequency + wavePhase) * waveAmplitude;
                if (y == 0) gc.moveTo(waveX, y);
                else gc.lineTo(waveX, y);
            }

            // Правая граница
            for (double y = h; y >= 0; y -= 2) {
                double waveX = nextX + Math.sin(y * waveFrequency + wavePhase) * waveAmplitude;
                gc.lineTo(waveX, y);
            }

            gc.closePath();
            gc.fill();

            currentX = nextX;
        }

        gc.restore();
    }

    private void applyRoundRectClip(GraphicsContext gc, double x, double y, double w, double h, double r) {
        gc.beginPath();
        gc.moveTo(x + r, y);
        gc.lineTo(x + w - r, y);
        gc.quadraticCurveTo(x + w, y, x + w, y + r);
        gc.lineTo(x + w, y + h - r);
        gc.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
        gc.lineTo(x + r, y + h);
        gc.quadraticCurveTo(x, y + h, x, y + h - r);
        gc.lineTo(x, y + r);
        gc.quadraticCurveTo(x, y, x + r, y);
        gc.closePath();
        gc.clip();
    }
}