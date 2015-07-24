package com.example.wv.myapplication;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

class PrintableImage {
    private InputStream imageInputStream;
    private int h;
    private int w;
    private int bitCount;
    private byte[] imageData;

    private PrintableImage(InputStream is) {
        this.imageInputStream = is;
    }

    public static PrintableImage create(String fileName)
            throws FileNotFoundException {
        File file = new File(fileName);
        if (!file.exists()) {
            return null;
        } else {
            PrintableImage image = new PrintableImage(new BufferedInputStream(
                    new FileInputStream(fileName)));
            image.loadImage();
            return image;
        }
    }

    public static PrintableImage create(InputStream is) {
        PrintableImage image = new PrintableImage(
                (InputStream) (is instanceof BufferedInputStream ? is
                        : new BufferedInputStream(is)));
        image.loadImage();
        return image;
    }

    public byte[] getImageData() {
        return this.imageData;
    }

    public int getHeight() {
        return this.h;
    }

    public int getWidth() {
        return this.w;
    }

    private void loadImage() {
        if (this.imageInputStream != null) {
            try {
                byte e = 14;
                byte[] header = new byte[e];
                this.imageInputStream.read(header, 0, e);
                if (!(new String(header, 0, 2, "GBK")).equals("BM")) {
                    return;
                }

                byte infoLen = 40;
                byte[] info = new byte[infoLen];
                this.imageInputStream.read(info, 0, infoLen);
                this.w = this.getInt(info, 4);
                this.h = this.getInt(info, 8);
                int outputW = (this.w + 7) / 8 * 8;
                int outputH = (this.h + 7) / 8 * 8;
                int compress = this.getInt(info, 16);
                if (compress != 0) {
                    return;
                }

                this.bitCount = this.getShort(info, 14);
                if (this.bitCount > 1) {
                    return;
                }

                int lineSize = (this.getWidth() * this.bitCount + 31) / 32 * 4;
                int outputLineSize = outputW / 8;
                int offset = outputLineSize * this.getHeight();
                int bufferSize = outputLineSize * outputH;
                byte[] bmpData = new byte[bufferSize];
                this.imageInputStream.read(bmpData, 0, 8);
                boolean needRevert = bmpData[0] != -1;
                Arrays.fill(bmpData, (byte) (~bmpData[0]));
                byte[] readBuffer = new byte[lineSize];

                for (int i = 0; i < this.getHeight(); ++i) {
                    this.imageInputStream.read(readBuffer, 0, lineSize);
                    int readOffset = offset - (i + 1) * outputLineSize;
                    System.arraycopy(readBuffer, 0, bmpData, readOffset,
                            outputLineSize);
                }

                if (needRevert) {
                    this.reverseImageColor(bmpData);
                }

                this.w = outputW;
                this.h = outputH;
                this.imageData = this.changeImageCorner(bmpData, outputW,
                        outputH);
            } catch (FileNotFoundException var17) {
                var17.printStackTrace();
            } catch (IOException var18) {
                var18.printStackTrace();
            }

        }
    }

    private void reverseImageColor(byte[] data) {
        for (int i = 0; i < data.length; ++i) {
            data[i] = (byte) (~data[i]);
        }

    }

    private byte[] changeImageCorner(byte[] in, int w, int h) {
        int cols = w / 8;
        int bitsOfLine = w;
        int lines = h;
        byte[] out = new byte[h * cols];

        for (int x = 0; x < bitsOfLine; ++x) {
            int col = x / 8;

            for (int y = 0; y < lines; ++y) {
                out[w * (y / 8) + x] |= (byte) ((in[y * cols + col] & 128 >>> (x & 7)) != 0 ? 1 << (y & 7)
                        : 0);
            }
        }

        return out;
    }

    private int getInt(byte[] four, int offset) {
        return (four[offset + 3] & 255) << 24 | (four[offset + 2] & 255) << 16
                | (four[offset + 1] & 255) << 8 | four[offset] & 255;
    }

    private int getShort(byte[] four, int offset) {
        return (four[offset + 1] & 255) << 8 | four[offset] & 255;
    }
}
