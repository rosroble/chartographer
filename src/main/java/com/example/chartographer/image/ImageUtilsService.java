package com.example.chartographer.image;

import com.example.chartographer.config.StartupRunner;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;

@Service
public class ImageUtilsService {
    public static final int BITMAP_FILE_HEADER_SIZE = 14;
    private static final char BMP_TYPE = 0x4D42;
    private static final int BMP_RESERVED = 0;
    private static final int BMP_COMPRESSION = 0;
    private static final char BMP_PLANES = 1;
    private static final int BMP_COLOR_USED = 0; // mandatory for color depths <= 8 bits
    private static final int BMP_COLOR_IMPORTANT = 0;
    private static final int PIXELS_PER_METER_WIDTH = 2834;
    private static final int PIXELS_PER_METER_HEIGHT = 2834;
    private static final char COLOR_DEPTH = 24;
    private static final int DWORD_SIZE = 4;
    private static final int DIB_HEADER_SIZE = 40;
    private static final int DIB_MIN_HEADER_SIZE = 12;
    private static final int DIB_MAX_HEADER_SIZE = 124;
    private static final int PIXEL_SIZE = 3;


    public boolean createEmptyBMPfile(int width, int height, String filename) throws IOException {
        OutputStream fos = Files.newOutputStream(StartupRunner.getPathToWorkingDirectory().resolve(filename + ".bmp"));
        if (writeHeaderToFile(fos, width, height)) {
            writeEmptyImage(fos, width, height);
            fos.close();
        }
        return false;
    }

    public boolean appendToImage(Charta c, InputStream is, int id, int x, int y, int width, int height) throws IOException {

        if (c.getWidth() < x + width || c.getHeight() < y + height) {
            return false;
        }

        File pathToChartaBmp = StartupRunner.getPathToWorkingDirectory().resolve(id + ".bmp").toFile();
        RandomAccessFile chartaImage = new RandomAccessFile(pathToChartaBmp, "rw");
        byte[] image = new byte[1 << 16];
        int read = is.readNBytes(image, 0, BITMAP_FILE_HEADER_SIZE + 4);
        if (read < BITMAP_FILE_HEADER_SIZE + 4) {
            return false;
        }
        int imageDIBHeaderSize = validateBmpHeader(image);
        is.readNBytes(imageDIBHeaderSize - 4);

        int fragmentPadding = getPadding(width);
        int chartaPadding = getPadding(c.getWidth());

        long offsetToFirstPx = ((long) (c.getHeight() - 1) * c.getWidth()) * PIXEL_SIZE + (long) (c.getHeight() - 1) * chartaPadding;
        long offsetToStartOfNewData = ((long) c.getWidth() * (height + y - 1) - x) * PIXEL_SIZE + (long) chartaPadding * (height + y - 1);

        chartaImage.seek(DIB_HEADER_SIZE + BITMAP_FILE_HEADER_SIZE + offsetToFirstPx - offsetToStartOfNewData);
        try {
            for (int i = 0; i < height; i++) {
                int t = is.readNBytes(image, 0, width * PIXEL_SIZE + fragmentPadding);
                read += t;
                chartaImage.write(image, 0, width * PIXEL_SIZE);
                chartaImage.skipBytes((c.getWidth() - width) * PIXEL_SIZE + chartaPadding);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            chartaImage.close();
        }
        return true;
    }

    public BufferedImage readFragment(Charta c, int x, int y, int width, int height) throws IOException {
        return (x + width > c.getWidth() || y + height > c.getHeight()) ?
                readFragmentOutOfBounds(c, x, y, width, height) :
                readFragmentInBounds(c, x, y, width, height);
    }

    public boolean deleteCharta(int id) {
        try {
            Files.delete(StartupRunner.workDirResolve(id + ".json"));
            Files.delete(StartupRunner.workDirResolve(id + ".bmp"));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public int validateBmpHeader(byte[] header) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(header).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        char fileFormat = byteBuffer.getChar();
        if (fileFormat != BMP_TYPE) return -1;
        return byteBuffer.getInt(BITMAP_FILE_HEADER_SIZE);
    }

    private BufferedImage readFragmentInBounds(Charta c, int x, int y, int width, int height) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("bmp");
        ImageReader reader = readers.next();
        File img = StartupRunner.workDirResolve(c.getId() + ".bmp").toFile();
        ImageInputStream iis = ImageIO.createImageInputStream(img);
        reader.setInput(iis, true);
        ImageReadParam irp = reader.getDefaultReadParam();
        int imageIndex = 0;
        Rectangle rect = new Rectangle(x, y, width, height);
        irp.setSourceRegion(rect);
        BufferedImage bi = reader.read(imageIndex, irp);
        return bi;
    }

    private BufferedImage readFragmentOutOfBounds(Charta c, int x, int y, int width, int height) throws IOException {
        createEmptyBMPfile(width, height, "-1");
        BufferedImage restricted = readFragmentInBounds(c, x, y, width, height);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(restricted, "bmp", os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        Charta tmp = new Charta(-1, width, height);
        appendToImage(tmp, is, -1, 0, 0, restricted.getWidth(), restricted.getHeight());
        BufferedImage finalPiece = ImageIO.read(StartupRunner.workDirResolve("-1.bmp").toFile());
        Files.deleteIfExists(StartupRunner.getPathToWorkingDirectory().resolve("-1.bmp"));
        return finalPiece;
    }

    private boolean writeHeaderToFile(OutputStream fos, int width, int height) {
        int imageSize = (PIXEL_SIZE * width + getPadding(width)) * height;
        try {
            ByteBuffer buffer = ByteBuffer.allocate(DIB_HEADER_SIZE + BITMAP_FILE_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putChar(BMP_TYPE).
                    putInt(DIB_HEADER_SIZE + BMP_RESERVED + imageSize).
                    putInt(BMP_RESERVED).
                    putInt(DIB_HEADER_SIZE + BITMAP_FILE_HEADER_SIZE).
                    putInt(DIB_HEADER_SIZE).
                    putInt(width).
                    putInt(height).
                    putChar(BMP_PLANES).
                    putChar(COLOR_DEPTH).
                    putInt(BMP_COMPRESSION).
                    putInt(imageSize).
                    putInt(PIXELS_PER_METER_WIDTH).
                    putInt(PIXELS_PER_METER_HEIGHT).
                    putInt(BMP_COLOR_USED).
                    putInt(BMP_COLOR_IMPORTANT);
            fos.write(buffer.array());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean writeEmptyImage(OutputStream fos, int width, int height) {
        try {
            int padding = getPadding(width);
            byte[] empty = new byte[width * 3];
            byte[] paddingTrash = new byte[padding];
            Arrays.fill(paddingTrash, (byte) 0xFF); // init with non-zero trash
            for (int i = 0; i < height; i++) {
                fos.write(empty);
                fos.write(paddingTrash);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private byte getPadding(int width) {
        return (byte) ((DWORD_SIZE - (width * (COLOR_DEPTH / 8)) % DWORD_SIZE) % DWORD_SIZE);
    }

}