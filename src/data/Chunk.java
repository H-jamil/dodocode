package data;

public class Chunk extends File {

	private final long startByte;
	private final long endByte;   // last byte is included
	
	public Chunk(String fileName, long size, long startByte, long endByte) {
		super(fileName, size);
		this.startByte = startByte;
		this.endByte = endByte;
	}

	public long getStartByte() {
		return startByte;
	}

	public long getEndByte() {
		return endByte;
	}
	
	public boolean isChunk() {
		return true;
	}
}
