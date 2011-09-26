/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package cubetech.misc;

import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.HashSet;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * This class implements an output stream filter for writing files in the
 * ZIP file format. Includes support for both compressed and uncompressed
 * entries.
 *
 * @author	David Connelly
 * @version	%I%, %G%
 */
public class FastZipOutputStream extends DeflaterOutputStream  { //
    /*
     * Header signatures
     */
    static long LOCSIG = 0x04034b50L;	// "PK\003\004"
    static long EXTSIG = 0x08074b50L;	// "PK\007\008"
    static long CENSIG = 0x02014b50L;	// "PK\001\002"
    static long ENDSIG = 0x06054b50L;	// "PK\005\006"

    /*
     * Header sizes in bytes (including signatures)
     */
    static final int LOCHDR = 30;	// LOC header size
    static final int EXTHDR = 16;	// EXT header size
    static final int CENHDR = 46;	// CEN header size
    static final int ENDHDR = 22;	// END header size

    /*
     * Local file (LOC) header field offsets
     */
    static final int LOCVER = 4;	// version needed to extract
    static final int LOCFLG = 6;	// general purpose bit flag
    static final int LOCHOW = 8;	// compression method
    static final int LOCTIM = 10;	// modification time
    static final int LOCCRC = 14;	// uncompressed file crc-32 value
    static final int LOCSIZ = 18;	// compressed size
    static final int LOCLEN = 22;	// uncompressed size
    static final int LOCNAM = 26;	// filename length
    static final int LOCEXT = 28;	// extra field length

    /*
     * Extra local (EXT) header field offsets
     */
    static final int EXTCRC = 4;	// uncompressed file crc-32 value
    static final int EXTSIZ = 8;	// compressed size
    static final int EXTLEN = 12;	// uncompressed size

    /*
     * Central directory (CEN) header field offsets
     */
    static final int CENVEM = 4;	// version made by
    static final int CENVER = 6;	// version needed to extract
    static final int CENFLG = 8;	// encrypt, decrypt flags
    static final int CENHOW = 10;	// compression method
    static final int CENTIM = 12;	// modification time
    static final int CENCRC = 16;	// uncompressed file crc-32 value
    static final int CENSIZ = 20;	// compressed size
    static final int CENLEN = 24;	// uncompressed size
    static final int CENNAM = 28;	// filename length
    static final int CENEXT = 30;	// extra field length
    static final int CENCOM = 32;	// comment length
    static final int CENDSK = 34;	// disk number start
    static final int CENATT = 36;	// internal file attributes
    static final int CENATX = 38;	// external file attributes
    static final int CENOFF = 42;	// LOC header offset

    /*
     * End of central directory (END) header field offsets
     */
    static final int ENDSUB = 8;	// number of entries on this disk
    static final int ENDTOT = 10;	// total number of entries
    static final int ENDSIZ = 12;	// central directory size in bytes
    static final int ENDOFF = 16;	// offset of first CEN header
    static final int ENDCOM = 20;	// zip file comment length
    private static class XEntry {
	public final ZipEntry entry;
	public final long offset;
	public final int flag;
	public XEntry(ZipEntry entry, long offset) {
	    this.entry = entry;
	    this.offset = offset;
	    this.flag = (entry.getMethod() == DEFLATED &&
			 (entry.getSize()  == -1 ||
			  entry.getCompressedSize() == -1 ||
			  entry.getCrc()   == -1))
		// store size, compressed size, and crc-32 in data descriptor
		// immediately following the compressed entry data
		? 8
		// store size, compressed size, and crc-32 in LOC header
		: 0;
	}
    }

    private XEntry current;
    private Vector<XEntry> xentries = new Vector<XEntry>();
    private HashSet<String> names = new HashSet<String>();
//    private CRC32 crc = new CRC32();
    private long written = 0;
    private long locoff = 0;
    private String comment;
    private int method = DEFLATED;
    private boolean finished;

    private boolean closed = false;

    private static int version(ZipEntry e) throws ZipException {
	switch (e.getMethod()) {
	case DEFLATED: return 20;
	case STORED:   return 10;
	default: throw new ZipException("unsupported compression method");
	}
    }

    /**
     * Checks to make sure that this stream has not been closed.
     */
    private void ensureOpen() throws IOException {
	if (closed) {
	    throw new IOException("Stream closed");
        }
    }
    /**
     * Compression method for uncompressed (STORED) entries.
     */
    public static final int STORED = ZipEntry.STORED;

