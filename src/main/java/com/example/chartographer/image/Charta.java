package com.example.chartographer.image;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;

public class Charta {

    public static final int MIN_WIDTH = 1;
    public static final int MAX_WIDTH = 20000;
    public static final int MIN_HEIGHT = 1;
    public static final int MAX_HEIGHT = 50000;


    private static int nextId = 0;

    private int id;
    @Min(1)
    @Max(20000)
    private int width;
    @Min(1)
    @Max(50000)
    private int height;
    private List<ImagePiece> pieces;

    public Charta(int id, int width, int height) {
        this.id = id;
        this.width = width;
        this.height = height;
        pieces = new ArrayList<>();
    }


    public Charta(int width, int height) {
        this(++nextId, width, height);
    }

    public static int getNextId() {
        return nextId;
    }

    public static void setNextId(int id) {
        nextId = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public void addPiece(ImagePiece piece) {
        pieces.add(piece);
    }

    public List<ImagePiece> getPieces() {
        return pieces;
    }

    public void setPieces(List<ImagePiece> pieces) {
        this.pieces = pieces;
    }

    public boolean hasFragmentInArea(int x, int y, int width, int height) {
        if ((x > this.width) || (y > this.height)) return false;
        for (ImagePiece piece : pieces) {
            if ((x + width > piece.getX())
                    && (x < piece.getX() + piece.getWidth())
                    && (y + height > piece.getY())
                    && (y < piece.getY() + piece.getHeight())) {
                return true;
            }
        }
        return false;
    }
}
