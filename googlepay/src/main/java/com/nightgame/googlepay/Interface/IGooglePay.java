package com.nightgame.googlepay.Interface;

public interface IGooglePay
{
    void connect(IConn connCb);

    void disconnect();

    boolean isReady();


    void reqQueryInApp(String skus);

    void reqQuerySubs(String skus);

    void reqQueryAll();

    void reqQueryHistoryInApp(IHistory history);

    void reqQueryHistorySubs(IHistory history);

    void reqQueryHistoryAll(IHistory history);


    void setAutoConsume(boolean value);

    void reqPay(String msg, IPay payCb, IConsume consumeCb);

    void reqConsume(String msg, IConsume consumeCb);

}
