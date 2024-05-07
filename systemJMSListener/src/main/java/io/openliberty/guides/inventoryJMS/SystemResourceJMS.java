// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.inventoryJMS;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.QueueReceiver;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/systems")
public class SystemResourceJMS {
	String props;

	@Inject
	private Tracer tracer;


	@GET
	@Path("/jms/{qm}/{qs}/{qr}")
	@WithSpan("Inventory request Received/ send response")
	public Response getPropertiesJMS(@PathParam("qm") String qm, @PathParam("qs") String qs,
			@PathParam("qr") String qr) {

		
    	 
		InitialContext ic;
		try {
			ic = new InitialContext();

			// QueueConnectionFactory cf1 = (QueueConnectionFactory) ic.lookup("jms/"+qm);
			QueueConnectionFactory cf1 = (QueueConnectionFactory) ic.lookup("jms/" + qm);
			Queue q1 = (Queue) ic.lookup("jms/" + qs);
			Queue q2 = (Queue) ic.lookup("jms/" + qr);

			QueueConnection con = cf1.createQueueConnection("bob", "bob");

			con.start();
			QueueSession session1 = (QueueSession) con.createQueueSession(false,
					Session.AUTO_ACKNOWLEDGE);

			QueueReceiver rec = session1.createReceiver(q1);
			
			TextMessage msg = (TextMessage) rec.receive(1000);

			String tracestate=msg.getStringProperty("tracestate");
			String traceparent=msg.getStringProperty("traceparent");
			String[] a_tp=traceparent.split("-");
			String tp_traceid=a_tp[1];
			String tp_spanid=a_tp[2];
			
			SpanBuilder sb = tracer.spanBuilder("GettingPropertiesJMS");		
			SpanContext parentContext =
	    	          SpanContext.   	          
	    	          createFromRemoteParent(
	    	              tp_traceid, tp_spanid, 
	    	              TraceFlags.getSampled(), 
	    	              TraceState.getDefault());			
			
			Context context= (Context.current()).with(Span.wrap(parentContext));
			Span getPropertiesSpan = sb.setParent(context).startSpan();
			Scope scope = getPropertiesSpan.makeCurrent();
			
			String msgID = msg.getJMSMessageID();
			
			if (session1 != null)
				session1.close();

			QueueSession session2 = (QueueSession) con.createQueueSession(false,
					Session.AUTO_ACKNOWLEDGE);
			QueueSender sender = session2.createSender(q2);

			String response = System.getProperties().toString();
			TextMessage respMsg = session2.createTextMessage(response);
			respMsg.setJMSMessageID(msgID);
			respMsg.setJMSCorrelationID(msgID);
			
			respMsg.setStringProperty("tracestate", tracestate);
			respMsg.setStringProperty("traceparent", traceparent);
			
			getPropertiesSpan.addEvent("msgID = " + msgID);
			System.out.println("msgID = " + msgID);
			getPropertiesSpan.addEvent("sendRequest Done");
			sender.send(respMsg);

			props = msg.getText();
			
			session2.close();
			con.close();
			getPropertiesSpan.end();

		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {


			return Response.ok(props).build();
		}

	}

}
