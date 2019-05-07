package com.stephen.core;
import com.google.zxing.LuminanceSource;

public class YUVLuminanceSource  extends LuminanceSource {

    private final byte[] mYuvData;

    public YUVLuminanceSource(byte[] yuvData, int width, int height) {
        super(width, height);

        mYuvData = yuvData;
    }

    @Override
    public byte[] getRow(int y, byte[] row) {
        if (y < 0 || y >= getHeight()) {
            throw new IllegalArgumentException("Requested row is outside the image: " + y);
        }
        final int width = getWidth();
        if (row == null || row.length < width) {
            row = new byte[width];
        }
        final int offset = y * width;
        System.arraycopy(mYuvData, offset, row, 0, width);
        return row;
    }

    @Override
    public byte[] getMatrix() {
        return mYuvData;
    }

    @Override
    public boolean isCropSupported() {
        return true;
    }

}