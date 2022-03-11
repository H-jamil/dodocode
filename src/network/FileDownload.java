package network;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.lang.Long;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpPipeliningClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.Header;

import data.Chunk;
import data.EmptyDatasetException;
import data.File;

public class FileDownload implements Runnable {
	
	private Transfer transfer;
	//private CloseableHttpPipeliningClient[] httpClients;
	private List<CloseableHttpPipeliningClient> httpClients;
	
	public FileDownload(Transfer transfer) {
		this.transfer = transfer;
		int pLevel = transfer.getParallelismLevel();
		//Plevel = Parallelism level
		//Create an arrayList of Http Pipelining Clients based on the parallelism level
		httpClients = new ArrayList<CloseableHttpPipeliningClient>();
		for (int i = 0; i < pLevel; i++) {
			httpClients.add(HttpAsyncClients.createPipelining());
			httpClients.get(i).start();
			//or httpClients.get(httpClients.size()-1).start(); 
		}

		/*
		httpClients = new CloseableHttpPipeliningClient[transfer.getPLevel()];
		for (int i = 0; i < transfer.getPLevel(); i++) {
			httpClients[i] = HttpAsyncClients.createPipelining();
			httpClients[i].start();
		}
		*/
	}

	@Override
	public void run() {
		
		while (true) {
			
			// Check if at least one channel (This Channel) should be closed (This is a concurrent channel)
			long channelsToClose = transfer.getChannelsToClose().getAndSet(0);
			if (channelsToClose > 0) {
				System.out.println("FileDownload: Closing channel for dataset " + transfer.getDataset().getName());
				transfer.getChannelsToClose().set(channelsToClose - 1); //Closing this concurrent channel, so decrement by one meaning closing this concurrent channel and the associated parallel channels
				try {
					//Closing the parallel channels associated with this concurrent channel (FileDown Load)
					for (CloseableHttpPipeliningClient httpClient: httpClients) {
						httpClient.close();
					}
				} catch (IOException e) {
					System.out.println("FileDownload: Something wrong while closing the http client");
				}
				return;
			}

			/*
			Get latest parallelism level
			If parallelism level is greater than the size of array list
  				Increase parallelism level – add parallel channels
			Else if parallelism level is less than the size of array list
  				Decrease parallelism level – close parallel channels
			*/

			//Get Parallelism value
			int pLevel = transfer.getParallelismLevel();
			//int deltaPlevel;
			if (pLevel > httpClients.size()){
				//Add more Parallel Channels
				int deltaPlevel = pLevel - httpClients.size();
				for (int i = 0; i < deltaPlevel; i++) {
					//Add channel to the end of the list
					httpClients.add(HttpAsyncClients.createPipelining());
					//Start the channel (last element in list)
					httpClients.get(httpClients.size()-1).start();
				}
			} else {
				try {
					if (pLevel < httpClients.size()) {
						//Close & Remove Parallel Channels
						int deltaPlevel = httpClients.size() - pLevel;
						for (int i = 0; i < deltaPlevel; i++) {
							//Remove channel at beginning of list
							httpClients.get(0).close();
							httpClients.remove(0);
						}
					}
				}catch (IOException e) {
					System.out.println("FileDownload: Something wrong while closing the http client");
				}
			}

			
			// Get ppLevel files from list, where pp = pipelining level
			List<File> fileList;
			try {
				//int ppLevel = transfer.getPPLevel(); //Pipelining
				int ppLevel = transfer.getPipelineLevel();
				//Since a file can be a chunk of a single file it is possible to request
				//one chunk after another chunk of the same file using pipelining
				//Meaning I can send chunks of a single file, but from the same dataset
				fileList = transfer.getDataset().removeFile(ppLevel);
			} catch (EmptyDatasetException e) {
				try {
					System.out.println("FileDownload: No more files, closing channels...");
					for (CloseableHttpPipeliningClient httpClient: httpClients) {
						httpClient.close();
					}
				} catch (IOException e2) {
					System.out.println("Something wrong while closing the http client");
				}
				return;
			}
			CountDownLatch remainingRequests;
			
			
			// If parallelism is 1, send request directly,
			// without creating more Runnables

			//if (transfer.getPLevel() == 1) {
			if (pLevel == 1) {
				// Prepare array of HTTP requests based on Pipeline value (pp)
				//Here File is just an object with the filename including filepath with the size of the file to get from server
				HttpGet[] httpRequests = new HttpGet[fileList.size()];
				for (int i = 0; i < fileList.size(); i++) {
					File f = fileList.get(i);
					//System.out.println("File size: " + f.getSize());
					HttpGet httpReq = new HttpGet(f.getPath());
					if (f.isChunk()) {
						Chunk c = (Chunk)f;
						httpReq.addHeader("Range", "bytes=" + String.valueOf(c.getStartByte()) + "-" + String.valueOf(c.getEndByte()));
						//System.out.println("*******FILE DOWNLOAD: HTTP REQUEST OBJECT (CHUNK " + i + " of " + fileList.size() +"): BYTES: " + String.valueOf(c.getStartByte()) + " - " + String.valueOf(c.getEndByte()));
					}
					httpRequests[i] = httpReq;

				}

				boolean done = false;
				while (!done) {
					try {
						// Send pipelined HTTP requests

						Future<List<HttpResponse>> future = httpClients.get(0).execute(transfer.getHttpServer(),
								Arrays.<HttpRequest>asList(httpRequests), null);

						//How do you know all files were received within 60 seconds
						//File Number based on the pipeline value
						future.get(60, TimeUnit.SECONDS);
						//List<HttpResponse> responses = future.get(60, TimeUnit.SECONDS);

						done = true;
						//System.out.println("*******FILE DOWNLOAD: THIS CONCURRENT CHANNEL FINISHED DOWNLOADING THE REQUESTED FILES, NEXT I WILL CHECK TO SEE IF THERE ARE ANY MORE FILE REQUESTS, IF NOT, THE CHANNEL WILL CLOSE");

						//LAR /////////////////////////////////////////////////////
						/*
						long totalBytesReceived = 0;
						int theCounter = 1;
						int myCounter = 1;
						//Get Content Length of Each Response

						for (HttpResponse hr:responses){


							Header[] theHeaders = hr.getAllHeaders();
							System.out.println("The number of headers in this HTTP Response Message # " + myCounter + " is " + theHeaders.length);
							for (int i = 0; i < theHeaders.length; i++){
								Header theHeader = theHeaders[i];
								String headerName = theHeader.getName();
								String headerValue = theHeader.getValue();
								System.out.println("*****Header " + i + ": <Name: " + headerName + ", Value: " + headerValue + ">");

							}
							myCounter++;




							StatusLine statLine = hr.getStatusLine();
							System.out.println("**********FILE_DOWN_LOAD: RESPONSE: STATUS LINE: STATUS CODE: " + statLine.getStatusCode() + ", REASON: " + statLine.getReasonPhrase());
							Header header = hr.getFirstHeader("Content-Length"); //Note sure of case
							String contentLengthInBytesString = header.getValue(); // or header.getElements
							//long contentLengthInBytes = Long.parseUnsignedLong(contentLengthInBytesString);
							long contentLengthInBytes = Long.parseUnsignedLong(header.getValue());
							totalBytesReceived += contentLengthInBytes;
							System.out.println("**********FILE_DOWN_LOAD: HTTP Response Content-Length Header Received: Value = " + header.getValue() + ", Total Bytes Received = " + totalBytesReceived);



							if (header != null) {
								String acontentLengthInBytesString = header.getValue(); // or header.getElements
								//System.out.println("content-Length Bytes as a String = " + acontentLengthInBytesString);
								if (contentLengthInBytesString != null ){
									//System.out.println("contentLengthInBytesString NOT EQUAL to NULL ");
									//long contentLengthInBytes = Long.getLong(contentLengthInBytesString).longValue();//
									long acontentLengthInBytes = Long.parseUnsignedLong(acontentLengthInBytesString);
									//System.out.println("content-Length Bytes as a Long = " + acontentLengthInBytes);
									totalBytesReceived += contentLengthInBytes;
									System.out.println("Total Bytes Received = " + totalBytesReceived);
								}
							}else {
								System.out.println("Content-Length Header is Null");
							}


						}
						*/


						//LAR /////////////////////////////////////////////////////

					} catch (Exception e) {
						try {
							System.out.println("Timeout or Error, restarting client");
							e.printStackTrace();
							httpClients.get(0).close();
							httpClients.set(0,HttpAsyncClients.createPipelining());
							httpClients.get(0).start();
							/*
							httpClients[0]
							httpClients[0] = HttpAsyncClients.createPipelining();
							httpClients[0].start();
							*/
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				} //End While

			} // if
			
			// If parallelism > 1
			else {
				//LAR COMMENTS BELOW FOR PARALLEL TRANSFERS GREATER THAN 1
				// For every file, split it in chunks and
				// assign each chunk to a different parallel channel
				//HttpRequest[Parallel Channels][Pipeline value - # of files]
				//Each file split by the number of parallel channels
				//HttpRequest[2 parallel channel ][4 files]
				/*
				   new HttpRequest[parallelism, number of files]
				   new HttpRequest[2,4] 2 x 4 array, but isn't below a 2 x 8 array
				   [row, column] 2 rows and 4 columns, but below is a 8 x 2 array
				   //HttpRequest[0][0] - [row 0, col 0] = Parallel channel 0, File 0, block 0
				   //HttpRequest[1][0] - [row 1, col 0] = Parallel channel 1, File 0, block 1
				   //HttpRequest[0][1] - [row 0, col 1] = Parallel channel 0, File 1, block 0
				   //HttpRequest[1][1] - [row 1, col 1] = Parallel channel 1, File 1, block 1
				   //HttpRequest[0][2] - [row 0, col 2] = Parallel channel 0, File 2, block 0
				   //HttpRequest[1][2] - [row 1, col 2] = Parallel channel 1, File 2, block 1
				   //HttpRequest[0][3] - [row 0, col 3] = Parallel channel 0, File 3, block 0
				   //HttpRequest[1][3] - [row 1, col 3] = Parallel channel 1, File 3, block 1
				   //-------------------------------------------------------------------------
				   //HttpRequest[0][0] - [row 0, col 0] = Parallel channel 0, File 0, block 0
				   //HttpRequest[0][1] - [row 0, col 1] = Parallel channel 0, File 1, block 0
				   //HttpRequest[0][2] - [row 0, col 2] = Parallel channel 0, File 2, block 0
				   //HttpRequest[0][3] - [row 0, col 3]] = Parallel channel 0, File 3, block 0
				   //HttpRequest[1][0] - [row 1, col 0] = Parallel channel 1, File 0, block 1
				   //HttpRequest[1][1] - [row 1, col 1] = Parallel channel 1, File 1, block 1
				   //HttpRequest[1][2] - [row 2, col 2] = Parallel channel 1, File 2, block 1
				   //HttpRequest[1][3] - [row 3, col 3] = Parallel channel 1, File 3, block 1

				   HttpRequest[1][3] this is the position
				   This is 2 rows and 4 columns
				 */
				//HttpRequest[][] httpRequests = new HttpRequest[transfer.getPLevel()][fileList.size()];
				HttpRequest[][] httpRequests = new HttpRequest[pLevel][fileList.size()];
				for (int i = 0; i < fileList.size(); i++) {
					File f = fileList.get(i);
					//Get length of chunk that should be sent through each parallel channel
					//long chunkLength = Math.round( (double)f.getSize() / (double)transfer.getPLevel());
					long chunkLength = Math.round( (double)f.getSize() / (double)pLevel);
					List<Chunk> chunkList = f.split(chunkLength);
					//for (int j = 0; j < transfer.getPLevel(); j++) {
					for (int j = 0; j < pLevel; j++) {
						// Create HttpRequest and add it to array
						Chunk c = chunkList.get(j);
						HttpRequest httpReq = new HttpGet(c.getPath());
						httpReq.addHeader("Range", "bytes=" + String.valueOf(c.getStartByte()) + "-" + String.valueOf(c.getEndByte()));

						httpRequests[j][i] = httpReq;
					}
				}
				
   
				// Now time to send the HTTP requests
				remainingRequests = new CountDownLatch(pLevel);
				//remainingRequests = new CountDownLatch(httpClients.size());
				//remainingRequests = new CountDownLatch(transfer.getPLevel());
				//Or Put the asynchronous part in a runnable to be ran by a thread
				//But this defeats the purpose of having an asynchronous, but I can
				//Still test out. Want to submit each parallel channel to thread pool
				//To see if that will solve the memory problem with too many concurrent channels
				//And parallelism.
				//for (int i = 0; i < transfer.getPLevel(); i++) {
				//The updated parallelism value is denoted by the size of the ArrayList
				//for (int i = 0; i < httpClients.size(); i++) {
				//counter = 0;
				for (int i = 0; i < pLevel; i++) {
					try {
						//httpClients[i].execute(transfer.getHttpServer(), Arrays.<HttpRequest>asList(httpRequests[i]), new FutureCallback<List<HttpResponse>>() {
						httpClients.get(i).execute(transfer.getHttpServer(), Arrays.<HttpRequest>asList(httpRequests[i]), new FutureCallback<List<HttpResponse>>() {
		                    @Override
		                    public void completed(List<HttpResponse> responses) {

		                    		remainingRequests.countDown();
		                        	//System.out.println("Received responses");
		                        	responses.clear();
		                    }
	
		                    @Override
		                    public void failed(final Exception ex) {
		                    		remainingRequests.countDown();
		                        System.err.println("Request failed");
		                    }
	
		                    @Override
		                    public void cancelled() {
		                    		remainingRequests.countDown();
		                        System.out.println("Request cancelled");
		                    }

						});
					} catch (Exception e) {
						System.out.println("SOMETHING WENT WRONG WITH EXECUTE");
						System.exit(0);
					}

				} // for loop
				// Wait for all requests to be completed
				boolean interrupted;
				do {
					try {
						interrupted = false;
						//Ensure all parallel channels received responses
						//System.out.println("FileDownLoad: Before remainingRequests.await(), remainingRequests countDown latch = " + remainingRequests.getCount());
						remainingRequests.await();
					} catch (InterruptedException e) {
						interrupted = true;
						System.out.println("File downloader " + transfer.getName() + " : interrupted while waiting for all downloads to be completed.");
					}
				} while (interrupted);
			} // else

			// Signal that ppLevel files have been transferred
			//COMMENTED OUT JUST FOR A MOMENT, HAVE TO UNCOMMENT

			for (File f: fileList) {
				transfer.signalTransferredFile(f);
			}



		} // while(true)
	} // run()
	
	
	static class MyRequestProducer extends BasicAsyncRequestProducer {

        private final HttpRequest request;

        MyRequestProducer(final HttpHost target, final HttpRequest request) {
            super(target, request);
            this.request = request;
        }

        @Override
        public void requestCompleted(final HttpContext context) {
            super.requestCompleted(context);
//            System.out.println();
//            System.out.println("Request sent: " + this.request.getRequestLine());
//            System.out.println("=================================================");
        }
    }

    static class MyResponseConsumer extends AsyncCharConsumer<Boolean> {

        private final HttpRequest request;

        MyResponseConsumer(final HttpRequest request) {
            this.request = request;
        }

        @Override
        protected void onResponseReceived(final HttpResponse response) {
            System.out.println();
            System.out.println("Response received: " + response.getStatusLine() + " -> " + this.request.getRequestLine());
            System.out.println("=================================================");
        }

        @Override
        protected void onCharReceived(final CharBuffer buf, final IOControl ioctrl) throws IOException {
        		System.out.println("Elements remaining: " + buf.remaining());
        		long count = 0;
            while (buf.hasRemaining()) {
            		count++;
            		buf.get();
//                System.out.print(buf.get());
            }
            System.out.println("Number of bytes received: " + count);
        }

        @Override
        protected void releaseResources() {
        }

        @Override
        protected Boolean buildResult(final HttpContext context) {
//            System.out.println();
//            System.out.println("=================================================");
//            System.out.println();
            return Boolean.TRUE;
        }

    }

	
}
