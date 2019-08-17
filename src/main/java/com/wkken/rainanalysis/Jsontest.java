package com.wkken.rainanalysis;


import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;

import java.util.HashMap;

public class Jsontest {

public static void main(String[] args) {

    HashMap<String,Object> item=new HashMap<>();
    for (int i=0; i<1890000;i++)
    {
        item.put("p"+i,"The party complaining about large HTTP response headers for the issue I was working was Akamai, a popular edge caching solution. Per this forum post, if the origin serves a response with more than 8192 bytes of headers, Akamai will serve a 502 to the client. The official documentation regarding this limitation is only available when logging in to their portal. This limitation is not handled particularly gracefully by Akamai and the result is a WSoD with no error message.\n" +
                "\n" +
                "Servers");
    }
    JSONObject  rs = new JSONObject();
  String p=  JSON.toJSONString(item);
//10000 =4968891
// 80000 39828891
    //18 89708891
    //990000  493898891
   System.out.println(p.getBytes().length);
}



}
