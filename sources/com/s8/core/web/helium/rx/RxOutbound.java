package com.s8.core.web.helium.rx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLSession;


/**
 * <h1>IOReactive</h1>
 * <p>
 * Based on the "don't call us, we'll call you" principle. 
 * Namely, use this class by overriding this method and supply 
 * bytes when required.
 * </p>
 * <p>
 * Note that is the responsability of the application to flip/clear/compact buffer
 * </p>
 * @author pc
 *
 */
public abstract class RxOutbound {

	public enum Need {

		NONE, SEND, SHUT_DOWN;
	}


	/**
	 * socket channel
	 */
	private SocketChannel socketChannel;


	/**
	 * the outbound byte buffer. MUST be left in WRITE mode.
	 */
	protected ByteBuffer networkBuffer;



	/**
	 * trigger sending
	 */
	private Need need;

	private int lastWriteBytecount;



	private final boolean rx_isVerbose;


	public RxOutbound(int capacity, RxWebConfiguration configuration) {
		super();

		this.need = Need.NONE;

		networkBuffer = ByteBuffer.allocate(capacity);
		this.rx_isVerbose = configuration.isRxVerbose;
	}

	public abstract RxConnection getConnection();

	
	/**
	 * 
	 * @param connection
	 */
	public void Rx_bind(RxConnection connection) {
		this.socketChannel = getConnection().getSocketChannel();
	}

	/**
	 * Compares <code>sessionProposedCapacity<code> with buffer's capacity. If buffer's capacity is smaller,
	 * returns a buffer with the proposed capacity. If it's equal or larger, returns a buffer
	 * with capacity twice the size of the initial one.
	 *
	 * @param buffer - the buffer to be enlarged.
	 * @param sessionProposedCapacity - the minimum size of the new buffer, proposed by {@link SSLSession}.
	 * @return A new buffer with a larger capacity.
	 */
	protected void increaseNetwordBufferCapacity(int sessionProposedCapacity) {

		/* allocate new buffer */
		ByteBuffer extendedBuffer = ByteBuffer.allocate(sessionProposedCapacity > networkBuffer.capacity() ? 
				sessionProposedCapacity : 
					networkBuffer.capacity() * 2);
		
		/* put networkBuffer in READ mode */
		networkBuffer.flip();

		/* copy remaining content */
		extendedBuffer.put(networkBuffer);
		
		/* replace (networkBuffer is in WRITE mode) */
		networkBuffer = extendedBuffer;
	}


	/**
	 * <p>
	 * Callback function for write.
	 * </p>
	 * 
	 * @param networkBuffer
	 * 
	 * @return a flag indicating if the above layer (implementing this method) is
	 *         requesting another write after this one. Typical reasons for
	 *         requesting such an additional write are:
	 *         <ul>
	 *         <li>We are in ever writing mode (for instance: streaming client)</li>
	 *         <li>The data to be transmitted have not successfully been entirely
	 *         written to the buffer, so need another buffer to write
	 *         remaining.</li>
	 *         </ul>
	 * @throws IOException 
	 */
	public abstract void onRxSending() throws IOException;


	/**
	 * Callback when has been remotely closed
	 * 
	 * @return
	 */
	public abstract void onRxRemotelyClosed();


	/**
	 * Callback when failed
	 * @param exception
	 * @throws IOException 
	 */
	public abstract void onRxFailed(IOException exception);

	/**
	 * 
	 * @return false if write operation did NOT result into an outbound termination, true otherwise
	 */
	public Need write() {


		try {

			if(need==Need.SEND) {

				// reset flag
				need = Need.NONE;


				// write as much as possible (I/O operation is always expensive)
				onRxSending();

				/* buffer READ_MODE start of section */
				// flip to prepare passing on socket channel
				networkBuffer.flip();


				// write operation
				lastWriteBytecount = socketChannel.write(networkBuffer);

				/* 
				 * Everything might not have been written, 
				 * so compact (and switch to direct write mode) */
				networkBuffer.compact();
				/* buffer READ_MODE end of section */


				// remote closing
				if(lastWriteBytecount==-1) {

					// reset flag
					need = Need.SHUT_DOWN;

					onRxRemotelyClosed();
				}
			}
		} 
		catch (IOException exception) {

			// print exception
			if(rx_isVerbose) {
				System.out.println("[RxOutbound] write encounters an exception:");
				System.out.println("\t"+exception.getMessage());
				System.out.println("\t--> SHUT_DOWN required");				
			}

			// reset flag
			need = Need.SHUT_DOWN;

			onRxFailed(exception);
		}

		return need;
	}



	public void send() {



		/* <DEBUG> 
		System.out.println(">>Send asked (previously: "+need+")");
		if(need==Need.SHUT_DOWN) {
			throw new RuntimeException("Cannot ask for sending once shut down has been requested");
		}
	 	</DEBUG> */

		// update flag
		need = Need.SEND;

		// notify selector
		getConnection().wakeup();
	}


	public Need getState() {
		return need;
	}



	public int getLastWriteBytecount() {
		return lastWriteBytecount;
	}





}
