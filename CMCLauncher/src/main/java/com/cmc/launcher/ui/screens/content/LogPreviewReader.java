package com.cmc.launcher.ui.screens.content;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

final class LogPreviewReader {
    static final int MAX_BYTES = 8 * 1024 * 1024;

    static final class Preview {
        final String text;
        final boolean truncated;

        Preview(String text, boolean truncated) {
            this.text = text;
            this.truncated = truncated;
        }
    }

    static Preview read(File file) throws IOException {
        try (RandomAccessFile stream = new RandomAccessFile(file, "r")) {
            long length = stream.length();
            int limit = (int) Math.min(length, MAX_BYTES);
            long offset = length - limit;
            boolean partialLine = false;
            if (offset > 0) {
                stream.seek(offset - 1);
                partialLine = stream.read() != 10;
            }
            byte[] bytes = new byte[limit];
            int count = 0;
            while (count < limit) {
                int n = stream.read(bytes, count, limit - count);
                if (n < 0) break;
                count += n;
            }
            int start = 0;
            if (partialLine) {
                int end = 0;
                while (end < count && bytes[end] != 10) end++;
                if (end + 1 < count) {
                    start = end + 1;
                } else {
                    while (start < count && (bytes[start] & 0xc0) == 0x80) start++;
                }
            }
            return new Preview(new String(bytes, start, count - start, StandardCharsets.UTF_8), offset > 0);
        }
    }

    private LogPreviewReader() {}
}
