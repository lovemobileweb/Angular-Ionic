package zyber.server.dao;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import zyber.server.ZyberSession;
import zyber.server.ZyberUserSession;
import zyber.server.dao.mapping.IRequiresZyberUserSession;
import zyber.server.io.ZyberFileInputStream;
import zyber.server.io.ZyberFileOutputStream;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;

@Table(keyspace = "zyber_secure", name = "file_version")
public class FileVersion extends InTenant  implements IRequiresZyberUserSession {
	public  static final String ENC_PLAIN_TEXT = "PlainText";
	public  static final String ENC_GZIP = "GZip";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
	@PartitionKey(1)
	@Column(name = "path_id")
	UUID pathId;
	
	@ClusteringColumn(0)
	Date version;
	@Column(name = "block_size")
	int blockSize;
	@Column(name = "size")
	long size;
	@Column(name = "key_type")
	String keyType;
	//byte[]
	ByteBuffer key;
	@Column(name = "data_encoding")
	String dataEncoding;

	@Column(name = "encoded_data_length")
	long encodedDataLength;

	@Transient
	private ZyberUserSession zus;
	@Transient
	SecretKey cachedKey;


	public String getDataEncoding() {
		return dataEncoding;
	}

	public void setDataEncoding(String dataEncoding) {
		this.dataEncoding = dataEncoding;
	}


	public ZyberUserSession getZus() {
		return zus;
	}

	public void setZus(ZyberUserSession zus) {
		this.zus = zus;
	}

	public FileVersion withZus(ZyberUserSession zus) {
		setZus(zus);
		return this;
	}

	public FileVersion(UUID pathId, Date version, int blockSize, String keyType, String dataEncoding, byte[] key,
			long size, ZyberUserSession zus) {
		super();
		this.pathId = pathId;
		this.version = version;
		this.blockSize = blockSize;
		this.keyType = keyType;
		this.dataEncoding = dataEncoding;
		this.key = ByteBuffer.wrap(key);
		this.zus = zus;
	}

	public FileVersion(ZyberUserSession zus, UUID pathId, Date version) {
		this.zus = zus;
		this.pathId = pathId;
		this.version = version;
		this.blockSize = Integer.parseInt(zus.session.getConfigValue(ZyberSession.CONF_KEY__DEFAULT_BLOCK_SIZE));
		this.dataEncoding = zus.session.getConfigValue(ZyberSession.CONF_KEY__DEFAULT_CIPHER);
	}

	public FileVersion() {

	}

