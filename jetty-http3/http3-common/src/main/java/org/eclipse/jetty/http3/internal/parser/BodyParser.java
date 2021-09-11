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

package org.eclipse.jetty.http3.internal.parser;

import java.nio.ByteBuffer;

import org.eclipse.jetty.http3.ErrorCode;
import org.eclipse.jetty.http3.frames.DataFrame;
import org.eclipse.jetty.http3.frames.HeadersFrame;
import org.eclipse.jetty.http3.frames.SettingsFrame;
import org.eclipse.jetty.util.BufferUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>The base parser for the frame body of HTTP/3 frames.</p>
 * <p>Subclasses implement {@link #parse(ByteBuffer)} to parse
 * the frame specific body.</p>
 *
 * @see Parser
 */
public abstract class BodyParser
{
    private static final Logger LOG = LoggerFactory.getLogger(BodyParser.class);

    private final long streamId;
    private final HeaderParser headerParser;
    private final Parser.Listener listener;

    protected BodyParser(long streamId, HeaderParser headerParser, Parser.Listener listener)
    {
        this.streamId = streamId;
        this.headerParser = headerParser;
        this.listener = listener;
    }

    protected long getStreamId()
    {
        return streamId;
    }

    protected long getBodyLength()
    {
        return headerParser.getFrameLength();
    }

    /**
     * <p>Parses the frame body bytes in the given {@code buffer}; only the body
     * bytes are consumed, therefore when this method returns, the buffer
     * may contain unconsumed bytes, for example for other frames.</p>
     *
     * @param buffer the buffer to parse
     * @return true if all the frame body bytes were parsed, false if not enough
     * frame body bytes were present in the buffer
     */
    public abstract boolean parse(ByteBuffer buffer);

    protected void emptyBody(ByteBuffer buffer)
    {
        sessionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code(), "invalid_frame");
    }

    protected void sessionFailure(ByteBuffer buffer, int error, String reason)
    {
        BufferUtil.clear(buffer);
        notifySessionFailure(error, reason);
    }

    protected void notifySessionFailure(int error, String reason)
    {
        try
        {
            listener.onSessionFailure(error, reason);
        }
        catch (Throwable x)
        {
            LOG.info("failure while notifying listener {}", listener, x);
        }
    }

    protected void notifyStreamFailure(long streamId, int error, String reason)
    {
        try
        {
            listener.onStreamFailure(streamId, error, reason);
        }
        catch (Throwable x)
        {
            LOG.info("failure while notifying listener {}", listener, x);
        }
    }

    protected void notifyData(DataFrame frame)
    {
        try
        {
            listener.onData(frame);
        }
        catch (Throwable x)
        {
            LOG.info("failure while notifying listener {}", listener, x);
        }
    }

    protected void notifyHeaders(HeadersFrame frame)
    {
        try
        {
            listener.onHeaders(frame);
        }
        catch (Throwable x)
        {
            LOG.info("failure while notifying listener {}", listener, x);
        }
    }

    protected void notifySettings(SettingsFrame frame)
    {
        try
        {
            listener.onSettings(frame);
        }
        catch (Throwable x)
        {
            LOG.info("failure while notifying listener {}", listener, x);
        }
    }
}
