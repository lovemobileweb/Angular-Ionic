package zyber.server.dao;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;

import services.DBTests;
import zyber.server.CassandraMapperDelegate;
import zyber.server.ZyberSession;
import zyber.server.ZyberTestSession;
import zyber.server.ZyberUserSession;

import com.datastax.driver.mapping.Result;

/**
 * A test class for {@link ZyberFileOutputStreamTest}
 */
public class ZyberFileOutputStreamTest extends TestCase {
	static Path createFile(Path p, String name, String body) throws IOException {
		Path ret = p.createFile(name);
		try (java.io.OutputStream os = ret.getOutputStream()) {
			os.write(body.getBytes());			
		}
		return ret;
	}
	
	public byte[] createBytesForTesting(long seed, int n) {
		//return LoremIpsum.getInstance().getWords(n).substring(0, n).getBytes();

		Random r = new Random(seed + n);
		byte[] bytes = new byte[n];
		r.nextBytes(bytes);
		return bytes;
	}

	static boolean isEclipse() {
		Throwable t = new Throwable();
		StackTraceElement[] trace = t.getStackTrace();
		return trace[trace.length - 1].getClassName().startsWith("org.eclipse");
	}

	/** Some of these tests are long running, so we skip them most of the time (quick test) */
	//	static boolean quickTest = isEclipse() ? false : true;
	static boolean quickTest = true;

	public void assertEquals(byte[] a, byte[] b) {
		assertEquals("Arrays not equal length.", a.length, b.length);
		for (int i = 0; i < a.length; i++) {
			assertEquals("Array value at index " + i + " not equal", a[i], b[i]);
		}

	}

	void testReadingBy(Path file, byte[] readBy, byte correctData[], boolean buffered) throws IOException {
		int pos = 0;
		try (@SuppressWarnings("resource")
		InputStream is = buffered ? 
				new BufferedInputStream(file.getInputStream(), correctData.length / 3) :
					file.getInputStream()) {
			int didRead;
			while ((didRead = is.read(readBy)) > 0) {
				// if (correctData.length-pos < 2 && readBy.length == 2)
				// System.out.println("Near end");

				try {
					assertEquals(Arrays.copyOfRange(readBy, 0, didRead),
							Arrays.copyOfRange(correctData, pos, pos + didRead));
				} catch (AssertionFailedError e) {
					throw new IllegalStateException("At parent index " + pos, e);
				}

				pos += didRead;
			}
			assertEquals(pos, correctData.length);
		}
	}