    /**
     * Compression method for compressed (DEFLATED) entries.
     */
    public static final int DEFLATED = ZipEntry.DEFLATED;

    /**
     * Creates a new ZIP output stream.
     * @param out the actual output stream
     */
    public FastZipOutputStream(OutputStream out) {
	super(out, new Deflater(3, true));
    }

    /**
     * Sets the ZIP file comment.
     * @param comment the comment string
     * @exception IllegalArgumentException if the length of the specified
     *		  ZIP file comment is greater than 0xFFFF bytes
     */
    public void setComment(String comment) {
        if (comment != null && comment.length() > 0xffff/3
                                           && getUTF8Length(comment) > 0xffff) {
	    throw new IllegalArgumentException("ZIP file comment too long.");
	}
	this.comment = comment;
    }

    /**
     * Sets the default compression method for subsequent entries. This
     * default will be used whenever the compression method is not specified
     * for an individual ZIP file entry, and is initially set to DEFLATED.
     * @param method the default compression method
     * @exception IllegalArgumentException if the specified compression method
     *		  is invalid
     */
    public void setMethod(int method) {
	if (method != DEFLATED && method != STORED) {
	    throw new IllegalArgumentException("invalid compression method");
	}
	this.method = method;
    }

    /**
     * Sets the compression level for subsequent entries which are DEFLATED.
     * The default setting is DEFAULT_COMPRESSION.
     * @param level the compression level (0-9)
     * @exception IllegalArgumentException if the compression level is invalid
     */
    public void setLevel(int level) {
	def.setLevel(level);
    }

    /**
     * Begins writing a new ZIP file entry and positions the stream to the
     * start of the entry data. Closes the current entry if still active.
     * The default compression method will be used if no compression method
     * was specified for the entry, and the current time will be used if
     * the entry has no set modification time.
     * @param e the ZIP entry to be written
     * @exception ZipException if a ZIP format error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public void putNextEntry(ZipEntry e) throws IOException {
	ensureOpen();
	if (current != null) {
	    closeEntry();	// close previous entry
	}
	if (e.getTime() == -1) {
	    e.setTime(System.currentTimeMillis());
	}
	if (e.getMethod() == -1) {
	    e.setMethod(method);	// use default method
	}
	switch (e.getMethod()) {
	case DEFLATED:
	    break;
	case STORED:
	    // compressed size, uncompressed size, and crc-32 must all be
	    // set for entries using STORED compression method
	    if (e.getSize() == -1) {
		e.setSize(e.getCompressedSize());
	    } else if (e.getCompressedSize() == -1) {
		e.setCompressedSize(e.getSize());
	    } else if (e.getSize() != e.getCompressedSize()) {
		throw new ZipException(
		    "STORED entry where compressed != uncompressed size");
	    }
	    if (e.getSize() == -1 || e.getCrc() == -1) {
		throw new ZipException(
		    "STORED entry missing size, compressed size, or crc-32");
	    }
	    break;
	default:
	    throw new ZipException("unsupported compression method");
	}
	if (! names.add(e.getName())) {
	    throw new ZipException("duplicate entry: " + e.getName());
	}
	current = new XEntry(e, written);
	xentries.add(current);
        writeLOC(current);
    }

    /**
     * Closes the current ZIP entry and positions the stream for writing
     * the next entry.
     * @exception ZipException if a ZIP format error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public void closeEntry() throws IOException {
	ensureOpen();
	if (current != null) {
	    ZipEntry e = current.entry;
	    switch (e.getMethod()) {
	    case DEFLATED:
		def.finish();
		while (!def.finished()) {
		    deflate();
		}
		if ((current.flag & 8) == 0) {
		    // verify size, compressed size, and crc-32 settings
		    if (e.getSize() != def.getBytesRead()) {
			throw new ZipException(
			    "invalid entry size (expected " + e.getSize() +
			    " but got " + def.getBytesRead() + " bytes)");
		    }
		    if (e.getCompressedSize() != def.getBytesWritten()) {
			throw new ZipException(
			    "invalid entry compressed size (expected " +
			    e.getCompressedSize() + " but got " + def.getBytesWritten() + " bytes)");
		    }
//		    if (e.crc != crc.getValue()) {
//			throw new ZipException(
//			    "invalid entry CRC-32 (expected 0x" +
//			    Long.toHexString(e.crc) + " but got 0x" +
//			    Long.toHexString(crc.getValue()) + ")");
//		    }
		} else {
		    e.setSize(def.getBytesRead());
		    e.setCompressedSize(def.getBytesWritten());
		    e.setCrc(0);//crc.getValue();
		    writeEXT(e);
		}
		def.reset();
		written += e.getCompressedSize();
		break;
	    case STORED:
		// we already know that both e.size and e.csize are the same
		if (e.getSize() != written - locoff) {
		    throw new ZipException(
			"invalid entry size (expected " + e.getSize() +
			" but got " + (written - locoff) + " bytes)");
		}
//		if (e.crc != crc.getValue()) {
//		    throw new ZipException(
//			 "invalid entry crc-32 (expected 0x" +
//			 Long.toHexString(e.crc) + " but got 0x" +
//			 Long.toHexString(crc.getValue()) + ")");
//		}
		break;
	    default:
		throw new ZipException("invalid compression method");
	    }
//	    crc.reset();
	    current = null;
	}
    }

    /**
     * Writes an array of bytes to the current ZIP entry data. This method
     * will block until all the bytes are written.
     * @param b the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public synchronized void write(byte[] b, int off, int len)
	throws IOException
    {
	ensureOpen();
        if (off < 0 || len < 0 || off > b.length - len) {
	    throw new IndexOutOfBoundsException();
	} else if (len == 0) {
	    return;
	}

	if (current == null) {
	    throw new ZipException("no current ZIP entry");
	}
	ZipEntry entry = current.entry;
	switch (entry.getMethod()) {
	case DEFLATED:
	    super.write(b, off, len);
	    break;
	case STORED:
	    written += len;
	    if (written - locoff > entry.getSize()) {
		throw new ZipException(
		    "attempt to write past end of STORED entry");
	    }
	    out.write(b, off, len);
	    break;
	default:
	    throw new ZipException("invalid compression method");
	}
//	crc.update(b, off, len);
    }

    /**
     * Finishes writing the contents of the ZIP output stream without closing
     * the underlying stream. Use this method when applying multiple filters
     * in succession to the same output stream.
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O exception has occurred
     */
    public void finish() throws IOException {
	ensureOpen();
	if (finished) {
	    return;
	}
	if (current != null) {
	    closeEntry();
	}
	if (xentries.size() < 1) {
	    throw new ZipException("ZIP file must have at least one entry");
	}
	// write central directory
	long off = written;
	for (XEntry xentry : xentries)
	    writeCEN(xentry);
	writeEND(off, written - off);
	finished = true;
    }

