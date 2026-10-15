package id.feezet.efauth;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal streaming TAR writer (ustar format). No external dependency.
 *
 * <p>Used by EXPORT TAR to pack an eFootball game-data folder
 * ({@code assetpacks/} + {@code SaveData/}) into a KONAMI.TAR-style
 * {@code .tar} file. Everything is streamed in small chunks so a ~2 GB
 * export never holds file contents in memory.</p>
 *
 * <p>Header layout (512 bytes): name[100], mode[8], uid[8], gid[8],
 * size[12 octal], mtime[12 octal], chksum[8], typeflag[1],
 * linkname[100], magic[6] "ustar\0", version[2] "00",
 * uname[32], gname[32], devmajor[8], devminor[8], prefix[155].
 * Paths longer than 100 chars are split into prefix + name at a '/'.
 * The archive ends with two 512-byte zero blocks.</p>
 */
public class TarWriter implements Closeable {

    private static final int BLOCK = 512;
    private static final int NAME_LEN = 100;
    private static final int PREFIX_LEN = 155;

    private final OutputStream out;
    private long bytesWritten = 0;

    public TarWriter(OutputStream out) {
        this.out = new BufferedOutputStream(out, 8192);
    }

    /** Total bytes written so far (for progress reporting). */
    public long getBytesWritten() {
        return bytesWritten;
    }

    /** Adds a directory entry. {@code path} must end with '/'. */
    public void addDirectory(String path, long mtime) throws IOException {
        writeHeader(normalizeDir(path), 0755, 0, mtime, '5');
    }

    /** Streams a file entry. The caller owns and closes {@code in}. */
    public void addFile(String path, long size, long mtime, InputStream in) throws IOException {
        writeHeader(path, 0644, size, mtime, '0');
        byte[] buf = new byte[8192];
        long remaining = size;
        long written = 0;
        while (remaining > 0) {
            int want = (int) Math.min(buf.length, remaining);
            int n = in.read(buf, 0, want);
            if (n < 0) break;
            out.write(buf, 0, n);
            bytesWritten += n;
            written += n;
            remaining -= n;
        }
        if (written != size) {
            // Never emit a silently truncated entry: abort so the caller
            // fails the whole export instead of producing a corrupt archive.
            throw new IOException("short read on " + path + " (expected " + size + ", got " + written + ")");
        }
        // Pad the file's data to a 512-byte boundary.
        long pad = (BLOCK - (size % BLOCK)) % BLOCK;
        writeZeros(pad);
    }

    /** Writes the two zero end blocks and flushes. */
    public void finish() throws IOException {
        writeZeros(BLOCK * 2L);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    private static String normalizeDir(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    private void writeHeader(String path, int mode, long size, long mtime, char typeflag) throws IOException {
        byte[] header = new byte[BLOCK];

        // Split long paths into prefix + name at a '/' boundary (ustar rule).
        String name = path;
        String prefix = "";
        if (name.getBytes(StandardCharsets.UTF_8).length > NAME_LEN) {
            int split = -1;
            for (int i = 0; i < name.length(); i++) {
                if (name.charAt(i) == '/') {
                    String head = name.substring(0, i);
                    String tail = name.substring(i + 1);
                    if (head.getBytes(StandardCharsets.UTF_8).length <= PREFIX_LEN
                            && tail.getBytes(StandardCharsets.UTF_8).length <= NAME_LEN) {
                        split = i;
                    }
                }
            }
            if (split < 0) {
                throw new IOException("Tar path too long (no usable '/' split): " + path);
            }
            prefix = name.substring(0, split);
            name = name.substring(split + 1);
        }

        putString(header, 0, NAME_LEN, name);
        putOctal(header, 100, 8, mode);
        putOctal(header, 108, 8, 0); // uid
        putOctal(header, 116, 8, 0); // gid
        putOctal(header, 124, 12, size);
        putOctal(header, 136, 12, mtime / 1000L);
        // chksum field: 8 bytes — filled with spaces while computing.
        for (int i = 148; i < 156; i++) header[i] = (byte) ' ';
        header[156] = (byte) typeflag;
        putString(header, 257, 6, "ustar");
        header[262] = 0;
        putString(header, 263, 2, "00");
        putString(header, 265, 32, "");
        putString(header, 297, 32, "");
        putOctal(header, 329, 8, 0); // devmajor
        putOctal(header, 337, 8, 0); // devminor
        putString(header, 345, PREFIX_LEN, prefix);

        long sum = 0;
        for (byte b : header) sum += b & 0xFF;
        putOctal(header, 148, 8, sum);

        out.write(header);
        bytesWritten += BLOCK;
    }

    private void writeZeros(long count) throws IOException {
        byte[] zeros = new byte[BLOCK];
        long remaining = count;
        while (remaining > 0) {
            int n = (int) Math.min(zeros.length, remaining);
            out.write(zeros, 0, n);
            bytesWritten += n;
            remaining -= n;
        }
    }

    private static void putString(byte[] buf, int offset, int length, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int n = Math.min(bytes.length, length);
        System.arraycopy(bytes, 0, buf, offset, n);
    }

    /** Writes {@code value} as a zero-padded octal field, NUL-terminated. */
    private static void putOctal(byte[] buf, int offset, int length, long value) {
        String octal = Long.toOctalString(value);
        int digits = length - 1; // last byte is NUL
        int start = offset + Math.max(0, digits - octal.length());
        for (int i = offset; i < offset + digits; i++) buf[i] = (byte) '0';
        byte[] octBytes = octal.getBytes(StandardCharsets.US_ASCII);
        int copy = Math.min(octBytes.length, digits);
        System.arraycopy(octBytes, 0, buf, start, copy);
        buf[offset + length - 1] = 0;
    }
}
