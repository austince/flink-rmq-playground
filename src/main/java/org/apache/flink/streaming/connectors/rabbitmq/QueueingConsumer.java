// Copyright (c) 2007-Present Pivotal Software, Inc.  All rights reserved.
//
// This software, the RabbitMQ Java client library, is triple-licensed under the
// Mozilla Public License 1.1 ("MPL"), the GNU General Public License version 2
// ("GPL") and the Apache License version 2 ("ASL"). For the MPL, please see
// LICENSE-MPL-RabbitMQ. For the GPL, please see LICENSE-GPL2.  For the ASL,
// please see LICENSE-APACHE2.
//
// This software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
// either express or implied. See the LICENSE file for specific language governing
// rights and limitations of this software.
//
// If you have any questions regarding licensing, please contact us at
// info@rabbitmq.com.
//
// Changes made:
//	- copied from com.rabbitmq.client.QueueingConsumer
//	- updated naming conventions for the Apache Flink standards

package org.apache.flink.streaming.connectors.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.utility.Utility;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class QueueingConsumer extends DefaultConsumer {
	private final BlockingQueue<Delivery> queue;

	// When this is non-null the queue is in shutdown mode and nextDelivery should
	// throw a shutdown signal exception.
	private volatile ShutdownSignalException shutdown;
	private volatile ConsumerCancelledException cancelled;

	// Marker object used to signal the queue is in shutdown mode.
	// It is only there to wake up consumers. The canonical representation
	// of shutting down is the presence of shutdown.
	// Invariant: This is never on queue unless shutdown != null.
	private static final Delivery POISON = new Delivery(null, null, null);

	public QueueingConsumer(Channel channel) {
		this(channel, Integer.MAX_VALUE);
	}

	public QueueingConsumer(Channel channel, int capacity) {
		super(channel);
		this.queue = new LinkedBlockingQueue<>(capacity);
	}

	/**
	 * Check if we are in shutdown mode and if so throw an exception.
	 */
	private void checkShutdown() {
		if (shutdown != null) {
			throw Utility.fixStackTrace(shutdown);
		}
	}

	/**
	 * If delivery is not POISON nor null, return it.
	 * <p/>
	 * If delivery, shutdown and cancelled are all null, return null.
	 * <p/>
	 * If delivery is POISON re-insert POISON into the queue and
	 * throw an exception if POISONed for no reason.
	 * <p/>
	 * Otherwise, if we are in shutdown mode or cancelled,
	 * throw a corresponding exception.
	 */
	private Delivery handle(Delivery delivery) {
		if (delivery == POISON ||
			delivery == null && (shutdown != null || cancelled != null)) {
			if (delivery == POISON) {
				queue.add(POISON);
				if (shutdown == null && cancelled == null) {
					throw new IllegalStateException(
						"POISON in queue, but null shutdown and null cancelled. " +
							"This should never happen, please report as a BUG");
				}
			}
			if (null != shutdown) {
				throw Utility.fixStackTrace(shutdown);
			}
			if (null != cancelled) {
				throw Utility.fixStackTrace(cancelled);
			}
		}
		return delivery;
	}

	/**
	 * Main application-side API: wait for the next message delivery and return it.
	 *
	 * @return the next message
	 * @throws InterruptedException       if an interrupt is received while waiting
	 * @throws ShutdownSignalException    if the connection is shut down while waiting
	 * @throws ConsumerCancelledException if this consumer is cancelled while waiting
	 */
	public Delivery nextDelivery() throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
		return handle(queue.take());
	}

	/**
	 * Main application-side API: wait for the next message delivery and return it.
	 *
	 * @param timeout timeout in millisecond
	 * @return the next message or null if timed out
	 * @throws InterruptedException       if an interrupt is received while waiting
	 * @throws ShutdownSignalException    if the connection is shut down while waiting
	 * @throws ConsumerCancelledException if this consumer is cancelled while waiting
	 */
	public Delivery nextDelivery(long timeout)
		throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
		return nextDelivery(timeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * Main application-side API: wait for the next message delivery and return it.
	 *
	 * @param timeout timeout
	 * @param unit    timeout unit
	 * @return the next message or null if timed out
	 * @throws InterruptedException       if an interrupt is received while waiting
	 * @throws ShutdownSignalException    if the connection is shut down while waiting
	 * @throws ConsumerCancelledException if this consumer is cancelled while waiting
	 */
	public Delivery nextDelivery(long timeout, TimeUnit unit)
		throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
		return handle(queue.poll(timeout, unit));
	}

	@Override
	public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
		shutdown = sig;
		queue.add(POISON);
	}

	@Override
	public void handleCancel(String consumerTag) throws IOException {
		cancelled = new ConsumerCancelledException();
		queue.add(POISON);
	}

	@Override
	public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
		checkShutdown();
		this.queue.add(new Delivery(envelope, properties, body));
	}
}
