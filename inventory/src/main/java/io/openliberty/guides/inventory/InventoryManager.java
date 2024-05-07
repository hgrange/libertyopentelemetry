package io.openliberty.guides.inventory;

import java.util.ArrayList;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import io.openliberty.guides.inventory.model.SystemData;
import io.openliberty.guides.inventory.model.InventoryList;
import io.openliberty.guides.inventory.model.SystemClient;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueReceiver;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.MessageProducer;

import java.util.List;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;


import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class InventoryManager {

    @Inject
    @ConfigProperty(name = "system.http.port")
    private int SYSTEM_PORT;

    private List<SystemData> systems = Collections.synchronizedList(new ArrayList<>());
    private SystemClient systemClient = new SystemClient();
    
    
    @WithSpan
    InitialContext getInitialContext() {
    	try {
			return new InitialContext();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
    
    @WithSpan
    QueueConnection createConnection( InitialContext ic, @SpanAttribute("ConnFactory") String ref, String user,  String password) {
    	try {
			QueueConnectionFactory cf1 = (QueueConnectionFactory) ic.lookup(ref);
			QueueConnection con = cf1.createQueueConnection(user, password);
			con.start();
			return con;
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
    	return null;
    }
    
    @WithSpan
    void closeConnection( QueueConnection con) {
    	try {
			con.close();

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
    }
    
    @WithSpan("Manager Put MQ message")
    String sendRequest(InitialContext ic, QueueConnection con, String qs, String qr, String text, SpanContext context ) {
    	String msgID;
    	try {
			Queue q1 = (Queue) ic.lookup(qs);
			Queue q2 = (Queue) ic.lookup(qr);
	    	QueueSession session = (QueueSession) con.createQueueSession(false,
					Session.AUTO_ACKNOWLEDGE);
	    	MessageProducer producer = session.createProducer(q1);
	    	TextMessage tm = session.createTextMessage(text);
	    	
	        tm.setJMSReplyTo(q2);
	        producer.send(tm);
	        HashMap<String, Object> properties = new HashMap<String, Object> ();
	        
	       Enumeration<?> srcProperties = tm.getPropertyNames();
	        
	        msgID = tm.getJMSMessageID();
			String tracestate=tm.getStringProperty("tracestate");
			String traceparent=tm.getStringProperty("traceparent");

	        
	    	if ( session != null) 
	    		        session.close();
	    	return msgID;
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
    
    @WithSpan("Manager Get MQ Message")
    String[] getResponse(InitialContext ic, QueueConnection con, String queue ) {
    	String msgID;
    	String[]  s_ret = new String[2];
    	try {
			Queue q2 = (Queue) ic.lookup(queue);

	    	QueueSession session = (QueueSession) con.createQueueSession(false,
						Session.AUTO_ACKNOWLEDGE);
			
	    	
	    	QueueReceiver rec = session.createReceiver(q2);
	    	TextMessage msg = (TextMessage) rec.receive();
	    	msgID = msg.getJMSMessageID();
	    	
	    	if ( session != null) 
	    		        session.close();
	    	s_ret[0] = msg.getText();
	    	s_ret[1] = msgID;
	    	return s_ret;
	    	
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    	return null;
    }

    @WithSpan("Inventory manager Get")
    public Properties get(String hostname) {
        systemClient.init(hostname, SYSTEM_PORT);
        Properties properties = systemClient.getProperties();
        return properties;
    }

    @WithSpan("Inventory manager List")
    public InventoryList list() {
        return new InventoryList(systems);
    }



    @WithSpan("Inventory manager Add")
    public void add(@SpanAttribute("hostname") String host,
                    Properties systemProps) {
        Properties props = new Properties();
        props.setProperty("os.name", systemProps.getProperty("os.name"));
        props.setProperty("user.name", systemProps.getProperty("user.name"));
        SystemData system = new SystemData(host, props);
        if (!systems.contains(system)) {
            systems.add(system);
        }
    }
    
    @WithSpan("Inventory manager Clear")
    int clear() {
        int propertiesClearedCount = systems.size();
        systems.clear();
        return propertiesClearedCount;
    }
}

