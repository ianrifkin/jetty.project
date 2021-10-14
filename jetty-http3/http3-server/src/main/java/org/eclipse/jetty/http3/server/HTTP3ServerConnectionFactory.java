//
// ========================================================================
// Copyright (c) 1995-2021 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.http3.server;

import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.frames.HeadersFrame;
import org.eclipse.jetty.http3.internal.HTTP3Session;
import org.eclipse.jetty.http3.internal.HTTP3Stream;
import org.eclipse.jetty.http3.server.internal.ServerHTTP3Session;
import org.eclipse.jetty.http3.server.internal.ServerHTTP3StreamConnection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.HttpConfiguration;

public class HTTP3ServerConnectionFactory extends AbstractHTTP3ServerConnectionFactory
{
    public HTTP3ServerConnectionFactory()
    {
        this(new HttpConfiguration());
    }

    public HTTP3ServerConnectionFactory(HttpConfiguration configuration)
    {
        super(configuration, new HTTP3SessionListener());
    }

    private static class HTTP3SessionListener implements Session.Server.Listener
    {
        @Override
        public Stream.Listener onRequest(Stream stream, HeadersFrame frame)
        {
            HTTP3Stream http3Stream = (HTTP3Stream)stream;
            HTTP3StreamListener listener = new HTTP3StreamListener(http3Stream.getEndPoint());
            listener.onRequest(stream, frame);
            return listener;
        }
    }

    private static class HTTP3StreamListener implements Stream.Listener
    {
        private final EndPoint endPoint;

        public HTTP3StreamListener(EndPoint endPoint)
        {
            this.endPoint = endPoint;
        }

        private ServerHTTP3StreamConnection getConnection()
        {
            return (ServerHTTP3StreamConnection)endPoint.getConnection();
        }

        public void onRequest(Stream stream, HeadersFrame frame)
        {
            HTTP3Stream http3Stream = (HTTP3Stream)stream;
            Runnable runnable = getConnection().onRequest(http3Stream, frame);
            if (runnable != null)
            {
                ServerHTTP3Session protocolSession = (ServerHTTP3Session)http3Stream.getSession().getProtocolSession();
                protocolSession.offer(runnable);
            }
        }

        @Override
        public void onDataAvailable(Stream stream)
        {
            HTTP3Stream http3Stream = (HTTP3Stream)stream;
            Runnable runnable = getConnection().onDataAvailable(http3Stream);
            if (runnable != null)
            {
                ServerHTTP3Session protocolSession = (ServerHTTP3Session)http3Stream.getSession().getProtocolSession();
                protocolSession.offer(runnable);
            }
        }

        @Override
        public void onTrailer(Stream stream, HeadersFrame frame)
        {
            HTTP3Stream http3Stream = (HTTP3Stream)stream;
            Runnable runnable = getConnection().onTrailer(http3Stream, frame);
            if (runnable != null)
            {
                ServerHTTP3Session protocolSession = (ServerHTTP3Session)http3Stream.getSession().getProtocolSession();
                protocolSession.offer(runnable);
            }
        }

        @Override
        public boolean onIdleTimeout(Stream stream, Throwable failure)
        {
            return getConnection().onIdleTimeout((HTTP3Stream)stream, failure);
        }

        @Override
        public void onFailure(Stream stream, Throwable failure)
        {
            getConnection().onFailure((HTTP3Stream)stream, failure);
        }
    }
}
