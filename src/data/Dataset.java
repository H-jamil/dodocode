package data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class Dataset {
	
	private final String name;
	private List<File> fileList;
	private long size;
	
	public Dataset(String name) {
		this.name = name;
		this.fileList = new LinkedList<File>();
		this.size = 0;
	}

	public String getName() {
		return name;
	}
	
	public long getSize() {
		return size;
	}
	
	public synchronized int getFileCount() {
		return fileList.size();
	}

	public synchronized List getFileList(){
		return fileList;
	}
	
	/*
	 * printFileList(): print a list of the files in the dataset
	 */
	public synchronized void printFileList() {
		System.out.println("Dataset " + name + ", total size = " + size);
		for (File f: fileList) {
			System.out.print("\t* File " + f.getPath() + ", size = " + f.getSize());
			if (f.isChunk()) {
				Chunk c = (Chunk) f;
				System.out.print(", start = " + c.getStartByte() + ", end = " + c.getEndByte());
			}
			System.out.println();
		}
		System.out.println();
	}
	
	/*
	 * split(chunkSize): split a file in chunks (Note: this doesn't split a data set into 2 datasets
	 * -- A single file now becomes a list of chunks
	 * -- The list of chunks is added to the chunk list
	 * -- a Single chunk list is return, but this is still a single Dataset
	 */
	public synchronized void split(long chunkSize) {
		//FileList.size returns the number of files in the list
		//Size is the total size in bytes of the data set
		if ((double)size / (double)fileList.size() <= chunkSize) {
			System.out.println("******Dataset: Split function: Dataset_size_(" + size + ") / fileList_size_(" + fileList.size() + ") <= chunkSize_(" + chunkSize + ") RETURNING FROM FUNCTION");
			return;
		}
		System.out.println("******Dataset: Split function: Dataset_size_(" + size + ") / fileList_size_(" + fileList.size() + ") > chunkSize_(" + chunkSize + ") SPLITING EACH FILE WITHIN THIS DATASET INTO CHUNKS, EACH CHUNK WILL BE CONSIDERED A FILE");
		//System.out.println("Splitting dataset " + this.name);
		List<File> splitFileList = new LinkedList<File>();
		//int counter_LAR = 0; //LAR JUNE 14, 2020
		for (File f: fileList) {
			//System.out.println("************* DATASET CLASS: SPLITTING FILE " + counter_LAR ); //LAR, JUNE 14, 2020
			splitFileList.addAll(f.split(chunkSize));
			//counter_LAR++; //LAR JUNE 14, 2020
		}

		fileList = splitFileList; //List of chunks instead of List of Files, but chunk class extends file class
		//Although file list is a list of chunks, it's really a list of files, since Chunk class is a subclass of File Class
		//File class is the super class and Chunk class is the sub class
	}

	/*
 * split(chunkSize): split a file in chunks (Note: this doesn't split a data set into 2 datasets
 * -- A single file now becomes a list of chunks
 * -- The list of chunks is added to the chunk list
 * -- a Single chunk list is return, but this is still a single Dataset
 */
	public synchronized void splitByParallelism(int pLevel) {
		System.out.println("******Dataset: Split by parallelism function: Dataset_size_(" + size + ") / fileList_size_(" + fileList.size() + ") > chunkSize_(" + pLevel + ") SPLITING EACH FILE WITHIN THIS DATASET INTO CHUNKS, EACH CHUNK WILL BE CONSIDERED A FILE");
		//System.out.println("Splitting dataset " + this.name);
		List<File> splitFileList = new LinkedList<File>();
		for (File f: fileList) {
			splitFileList.addAll(f.splitByPLevel(pLevel));
			//counter_LAR++; //LAR JUNE 14, 2020
		}

		fileList = splitFileList; //List of chunks instead of List of Files, but chunk class extends file class
		//Although file list is a list of chunks, it's really a list of files, since Chunk class is a subclass of File Class
		//File class is the super class and Chunk class is the sub class
	}

	
	/*
	 * removeFile(): remove a file from the dataset
	 */
	public synchronized File removeFile() throws EmptyDatasetException {
		if (fileList.isEmpty()) {
			throw new EmptyDatasetException(name);
		}
		else {
			File f = fileList.remove(0);
			this.size -= f.getSize();
			return f;
		}
	}
	
	/*
	 * removeFile(numFiles): remove multiple files from the dataset
	 * Does this remove files or chunks from the list
	 */
	public synchronized List<File> removeFile(int numFiles) throws EmptyDatasetException {
		// returns up to numFiles, but less if there are fewer files in the dataset
		if (fileList.isEmpty()) {
			throw new EmptyDatasetException(name);
		}
		else {
			List<File> fileSubList = new LinkedList<File>();
			for (int i = 0; i < numFiles; i++) {
				File f = fileList.remove(0);
				this.size -= f.getSize(); //DataSetSize - file.size
				fileSubList.add(f); //
				if (fileList.isEmpty()) {
					return fileSubList;
				}
			}
			return fileSubList;
		}
	}
	
	/*
	 * readFromFile(path, fileCount): read dataset from file
	 */
	public synchronized void readFromFile(String path, int fileCount) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			for (int i = 0; i < fileCount; i++){
				String line = br.readLine();
				if (line != null ) {
					//System.out.println("*********DataSet: READ LINE: " + line + " ************");
					StringTokenizer st = new StringTokenizer(line);
					long fileSize = Long.parseLong(st.nextToken());
					this.size += fileSize;
					this.fileList.add(new File("/" + this.name + "/" + String.valueOf(i), fileSize));
				}
			}
			br.close();
		}
		catch(NullPointerException e)
		{
			System.out.print("NullPointerException Caught");
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			System.out.println("Cannot find the dataset file.");
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while reading the dataset file.");
			e.printStackTrace();
		}
	}
}
