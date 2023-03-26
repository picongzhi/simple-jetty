package com.pcz.simple.jetty.server.handler;

import com.pcz.simple.jetty.server.Request;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 错误处理器
 *
 * @author picongzhi
 */
public class ErrorHandler extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    }

    public interface ErrorPageMapper {
    }
}
