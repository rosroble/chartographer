package com.example.chartographer.image;

public class ImagePiece {
    public static final int PIECE_MIN_WIDTH = 1;
    public static final int PIECE_MIN_HEIGHT = 1;
    public static final int PIECE_MAX_WIDTH = 5000;
    public static final int PIECE_MAX_HEIGHT = 5000;
    private int width;
    private int height;
    private int x;
    private int y;

    public ImagePiece(int width, int height, int x, int y) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