	@Override
	public int hashCode() {
		return pathId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileVersion other = (FileVersion) obj;
		if (pathId == null) {
			if (other.pathId != null)
				return false;
		} else if (!pathId.equals(other.pathId))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	public UUID getPathId() {
		return pathId;
	}

	public void setPathId(UUID pathId) {
		this.pathId = pathId;
	}


	public long getEncodedDataLength() {
		return encodedDataLength;
	}

	public void setEncodedDataLength(long encodedDataLength) {
		this.encodedDataLength = encodedDataLength;
	}

	public Date getVersion() {
		return version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getKeyType() {
		return keyType;
	}

	public void setKeyType(String keyType) {
		this.keyType = keyType;
	}

	public ByteBuffer getKey() {
		return key;
	}

	public void setKey(ByteBuffer key) {
		this.key = key;
	}

	static SecureRandom rand = new SecureRandom();
	private SecretKey getSecretKey() throws NoSuchAlgorithmException {
		if (cachedKey != null) return cachedKey;

		if (key == null) {
			if (keyType == null)
				keyType = zus.session.getConfigValue(ZyberSession.CONF_KEY__DEFAULT_KEY_TYPE);

			KeyGenerator kgen = KeyGenerator.getInstance(keyType);
			kgen.init(rand);
			kgen.init(Integer.parseInt(zus.session.getConfigValue(ZyberSession.CONF_KEY__DEFAULT_KEY_LENGTH)));
			cachedKey = kgen.generateKey();
			key = ByteBuffer.wrap(cachedKey.getEncoded());
			return cachedKey;
		} else {
			cachedKey = new SecretKeySpec(getKey().array(), getKeyType());
			return cachedKey;
		}
	}

	/** Decrypts the data as it reads the file. */
	public InputStream getInputStream() {
		return getInputStream(this.getBlockSize()*2);
	}
	/** The crypto stream reads the data in small chunks, triggering a massive number of database reads. By default we buffer 2 blocks with a bufferedinputstream. You can change the buffer size with the buffersize parameter if necessary.*/
	public InputStream getInputStream(int bufferSize) {
		try {
			if (dataEncoding == null) throw new IllegalStateException("dataEncoding not specified.");
			String enc = dataEncoding;
			boolean gZipped = false;
			InputStream ret = new ZyberFileInputStream(this);
			ret = new BufferedInputStream(ret, bufferSize);

			if (enc.startsWith(ENC_GZIP)) {
				enc = enc.substring(ENC_GZIP.length()+1);
				gZipped = true;
			}

			if (!enc.equals(ENC_PLAIN_TEXT)) {
				// Decrypt cipher
				SecretKey sKey = getSecretKey();
				Cipher decryptCipher = Cipher.getInstance(enc);
				IvParameterSpec ivParameterSpec = new IvParameterSpec("akdidksldnsildiw".getBytes()); //TODO: What should I do for an IV???

				decryptCipher.init(Cipher.DECRYPT_MODE, sKey, ivParameterSpec);

				ret = new CipherInputStream(ret, decryptCipher);	
				
				ret = new BufferedInputStream(ret, bufferSize);
				
			}
			
			if (gZipped) {
				ret = new GZIPInputStream(ret, bufferSize);
			}

			return ret;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new IllegalStateException(e);
		} catch (NoSuchPaddingException e) {
			throw new IllegalStateException(e);
		} catch (InvalidKeyException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}



	public class FileSizeTrackingOutputStream extends HasPathOutputStream {
		@Override
		public Path getPath() {
			return updatePathOnClose;
		}

		final Path updatePathOnClose;
		final OutputStream passThrough;
		long fileSize = 0;

		public FileSizeTrackingOutputStream(Path updatePathOnClose, OutputStream passThrough) {
			this.updatePathOnClose = updatePathOnClose;
			this.passThrough = passThrough;
		}
		public void write(int b) throws IOException {
			fileSize+=1;
			passThrough.write(b);
		}
		public void write(byte[] b) throws IOException {
			fileSize+=b.length;
			passThrough.write(b);
		}
		public void write(byte[] b, int off, int len) throws IOException {
			fileSize+=len;
			passThrough.write(b, off, len);
		}
		public void flush() throws IOException {
			passThrough.flush();
			updatePathOnClose.setCurrentVersion(version);
			updatePathOnClose.setSize(fileSize);
			setSize(fileSize);
			zus.mapper(Path.class).save(updatePathOnClose);
			zus.mapper(FileVersion.class).save(FileVersion.this);

		}
		public void close() throws IOException {
			passThrough.close();
			updatePathOnClose.setCurrentVersion(version);
			updatePathOnClose.setCurrentVersion(FileVersion.this);
			updatePathOnClose.setSize(fileSize);
			setSize(fileSize);
			zus.mapper(Path.class).save(updatePathOnClose);
			zus.mapper(FileVersion.class).save(FileVersion.this);
		}

	}

	/** Encryptes the data as it writes to the file, and updates the current file version at completion. */
	public HasPathOutputStream getOutputStream(Path path) {
		return getOutputStream(path, getBlockSize()*2);
	}
	public HasPathOutputStream getOutputStream(Path path, int bufferSize) {
		try {
			if (dataEncoding == null) throw new IllegalStateException("dataEncoding not specified.");
			String enc = dataEncoding;
			boolean gZipped = false;
			
			OutputStream ret = new ZyberFileOutputStream(this);

			ret = new BufferedOutputStream(ret, bufferSize);

			if (enc.startsWith(ENC_GZIP)) {
				gZipped = true;
				enc = enc.substring(ENC_GZIP.length()+1);				
			}			

			if (!enc.equals(ENC_PLAIN_TEXT)) {
				SecretKey sKey = getSecretKey();
				IvParameterSpec ivParameterSpec = new IvParameterSpec("akdidksldnsildiw".getBytes());

				// Encrypt cipher
				Cipher encryptCipher = Cipher.getInstance(enc);
				encryptCipher.init(Cipher.ENCRYPT_MODE, sKey, ivParameterSpec);

				ret = new CipherOutputStream(ret, encryptCipher);
			}
			if (gZipped) {
				ret = new GZIPOutputStream(ret, bufferSize, true);
			}
			return new FileSizeTrackingOutputStream(path, ret);

		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		} catch (NoSuchPaddingException e) {
			throw new IllegalStateException(e);
		} catch (InvalidKeyException e) {
			throw new IllegalStateException(e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}


}
