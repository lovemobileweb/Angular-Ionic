package zyber.server.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import zyber.server.dao.FileDataAccessor;
import zyber.server.dao.FileVersion;

/** Pretty much always use this class with a {@link BufferedReader}. */

public class ZyberFileInputStream extends InputStream {
	private final FileVersion fileVersion;
	private final int blockSize;

	private long filePosition;
	private final FileDataAccessor fdAccessor;
	/** Main constructor. */
	public ZyberFileInputStream(FileVersion path) {
		this.fileVersion = path;
		this.blockSize = path.getBlockSize();
		this.fdAccessor = fileVersion.getZus().accessor(FileDataAccessor.class);
	}

	/** See {@link InputStream#skip(long)}. */
	public long skip(long n) throws IOException {
		long availableToSkip = fileVersion.getEncodedDataLength() - filePosition;
		long toSkip = Math.min(n, availableToSkip);
		filePosition += toSkip;

		return toSkip;
	}

	@Override
	public int read() throws IOException {
		throw new IllegalStateException("Don't hit the database for asingle byte.");
	}

	public int read(final byte b[], final int offIn, final int lenIn) throws IOException {
		//System.out.println("Reading a block: "+filePosition+" for "+lenIn);
		int len = lenIn;
		int off = offIn;

		long startBlock = (filePosition + off) / blockSize;
		long endBlock = (filePosition + off + len-1) / blockSize;
		ResultSet listFD = fdAccessor.getBlocks(fileVersion.getPathId(), fileVersion.getVersion().getTime(), startBlock, endBlock);

		int bytesActuallyRead = 0;

		while (!listFD.isExhausted()) {
			Row r = listFD.one();

			ByteBuffer thisBlockData = r.getBytes("bytes");
			long thisBlockNo = r.getLong("block_number");
			int thisBlockStartingPos = (int) (filePosition - (thisBlockNo * blockSize));

			int bytesToCopy = Math.min(len, thisBlockData.remaining() - thisBlockStartingPos > len ? Integer.MAX_VALUE
					: (int) (thisBlockData.remaining() - thisBlockStartingPos));

			//System.out.println("...Did read a block: "+filePosition+" for "+bytesToCopy);
		
			if (bytesToCopy==0) continue;
			
			bytesActuallyRead += bytesToCopy;
			// System.out.println("@"+path.getPathId()+"#"+thisBlockNo+" got:
			// "+thisBlockStartingPos+" for "+bytesToCopy+" of bytes
			// "+thisBlockData.remaining());
			try {
				if (thisBlockStartingPos < 0)
					throw new IllegalStateException("Internal error - thisBlockStartinPos = " + thisBlockStartingPos);
				if (thisBlockStartingPos != 0)
					thisBlockData.position(thisBlockStartingPos);
				thisBlockData.get(b, off, bytesToCopy);
				filePosition = Math.max(filePosition, thisBlockNo * blockSize + thisBlockStartingPos + bytesToCopy);
				len = len - bytesToCopy;
				off = off + bytesToCopy;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		return bytesActuallyRead == 0 ? -1 : bytesActuallyRead;
	}

	@Override
	public int available() throws IOException {
		return (fileVersion.getEncodedDataLength() - filePosition >= Integer.MAX_VALUE) ? Integer.MAX_VALUE
				: (int) (fileVersion.getEncodedDataLength() - filePosition);
	}

}
