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
package io.openliberty.guides.inventory;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import io.opentelemetry.api.trace.Tracer;
import io.openliberty.guides.inventory.model.InventoryList;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.QueueReceiver;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@RequestScoped
@Path("/systems")
public class InventoryResource {

    @Inject
    private InventoryManager manager;
    

    @Inject
    private Tracer tracer;
    
    @GET
    @Path("/jms/{qm}/{qs}/{qr}")

    public Response getPropertiesJMS(@PathParam("qm") String qm, @PathParam("qs") String qs, @PathParam("qr") String qr) throws NamingException, JMSException {
    	Span getPropertiesSpan = tracer.spanBuilder("GettingPropertiesJMS").startSpan();
    	Scope scope = getPropertiesSpan.makeCurrent();
    	SpanContext context = getPropertiesSpan.getSpanContext();
    	
    	InitialContext ic = manager.getInitialContext();
    	getPropertiesSpan.addEvent("getInitailContext Done");
    	
    	QueueConnection con = manager.createConnection(ic, "jms/"+qm , "bob", "bob");
    	getPropertiesSpan.addEvent("createConnection Done");
    	
    	String msgID = manager.sendRequest(ic , con, "jms/"+qs, "jms/"+qr, "get system properties", context);
    	System.out.println("sendRequest msgID = " + msgID);
    	getPropertiesSpan.addEvent("msgID = " + msgID);
    	getPropertiesSpan.addEvent("sendRequest Done");
    	
    	String[] s_ret = manager.getResponse(ic, con, "jms/"+qr );
    	getPropertiesSpan.addEvent("MsgID = " + s_ret[1] );
    	System.out.println("getResponse msgID = " + s_ret[1]);
    	getPropertiesSpan.addEvent("getResponse Done");
    	
        manager.closeConnection(con);
        getPropertiesSpan.addEvent("closeConnection Done");
        
        getPropertiesSpan.end();
        return Response.ok(s_ret[0]).build();
       
    }

   
    
    
    
    
    
    @GET
    @Path("/{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPropertiesForHost(@PathParam("hostname") String hostname) {
        Span getPropertiesSpan = tracer.spanBuilder("GettingProperties").startSpan();
        Properties props = null;

        try (Scope scope = getPropertiesSpan.makeCurrent()) {
            props = manager.get(hostname);
            if (props == null) {
                getPropertiesSpan.addEvent("Cannot get properties");
                return Response.status(Response.Status.NOT_FOUND)
                         .entity("{ \"error\" : \"Unknown hostname or the system "
                               + "service may not be running on " + hostname + "\" }")
                         .build();
            }
            getPropertiesSpan.addEvent("Received properties");

            manager.add(hostname, props);
        } finally {
            getPropertiesSpan.end();
        }
        return Response.ok(props).build();

    }
    
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public InventoryList listContents() {
        return manager.list();
    }
    
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearContents() {
        int cleared = manager.clear();

        if (cleared == 0) {
            return Response.status(Response.Status.NOT_MODIFIED)
                           .build();
        }
        return Response.status(Response.Status.OK)
                       .build();
    }
}