	public void testPerformance() throws Exception {
		ZyberUserSession zus = ZyberTestSession.getTestUserSession(DBTests.testingTenantId(),"test_speed");

		createFile(zus.getRootPath(),"Warmup.txt", "get everything loaded.");

		int[] testSizes = quickTest ? new int[] { 10 } : new int[] { 1, 10, 50, 100, 250, 4000 };
		int[] blockSizes = quickTest ? new int[] {1*1024*1024} : new int[] {1*1024*1024,100*1024,1000};

	
		for (int blockSize : blockSizes) {
			for (String cipher : new String[] { FileVersion.ENC_PLAIN_TEXT, "AES/CBC/PKCS5Padding", FileVersion.ENC_GZIP+"/AES/CBC/PKCS5Padding" }) {
				for (int writeSizeMB : testSizes) {
					int writeSize = writeSizeMB * 1024 * 1024;
					byte[] testData = createBytesForTesting(1231, 1024 * 1024);

					if (blockSize < 10000 && writeSize > 5000000) continue;
					
					long startTime = System.currentTimeMillis();

					Path testFile = zus.getRootPath().createChild(this.getClass().getName() + "-" + cipher+"-"+writeSize, PathType.File);
					FileVersion testFileV = testFile.getCurrentFileVersion();
					assertNull(testFileV); // No current version.
					testFileV = new FileVersion(zus, testFile.getPathId(), new Date());
					testFileV.setBlockSize(blockSize);
					testFileV.setDataEncoding(cipher);
					OutputStream out = new BufferedOutputStream(testFileV.getOutputStream(testFile), 50 * 1024 * 1024);
					for (int i = 0; i < writeSizeMB; i++) {
						out.write(testData);
					}
					out.close();
					assertNotNull(testFile.getCurrentFileVersion());
					long elapsed = System.currentTimeMillis() - startTime;
					System.out.println("Write test sample file with " + NumberFormat.getIntegerInstance().format(writeSizeMB)
							+ " MB in " + (elapsed / 1000.0) + " seconds for "
							+ NumberFormat.getIntegerInstance().format(writeSize / (elapsed / 1000.0))
							+ " bytes/sec using a block size of "+ NumberFormat.getIntegerInstance().format(testFile.getCurrentFileVersion().getBlockSize())
							+" using cipher "+cipher);
//					if (writeSizeMB > 5 && blockSize > 1000000)
//						assertTrue("For files over 1 MB, the speed should be over 20 MB/sec",
//								writeSize / (elapsed / 1000.0) > 20000000);
				}

				for (int writeSizeMB : testSizes) {
					int writeSize = writeSizeMB * 1024 * 1024;
					long startTime = System.currentTimeMillis();

					
					if (blockSize < 10000 && writeSize > 5000000) continue;

					Path testFile = zus.getRootPath().getFirstChild(this.getClass().getName() + "-" + cipher+"-"+writeSize);
					try (InputStream in = testFile.getInputStream()) {
						int readChunk = 50 * 1024 * 1024;
						int totalRead = 0;
						int didRead = 0;
						byte[] readBuffer = new byte[readChunk];
						while ((didRead = in.read(readBuffer)) > 0) {
							totalRead += didRead;
						}
						assertEquals(writeSize,  totalRead);
					}
					long elapsed = System.currentTimeMillis() - startTime;
					System.out.println("Read test sample file with " + NumberFormat.getIntegerInstance().format(writeSizeMB)
							+ " MB in " + (elapsed / 1000.0) + " seconds for "
							+ NumberFormat.getIntegerInstance().format(writeSize / (elapsed / 1000.0)) + " bytes/sec"
							+" using a block size of "+ NumberFormat.getIntegerInstance().format(testFile.getCurrentFileVersion().getBlockSize())
							+" using cipher "+cipher);
//					if (writeSizeMB > 1)
//						assertTrue("For files over 1 MB, the speed should be over 20 MB/sec",
//								writeSize / (elapsed / 1000.0) > 20000000);
				}
			}
		}
	}

	public void testSimpleSmallReadWrite() throws Exception {
		ZyberUserSession zus = ZyberTestSession.getTestUserSession(DBTests.testingTenantId(),"test_files");

		String simpleTest = "Hi there kitty cat.";
		createFile(zus.getRootPath(),"test-enc", simpleTest);

		Path newHandleToCreatedFile = zus.getRootPath().getFirstChild("test-enc");
		assertNotNull(newHandleToCreatedFile);
		assertNotNull("Must have a version here, as we just wrote above?", newHandleToCreatedFile.getCurrentFileVersion());
		String didRead = IOUtils.toString(newHandleToCreatedFile.getCurrentFileVersion().getInputStream());
		assertEquals(didRead,simpleTest);

	}

