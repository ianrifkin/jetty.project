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

package org.eclipse.jetty12.server.servlet;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty12.server.handler.ContextHandler;

public class ServletDispatcher implements RequestDispatcher
{
    private final ContextHandler.Context _context;
    private final ServletHandler _servletHandler;
    private final ServletHandler.MappedServlet _mappedServlet;

    public ServletDispatcher(ContextHandler.Context context, ServletHandler servletHandler, ServletHandler.MappedServlet mapping)
    {
        _context = context;
        _servletHandler = servletHandler;
        _mappedServlet = mapping;
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException
    {
        // TODO stuff about parameters

        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;

        _mappedServlet.handle(new ForwardServletRequestWrapper(httpRequest), httpResponse);

        if (!httpRequest.isAsyncStarted())
        {
            try
            {
                httpResponse.getOutputStream().close();
            }
            catch (IllegalStateException e)
            {
                httpResponse.getWriter().close();
            }
        }
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException
    {
        // Like the forward impl, but also need to wrap response to stop close and headers
    }

    private class ForwardServletRequestWrapper extends HttpServletRequestWrapper
    {
        private final HttpServletRequest _httpServletRequest;

        public ForwardServletRequestWrapper(HttpServletRequest httpRequest)
        {
            super(httpRequest);
            _httpServletRequest = httpRequest;
        }

        @Override
        public String getPathInfo()
        {
            return _mappedServlet.getServletPathMapping().getPathInfo();
        }

        @Override
        public String getServletPath()
        {
            return _mappedServlet.getServletPathMapping().getServletPath();
        }

        @Override
        public Object getAttribute(String name)
        {
            switch (name)
            {
                case RequestDispatcher.FORWARD_REQUEST_URI:
                    return _httpServletRequest.getRequestURI();
                case RequestDispatcher.FORWARD_SERVLET_PATH:
                    return _httpServletRequest.getServletPath();
                case RequestDispatcher.FORWARD_PATH_INFO:
                    return _httpServletRequest.getPathInfo();

                // TODO etc.
                default:
                    break;
            }
            return super.getAttribute(name);
        }
    }
}
