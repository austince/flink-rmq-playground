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
//	- brought this class out of com.rabbitmq.client.QueueingConsumer

package org.apache.flink.streaming.connectors.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

/**
 * Encapsulates an arbitrary message - simple "bean" holder structure.
 * TODO: replace this with `com.rabbitmq.client.Delivery` in RMQ v5.x
 */
public class Delivery {
	private final Envelope envelope;
	private final AMQP.BasicProperties properties;
	private final byte[] body;

	public Delivery(Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
		this.envelope = envelope;
		this.properties = properties;
		this.body = body;
	}

	/**
	 * Retrieve the message envelope.
	 *
	 * @return the message envelope
	 */
	public Envelope getEnvelope() {
		return envelope;
	}

	/**
	 * Retrieve the message properties.
	 *
	 * @return the message properties
	 */
	public AMQP.BasicProperties getProperties() {
		return properties;
	}

	/**
	 * Retrieve the message body.
	 *
	 * @return the message body
	 */
	public byte[] getBody() {
		return body;
	}
}