    /**
     * Closes the ZIP output stream as well as the stream being filtered.
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public void close() throws IOException {
        if (!closed) {
            super.close();
            def.end();
            closed = true;
        }
    }

    /*
     * Writes local file (LOC) header for specified entry.
     */
    private void writeLOC(XEntry xentry) throws IOException {
	ZipEntry e = xentry.entry;
	int flag = xentry.flag;
	writeInt(LOCSIG);	    // LOC header signature
	writeShort(version(e));     // version needed to extract
	writeShort(flag);           // general purpose bit flag
	writeShort(e.getMethod());       // compression method
	writeInt(e.getTime());           // last modification time
	if ((flag & 8) == 8) {
	    // store size, uncompressed size, and crc-32 in data descriptor
	    // immediately following compressed entry data
	    writeInt(0);
	    writeInt(0);
	    writeInt(0);
	} else {
	    writeInt(e.getCrc());        // crc-32
	    writeInt(e.getCompressedSize());      // compressed size
	    writeInt(e.getSize());       // uncompressed size
	}
	byte[] nameBytes = getUTF8Bytes(e.getName());
	writeShort(nameBytes.length);
	writeShort(e.getExtra() != null ? e.getExtra().length : 0);
	writeBytes(nameBytes, 0, nameBytes.length);
	if (e.getExtra() != null) {
	    writeBytes(e.getExtra(), 0, e.getExtra().length);
	}
	locoff = written;
    }

    /*
     * Writes extra data descriptor (EXT) for specified entry.
     */
    private void writeEXT(ZipEntry e) throws IOException {
	writeInt(EXTSIG);	    // EXT header signature
	writeInt(e.getCrc());	    // crc-32
	writeInt(e.getCompressedSize());	    // compressed size
	writeInt(e.getSize());	    // uncompressed size
    }

