package data;

import java.util.LinkedList;
import java.util.List;

public class File {
	
	private final String path;
	private final long size;   // in bytes
	
	public File(String path, long size) {
		this.path = path;
		this.size = size;
	}
	
	public String getPath() {
		return path;
	}
	
	public long getSize() {
		return size;
	}
	
	public boolean isChunk() {
		return false;
	}
	
	public List<Chunk> split(long chunkSize) {
		List<Chunk> chunkList = new LinkedList<Chunk>();
		long numChunks = (long)Math.ceil((double)size / (double)chunkSize);
		for (int i = 0; i < numChunks; i++) {
			long startByte = i * chunkSize;
			long endByte = Math.min( (i+1) * chunkSize - 1, this.size - 1);
			chunkList.add(new Chunk(this.path, endByte - startByte + 1, startByte, endByte));
									// last byte is included
			//System.out.println("*************** FILE CLASS: SPLIT FUNCTION: ADDING CHUNK " + i + " of " + numChunks + " For current File, Start Byte:_" + startByte + ", End Byte:_" + endByte);
		}
		return chunkList;
	}

	//Split by Parallelism
	public List<Chunk> splitByPLevel(int pLevel) {
		List<Chunk> chunkList = new LinkedList<Chunk>();
		////Note this doesn't return a decimal returns just the integer part
		//Example: size = 7 Bytes, Parallelism = 3, 7/3 = 2 and 1/3 so returns 2
		long chunkSize = size / pLevel;
		long numChunks = (long)Math.ceil((double)size / (double)chunkSize);
		for (int i = 0; i < numChunks; i++) {
			long startByte = i * chunkSize;
			long endByte = Math.min( (i+1) * chunkSize - 1, this.size - 1);
			chunkList.add(new Chunk(this.path, endByte - startByte + 1, startByte, endByte));
			// last byte is included
			//System.out.println("*************** FILE CLASS: SPLIT FUNCTION: ADDING CHUNK " + i + " of " + numChunks + " For current File, Start Byte:_" + startByte + ", End Byte:_" + endByte);
		}
		return chunkList;
	}

}