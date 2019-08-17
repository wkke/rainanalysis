package com.wkken.rainanalysis;

import java.util.Date;

public class ResultHolder {
public static final ResultHolder INSTANCE = new ResultHolder();

private String _Result="";

public Boolean getDebug() {
    return isDebug;
}

public void setDebug(Boolean debug) {
    isDebug = debug;
}

private Boolean isDebug=false;

public int getHours() {
    return hours;
}

public void setHours(int hours) {
    this.hours = hours;
}

private int hours=2;

private  long LastFileTime=0;
private ResultHolder() {
    System.err.println("Elvis Constructor is invoked!");
    if (INSTANCE != null) {
        System.err.println("实例已存在，无法初始化！");
        throw new UnsupportedOperationException("实例已存在，无法初始化！");
    }


}
public long lastDate(){

    return LastFileTime;

}

public String getResult(){

    return _Result;

}

public void updateResult(String newstr){
    LastFileTime= new Date().getTime();
    _Result=newstr;
}
}