	public void testSimpleSmallReadWriteWithMultipleVersions() throws Exception {
		ZyberUserSession zus = ZyberTestSession.getTestUserSession(DBTests.testingTenantId(),"test_files");

		ZyberSession sessionForTesting = ZyberTestSession.getSessionForTesting();
		FileVersionAccessor accessor = zus.accessor(FileVersionAccessor.class);
		FileDataAccessor dataAccessor = zus.accessor(FileDataAccessor.class);
		dataAccessor.deleteAll();

		String simpleTest = "v1";
		createFile(zus.getRootPath(),"test-enc", simpleTest);

		Path firstChild = zus.getRootPath().getFirstChild("test-enc");

		Thread.sleep(1);
		FileVersion fileVersion = new FileVersion(zus, firstChild.getPathId(), new Date());
		CassandraMapperDelegate<FileVersion> mapper = zus.mapper(FileVersion.class);
		mapper.save(fileVersion);

		OutputStream outputStream = fileVersion.getOutputStream(firstChild);
		outputStream.write("v12".getBytes());
		outputStream.close();

		Thread.sleep(1);

		Result<FileVersion> alVersions = accessor.getAllVersions(firstChild.getPathId());
		FileVersion one = alVersions.one();
		FileVersion two = alVersions.one();
		one.setZus(zus);
		two.setZus(zus);
		assertEquals("v1",IOUtils.toString(one.getInputStream()));
		assertEquals("v12",IOUtils.toString(two.getInputStream()));

	}

	public void testBasicReadAndWrite() throws Exception {
		ZyberUserSession zus = ZyberTestSession.getTestUserSession(DBTests.testingTenantId(), "test_files");

		byte[][] tests = quickTest ? 
				new byte[][] { createBytesForTesting(1231, 1999), createBytesForTesting(1231, 2000),createBytesForTesting(1231, 2001) }
		:
			new byte[][] { 
					createBytesForTesting(1231, 10), 
					createBytesForTesting(1231, 511), createBytesForTesting(1231, 512),createBytesForTesting(1231, 513), // cipher stream reads 512 k at a time.
					createBytesForTesting(1231, 1997), createBytesForTesting(1231, 1998),
					createBytesForTesting(1231, 1999), createBytesForTesting(1231, 2000),
					createBytesForTesting(1231, 2001), createBytesForTesting(1231, 2002),
					createBytesForTesting(1231, 2003), createBytesForTesting(1231, 2004) };

					for (boolean buffered : new boolean[] { false, true }) {
						for (byte[] testData : tests) {
							for (int writeSize : new int[] { 1, 2, 3, 4, 7, testData.length }) {
								for (String cipher : new String[] { FileVersion.ENC_PLAIN_TEXT, "AES/CBC/PKCS5Padding", FileVersion.ENC_GZIP+"/AES/CBC/PKCS5Padding" }) {
									long startTime = System.currentTimeMillis();

									Path testFile = zus.getRootPath().createChild(
											this.getClass().getName() + "-" + writeSize + "-" + testData.length, PathType.File);

									FileVersion testFileV = testFile.getCurrentFileVersion();
									assertNull(testFileV); // No current version.
									testFileV = new FileVersion(zus, testFile.getPathId(), new Date());
									testFileV.setBlockSize(100);
									testFileV.setDataEncoding(cipher);


									try (OutputStream s = buffered ? 
											new BufferedOutputStream(testFileV.getOutputStream(testFile), testData.length / 3) :
												testFileV.getOutputStream(testFile)) {
										for (int i = 0; i < testData.length; i += writeSize) {
											int bytesToWrite = Math.min(writeSize, testData.length - i);
											s.write(Arrays.copyOfRange(testData, i, i + bytesToWrite));
										}
									}

									try (InputStream is = testFile.getInputStream()) {
										assertEquals(testData, IOUtils.toByteArray(is));
									}

									// Some read tests.
									testReadingBy(testFile, new byte[writeSize], testData, buffered);

									long elapsed = System.currentTimeMillis() - startTime;
									System.out.println("Tested sample file with " + testData.length + " bytes, reading/writing at "
											+ writeSize + " size chunks" + (buffered ? " using buffers" : "") + " with cipher "+cipher+" in "
											+ (elapsed / 1000.0) + " seconds for " + (testData.length / writeSize * 2 + 2)
											+ " database operations.");

								}
							}
							Path emptyFile = zus.getRootPath().createChild("Can I Read a 0 byte file.txt", PathType.File);
							assertEquals(0, emptyFile.getInputStream().available());

							int didReadFromEmptyFile = emptyFile.getInputStream().read(new byte[1]);
							assertEquals(-1, didReadFromEmptyFile);
						}
					}
	}

}