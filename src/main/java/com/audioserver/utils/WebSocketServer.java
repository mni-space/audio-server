package com.audioserver.utils;
import com.alibaba.fastjson2.JSONObject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/websocket/{sid}")
@Component
public class WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    private static int onlineCount = 0;

    private static ConcurrentHashMap<String,WebSocketServer> webSocketServerMap = new ConcurrentHashMap<>();

    private Session session;

    private String sid;


    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        this.sid = sid;
        this.session = session;
        webSocketServerMap.put(sid, this);
        addOnlineCount();
        log.info("有新窗口开始监听:%s,当前在线人数为%d".formatted(sid, getOnlineCount()));
        log.info("openSuccess:%s".formatted(webSocketServerMap.keySet()));
    }

    @OnClose
    public void onClose() {
        webSocketServerMap.remove(sid);
        subOnlineCount();
        log.info("有一连接关闭！当前在线人数为" + getOnlineCount());
        log.info("openSuccess:"+webSocketServerMap.keySet());
    }

    @OnMessage
    public void onMessage(String jsonStr) throws IOException {
        sendInfo(jsonStr);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        if(error instanceof EOFException) {
            return;
        }
        if(error instanceof IOException && error.getMessage().contains("已建立的连接")) {
            return;
        }
        log.error("发生错误", error);
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        synchronized (session) {
            this.session.getBasicRemote().sendText(message);
        }
    }

    public static void sendInfo(String sid,String message) throws IOException {
        WebSocketServer socketServer = webSocketServerMap.get(sid);
        if(socketServer != null) {
            socketServer.sendMessage(message);
        }
    }

    /**
     * 广播
     * @param jsonStr
     * @throws IOException
     */
    public static void sendInfo(String jsonStr) throws IOException {
        for(String sid : webSocketServerMap.keySet()) {
            webSocketServerMap.get(sid).sendMessage(jsonStr);
        }
    }

    /**
     * 指定对象发送
     * @param userId
     * @param message
     * @throws IOException
     */
    public static void sendInfoByUserId(Long userId,Object message) throws IOException {
        for(String sid : webSocketServerMap.keySet()) {
            String[] sids =  sid.split("id");
            if(sids.length == 2) {
                String id = sids[1];
                if(userId.equals(Long.parseLong(id))) {
                    webSocketServerMap.get(sid).sendMessage(JSONObject.toJSONString(message));
                }
            }
        }
    }

    public static Session getWebSocketSession(String sid) {
        if(webSocketServerMap.containsKey(sid)) {
            return webSocketServerMap.get(sid).session;
        }
        return null;
    }

    public static synchronized void addOnlineCount() {
        onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        onlineCount--;
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }
}
