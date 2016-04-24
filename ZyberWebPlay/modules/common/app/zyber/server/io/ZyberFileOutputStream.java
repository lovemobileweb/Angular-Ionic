package zyber.server.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import zyber.server.CassandraMapperDelegate;
import zyber.server.dao.FileData;
import zyber.server.dao.FileVersion;

/**
 * A speedy implementation of ByteArrayOutputStream. It's not synchronized, and
 * it does not copy buffers when it's expanded. There's also no copying of the
 * internal buffer if it's contents is extracted with the writeTo(stream)
 * method.
 *
 * @author Rickard ?berg
 * @author Brat Baker (Atlassian)
 * @author Alexey
 * @version $Date: 2008-01-19 10:09:56 +0800 (Sat, 19 Jan 2008) $ $Id:
 *          FastByteArrayOutputStream.java 3000 2008-01-19 02:09:56Z tm_jee $
 */
public class ZyberFileOutputStream extends OutputStream {
	private static class Block {
		public Block(long blockNumber, byte[] data) {
			this.blockNumber = blockNumber;
			this.data = data;
		}

		public final long blockNumber;
		public final byte[] data;

		public String toString() {
			return getClass().getName() + "[block#:" + blockNumber + " data:" + data.length + " bytes]";
		}
	}

	private LinkedList<Block> buffers = new LinkedList<Block>();

	// Attributes ----------------------------------------------------
	// internal buffer
	private byte[] buffer;

	// is the stream closed?
	private boolean closed;
	private int blockSize;
	/** Position in the current buffer. */
	private int index;
	private int size;
	
	private FileVersion fileVersion;

	private final CassandraMapperDelegate<FileData> fileDataMapper;
	private final CassandraMapperDelegate<FileVersion> fileVersionMapper;
	

	// Constructors --------------------------------------------------
	public ZyberFileOutputStream(FileVersion fileVersion) {
		blockSize = fileVersion.getBlockSize();
		buffer = new byte[blockSize];
		this.fileVersion = fileVersion;		
		fileDataMapper = fileVersion.getZus().mapper(FileData.class);
		fileVersionMapper = fileVersion.getZus().mapper(FileVersion.class);

	}

	public int getSize() {
		return size + index;
	}

	public void close() {
		flush();
		
		closed = true;
	}

	private synchronized void flushCompletedBuffers() {
		// Write the completed buffers.
		for (Block b : buffers) {
			FileData fd = new FileData(fileVersion.getPathId(), b.blockNumber, ByteBuffer.wrap(b.data), fileVersion.getZus(),fileVersion.getVersion().getTime());
			fileDataMapper.save(fd);
		}

		buffers.clear();

	}

	public synchronized void flush() {
		flushCompletedBuffers();
		if (index != 0) {
			int partialBlockNo = size / blockSize;

			// NOTE: It looks like this could cause duplicate writes when flushing, but cassandra will just over-write 
			// on the second insert.
			FileData fd = new FileData(fileVersion.getPathId(), partialBlockNo, ByteBuffer.wrap(buffer, 0, index),
					fileVersion.getZus(),fileVersion.getVersion().getTime());
			fileDataMapper.save(fd);
		}

		this.fileVersion.setEncodedDataLength(getSize());
		fileVersionMapper.save(this.fileVersion);
	}

	public String toString() {
		return "OutputStream(file=" + fileVersion.getPathId() + ")";
	}

	// OutputStream overrides ----------------------------------------
	public void write(int datum) throws IOException {
		if (closed) {
			throw new IOException("Stream closed");
		} else {
			if (index == blockSize) {
				addBuffer();
			}

			// store the byte
			buffer[index++] = (byte) datum;
		}
	}

	public void write(byte[] data, int offset, int length) throws IOException {
		if (data == null) {
			throw new NullPointerException();
		} else if ((offset < 0) || ((offset + length) > data.length) || (length < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (closed) {
			throw new IOException("Stream closed");
		} else {
			if ((index + length) > blockSize) {
				int copyLength;

				do {
					if (index == blockSize) {
						addBuffer();
					}

					copyLength = blockSize - index;

					if (length < copyLength) {
						copyLength = length;
					}

					System.arraycopy(data, offset, buffer, index, copyLength);
					offset += copyLength;
					index += copyLength;
					length -= copyLength;
				} while (length > 0);
				flushCompletedBuffers();
			} else {
				// Copy in the subarray
				System.arraycopy(data, offset, buffer, index, length);
				index += length;
			}
		}
	}

	/**
	 * Create a new buffer and store the current one in linked list
	 */
	private synchronized void addBuffer() {
		buffers.addLast(new Block(size / blockSize, buffer));

		buffer = new byte[blockSize];
		size += index;
		index = 0;
	}
}