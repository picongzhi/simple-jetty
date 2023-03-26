package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.component.Destroyable;
import com.pcz.simple.jetty.core.component.LifeCycle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 处理器
 *
 * @author picongzhi
 */
public interface Handler extends LifeCycle, Destroyable {
    /**
     * 处理请求
     *
     * @param target      目标请求，URI 或者名称
     * @param baseRequest 请求对象
     * @param request     {@link HttpServletRequest}
     * @param response    {@link HttpServletResponse}
     * @throws IOException      IO 异常
     * @throws ServletException Servlet 异常
     */
    void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException;

    /**
     * 获取服务器
     *
     * @return 服务器
     */
    Server getServer();

    /**
     * 设置服务器
     *
     * @param server 服务器
     */
    void setServer(Server server);

    /**
     * 执行销毁
     */
    @Override
    void destroy();
}