    /*
     * Write central directory (CEN) header for specified entry.
     * REMIND: add support for file attributes
     */
    private void writeCEN(XEntry xentry) throws IOException {
	ZipEntry e  = xentry.entry;
	int flag = xentry.flag;
	int version = version(e);
	writeInt(CENSIG);	    // CEN header signature
	writeShort(version);	    // version made by
	writeShort(version);	    // version needed to extract
	writeShort(flag);	    // general purpose bit flag
	writeShort(e.getMethod());	    // compression method
	writeInt(e.getTime());	    // last modification time
	writeInt(e.getCrc());	    // crc-32
	writeInt(e.getCompressedSize());	    // compressed size
	writeInt(e.getSize());	    // uncompressed size
	byte[] nameBytes = getUTF8Bytes(e.getName());
	writeShort(nameBytes.length);
	writeShort(e.getExtra() != null ? e.getExtra().length : 0);
	byte[] commentBytes;
	if (e.getComment() != null) {
	    commentBytes = getUTF8Bytes(e.getComment());
	    writeShort(commentBytes.length);
	} else {
	    commentBytes = null;
	    writeShort(0);
	}
	writeShort(0);		    // starting disk number
	writeShort(0);		    // internal file attributes (unused)
	writeInt(0);		    // external file attributes (unused)
	writeInt(xentry.offset);    // relative offset of local header
	writeBytes(nameBytes, 0, nameBytes.length);
	if (e.getExtra() != null) {
	    writeBytes(e.getExtra(), 0, e.getExtra().length);
	}
	if (commentBytes != null) {
	    writeBytes(commentBytes, 0, commentBytes.length);
	}
    }

    /*
     * Writes end of central directory (END) header.
     */
    private void writeEND(long off, long len) throws IOException {
	int count = xentries.size();
	writeInt(ENDSIG);	    // END record signature
	writeShort(0);		    // number of this disk
	writeShort(0);		    // central directory start disk
	writeShort(count);	    // number of directory entries on disk
	writeShort(count);	    // total number of directory entries
	writeInt(len);		    // length of central directory
	writeInt(off);		    // offset of central directory
	if (comment != null) {	    // zip file comment
	    byte[] b = getUTF8Bytes(comment);
	    writeShort(b.length);
	    writeBytes(b, 0, b.length);
	} else {
	    writeShort(0);
	}
    }

    /*
     * Writes a 16-bit short to the output stream in little-endian byte order.
     */
    private void writeShort(int v) throws IOException {
	OutputStream out = this.out;
	out.write((v >>> 0) & 0xff);
	out.write((v >>> 8) & 0xff);
	written += 2;
    }

    /*
     * Writes a 32-bit int to the output stream in little-endian byte order.
     */
    private void writeInt(long v) throws IOException {
	OutputStream out = this.out;
	out.write((int)((v >>>  0) & 0xff));
	out.write((int)((v >>>  8) & 0xff));
	out.write((int)((v >>> 16) & 0xff));
	out.write((int)((v >>> 24) & 0xff));
	written += 4;
    }

    /*
     * Writes an array of bytes to the output stream.
     */
    private void writeBytes(byte[] b, int off, int len) throws IOException {
	super.out.write(b, off, len);
	written += len;
    }

    /*
     * Returns the length of String's UTF8 encoding.
     */
    static int getUTF8Length(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch <= 0x7f) {
                count++;
            } else if (ch <= 0x7ff) {
                count += 2;
            } else {
                count += 3;
            }
        }
        return count;
    }

    /*
     * Returns an array of bytes representing the UTF8 encoding
     * of the specified String.
     */
    private static byte[] getUTF8Bytes(String s) {
	char[] c = s.toCharArray();
	int len = c.length;
	// Count the number of encoded bytes...
	int count = 0;
	for (int i = 0; i < len; i++) {
	    int ch = c[i];
	    if (ch <= 0x7f) {
		count++;
	    } else if (ch <= 0x7ff) {
		count += 2;
	    } else {
		count += 3;
	    }
	}
	// Now return the encoded bytes...
	byte[] b = new byte[count];
	int off = 0;
	for (int i = 0; i < len; i++) {
	    int ch = c[i];
	    if (ch <= 0x7f) {
		b[off++] = (byte)ch;
	    } else if (ch <= 0x7ff) {
		b[off++] = (byte)((ch >> 6) | 0xc0);
		b[off++] = (byte)((ch & 0x3f) | 0x80);
	    } else {
		b[off++] = (byte)((ch >> 12) | 0xe0);
		b[off++] = (byte)(((ch >> 6) & 0x3f) | 0x80);
		b[off++] = (byte)((ch & 0x3f) | 0x80);
	    }
	}
	return b;
    }
}
