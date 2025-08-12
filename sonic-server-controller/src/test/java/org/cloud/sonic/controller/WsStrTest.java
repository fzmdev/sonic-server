package org.cloud.sonic.controller;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
public class WsStrTest {

    @Test
    public void testStr() {
        String wsUrl = "ws://10.0.90.32:7777/websockets/android/28a810ab-dc92-48f5-8654-fdf68654279f/9A301FFAZ007EC/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic29uaWMiLCIxZDRkMDlkZS02OTRlLTQwZDktODYwNy00ZmMyMmRlZDIwMTYiXSwiZXhwIjoxNzU1ODI5NDI0fQ.UMqdG4KchPaAEX4CbuBzWStCeqgc31z0JYsyR8zriVg";
        String[] parts = wsUrl.replace("ws://", "").split("/");
        String[] hostPort = parts[0].split(":");

        Map<String, String> result = new HashMap<>();
        result.put("host", hostPort[0]);
        result.put("port", hostPort[1]);
        result.put("platform", parts[2]);
        result.put("secretKey", parts[3]);
        result.put("udId", parts[4]);
        result.put("token", parts[5]);

        System.out.println(result.get("platform"));
        System.out.println(result.get("udId"));

    }
}
