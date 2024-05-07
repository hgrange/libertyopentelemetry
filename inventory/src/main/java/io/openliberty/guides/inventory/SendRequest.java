package io.openliberty.guides.inventory;

import java.util.Enumeration;
import java.util.HashMap;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.jms.JmsQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Queue;

public class SendRequest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		 
		try {
			JmsFactoryFactory ff = (JmsFactoryFactory)JmsFactoryFactory.getInstance(JmsConstants.WMQ_PROVIDER);

			JmsQueueConnectionFactory factory = ff.createQueueConnectionFactory();

			factory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE,
					WMQConstants.WMQ_CM_CLIENT);

			factory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, "QM1");
			factory.setStringProperty(WMQConstants.WMQ_HOST_NAME, "192.168.184.133");
			factory.setIntProperty(WMQConstants.WMQ_PORT, 1414);
			factory.setStringProperty(WMQConstants.WMQ_CHANNEL, "APP2.SVRCONN");
			factory.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "ootel");

			QueueConnection con = (QueueConnection) factory.createQueueConnection("bob", "bob");



			QueueSession session = (QueueSession) con.createQueueSession(false,
					Session.AUTO_ACKNOWLEDGE);
			
			Queue q1 = (Queue) session.createQueue("Q1");
			Queue q2 = (Queue) session.createQueue("Q2");
			
			MessageProducer producer = session.createProducer(q1);
			TextMessage tm = session.createTextMessage("Get remote system properties");
     
			//tm.setJMSReplyTo(q2);
			producer.send(tm);
			HashMap<String, Object> properties = new HashMap<String, Object>();

			Enumeration<?> srcProperties = tm.getPropertyNames();
			String msgID = tm.getJMSMessageID();
			System.out.println("Message sent !!!!!!!!!!");
			System.out.println("Message ID = " + msgID);

			/*
			MessageConsumer rec = session.createConsumer(q1);
			Message msg = (Message) rec.receive(1000);

			
			String msgID2 = msg.getJMSMessageID();

			System.out.println("Message received !!!!!!!!!!");
			System.out.println("Message ID = " + msgID2);
			*/
			session.close();

			con.close();

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
