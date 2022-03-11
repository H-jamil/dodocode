package data;

@SuppressWarnings("serial")
public class EmptyDatasetException extends Exception {

	private final String datasetName;
	
	public EmptyDatasetException(String datasetName) {
		this.datasetName = datasetName;
	}
	
	public String getDatasetName() {
		return datasetName;
	}
}
