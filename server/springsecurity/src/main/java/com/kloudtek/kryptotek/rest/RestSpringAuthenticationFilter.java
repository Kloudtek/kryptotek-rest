package com.kloudtek.kryptotek.rest;

import com.kloudtek.util.InvalidBackendDataException;
import com.kloudtek.util.TimeUtils;
import com.kloudtek.util.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.util.Date;

import static com.kloudtek.kryptotek.rest.RESTRequestSigner.*;
import static com.kloudtek.kryptotek.rest.SpringAuthenticationFilterHelper.STREAM_ATTR;

/**
 * Created by yannick on 6/21/17.
 */
public class RestSpringAuthenticationFilter extends GenericFilterBean {
    private SpringAuthenticationFilterHelper springAuthenticationFilterHelper;

    public RestSpringAuthenticationFilter(SpringAuthenticationFilterHelper springAuthenticationFilterHelper) {
        this.springAuthenticationFilterHelper = springAuthenticationFilterHelper;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String nonce = request.getHeader(HEADER_NONCE);
            String identity = request.getHeader(HEADER_IDENTITY);
            String timestampStr = request.getHeader(HEADER_TIMESTAMP);
            String signature = request.getHeader(HEADER_SIGNATURE);
            try {
                SigningUserDetails userDetails = springAuthenticationFilterHelper.authenticateRequest(request.getInputStream(), nonce, identity, timestampStr,
                        signature, request.getMethod(), request.getRequestURI(), request.getQueryString(), request);
                SecurityContextHolder.getContext().setAuthentication(new SignedRequestAuthenticationToken(userDetails));
                InputStream stream = (InputStream) request.getAttribute(STREAM_ATTR);
                if (stream != null) {
                    request.removeAttribute(STREAM_ATTR);
                    ResponseWrapper rw = new ResponseWrapper(response);
                    chain.doFilter(new RequestWrapper(request, stream), rw);
                    rw.outputStreamWrapper.os.close();
                    byte[] respData = rw.outputStreamWrapper.os.toByteArray();
                    RESTResponseSigner responseSigner = new RESTResponseSigner(nonce, signature,
                            rw.err != null ? rw.err : response.getStatus(), rw.err != null, respData);
                    response.setHeader(HEADER_TIMESTAMP, TimeUtils.formatISOUTCDateTime(new Date()));
                    response.setHeader(HEADER_SIGNATURE, springAuthenticationFilterHelper.signResponse(userDetails, responseSigner.getDataToSign()));
                    if (rw.err != null) {
                        response.setHeader(HEADER_EXCLUDEBODY, "true");
                    }
                    if (respData.length > 0) {
                        OutputStream os = response.getOutputStream();
                        try {
                            os.write(respData);
                        } finally {
                            IOUtils.close(os);
                        }
                    }
                    if (rw.err != null) {
                        if( rw.errMsg != null ) {
                            response.sendError(rw.err,rw.errMsg);
                        } else {
                            response.sendError(rw.err);
                        }
                    }
                    response.flushBuffer();
                    return;
                }
            } catch (AuthenticationFailedException e) {
                //
            } catch (InvalidRequestException e) {
                //
            } catch (InvalidBackendDataException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }


    public class RequestWrapper extends HttpServletRequestWrapper {
        private InputStreamWrapper stream;

        RequestWrapper(HttpServletRequest request, InputStream stream) {
            super(request);
            this.stream = new InputStreamWrapper(stream);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return stream;
        }
    }


    public class ResponseWrapper extends HttpServletResponseWrapper {
        OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper();
        private Integer err;
        private String errMsg;

        ResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return outputStreamWrapper;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return new PrintWriter(outputStreamWrapper.os);
        }

        @Override
        public void flushBuffer() throws IOException {
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            err = sc;
            errMsg = msg;
        }

        @Override
        public void sendError(int sc) throws IOException {
            err = sc;
        }
    }

    public class InputStreamWrapper extends ServletInputStream {
        private InputStream is;
        private boolean finished;
        private ReadListener readListener;

        InputStreamWrapper(InputStream is) {
            this.is = is;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            this.readListener = readListener;
        }

        @Override
        public int read() throws IOException {
            try {
                int read = is.read();
                if (read == -1) {
                    finished = true;
                    if (readListener != null) {
                        readListener.onAllDataRead();
                    }
                }
                return read;
            } catch (IOException e) {
                if (readListener != null) {
                    readListener.onError(e);
                }
                throw e;
            }
        }
    }

    public class OutputStreamWrapper extends ServletOutputStream {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        OutputStreamWrapper() {
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public void write(int b) throws IOException {
            os.write(b);
        }

        @Override
        public void write(@NotNull byte[] b) throws IOException {
            os.write(b);
        }

        @Override
        public void write(@NotNull byte[] b, int off, int len) throws IOException {
            os.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            os.flush();
        }

        @Override
        public void close() throws IOException {
            os.close();
        }
    }
}
