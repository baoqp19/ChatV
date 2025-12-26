package data;

import tags.Tags;
import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a file data chunk for transmission in VKU Chat.
 * Java 21 compatible using {@code @Serial} annotation.
 */
public class DataFile implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public byte[] data;

	/**
	 * Creates a DataFile with default size from Tags.MAX_MSG_SIZE
	 */
	public DataFile() {
		this.data = new byte[Tags.MAX_MSG_SIZE];
	}

	/**
	 * Creates a DataFile with specified size
	 * 
	 * @param size Size of data buffer in bytes
	 */
	public DataFile(int size) {
		this.data = new byte[size];
	}

	/**
	 * Gets the actual data
	 * 
	 * @return Byte array containing the file data
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Sets the data
	 * 
	 * @param data Byte array to set
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * Gets the size of the data
	 * 
	 * @return Length of the data array
	 */
	public int getSize() {
		return data != null ? data.length : 0;
	}
}
