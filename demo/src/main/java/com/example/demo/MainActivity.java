package com.example.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.nightgame.googlepay.GooglePay;
import com.nightgame.googlepay.Interface.IConn;
import com.nightgame.googlepay.Interface.IConsume;
import com.nightgame.googlepay.Interface.IHistory;
import com.nightgame.googlepay.Interface.IPay;
import com.nightgame.leeframework.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity
{
    private GooglePay _GooglePay;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        _GooglePay = GooglePay.build(this);

        _GooglePay.setAutoConsume(true);

        connect();
    }

    /**
     * 连接Google服务
     */
    public void connect()
    {
        try
        {
            Runtime.getRuntime().exec("logcat -c");
            Process process = Runtime.getRuntime().exec("logcat");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            while (true)
            {
                String line;
                while ((line = bufferedReader.readLine()) != null)
                {
                    Utils.log(line);
                }

                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException var5)
                {
                }
            }
        }
        catch (Exception ex)
        {

        }
        _GooglePay.connect(new IConn()
        {
            @Override
            public void ConnCb(boolean res, String str)
            {
                if (res)
                {
                    Utils.log("连接成功 ： " + str);
                }
                else
                {
                    Utils.log("连接失败 : " + str);
                }
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        GooglePay.instance.onResume();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        GooglePay.instance.onDestroy();
    }


    /**
     * 断开与Google服务的连接
     */
    public void disconnect()
    {
        _GooglePay.disconnect();
    }

    /**
     * 查询所有订单
     */
    public void queryAll()
    {
        _GooglePay.reqQueryAll();
    }

    /**
     * 查询所有历史订单
     */
    public void queryAHistoryAll()
    {
        _GooglePay.reqQueryHistoryAll(new IHistory()
        {
            @Override
            public void HistoryCb(boolean res, String str)
            {
                if (res)
                {
                    try
                    {
                        JSONObject json = new JSONObject(str);
                        int rspCode = json.getInt("rspCode");
                        String debugMsg = json.getString("debugMsg");

                        JSONArray arr = json.getJSONArray("arr");

                        for (int i = 0; i < arr.length(); i++)
                        {
                            JSONObject item = arr.getJSONObject(i);

                            String purchaseToken = item.getString("purchaseToken");
                            String originalJson =item.getString("originalJson");
                            String signature =item.getString("signature");
                            long purchaseTime =item.getLong("purchaseTime");
                            String developerPayload =item.getString("developerPayload");
                            int quantity =item.getInt("quantity");
                            String skus =item.getString("skus");
                        }
                    }
                    catch (JSONException ex)
                    {
                        Utils.error(ex.toString());
                    }
                }
            }
        });
    }

    /**
     * 支付
     */
    public void pay(String sku, String orderId)
    {
        JSONObject rspMsg = new JSONObject();
        try
        {
            rspMsg.put("OrderId", orderId);
            rspMsg.put("Sku", sku);
        }
        catch (JSONException ex)
        {
            Utils.log("pay : " + ex);
        }

        _GooglePay.reqPay(rspMsg.toString(), new IPay()
        {
            @Override
            public void PayCb(boolean res, String str)
            {
                if (res)
                {
                    Utils.logFat("支付成功 : %s", str);
                    try
                    {
                        JSONObject json = new JSONObject(str);
                        int rspCode = json.getInt("rspCode");
                        String developerPayload = json.getString("developerPayload");
                        int purchaseState = json.getInt("purchaseState");
                        String purchaseToken = json.getString("purchaseToken");
                        String data = json.getString("data");
                        String signature = json.getString("signature");
                        String orderId = json.getString("orderId");
                        String accountId = json.getString("accountId");
                        String profileId = json.getString("profileId");
                        String packageName = json.getString("packageName");
                        long purchaseTime = json.getLong("purchaseTime");
                        int quantity = json.getInt("quantity");
                        boolean isAcknowledged = json.getBoolean("isAcknowledged");
                        boolean isAutoRenewing = json.getBoolean("isAutoRenewing");
                    }
                    catch (JSONException ex)
                    {
                        Utils.error(ex.toString());
                    }
                }
                else
                {
                    try
                    {
                        JSONObject json = new JSONObject(str);
                        int rspCode = json.getInt("rspCode");
                        Utils.logFat("支付失败 : %s", rspCode);
                    }
                    catch (JSONException ex)
                    {
                        Utils.error(ex.toString());
                    }
                }
            }
        }, new IConsume()
        {
            @Override
            public void ConsumeCb(boolean res, String str)
            {
                try
                {
                    JSONObject json = new JSONObject(str);
                    int rspCode = json.getInt("rspCode");
                    String developerPayload = json.getString("developerPayload");
                    int purchaseState = json.getInt("purchaseState");
                    String purchaseToken = json.getString("purchaseToken");
                    String data = json.getString("data");
                    String signature = json.getString("signature");
                    String orderId = json.getString("orderId");
                    String accountId = json.getString("accountId");
                    String profileId = json.getString("profileId");
                    String packageName = json.getString("packageName");
                    long purchaseTime = json.getLong("purchaseTime");
                    int quantity = json.getInt("quantity");
                    boolean isAcknowledged = json.getBoolean("isAcknowledged");
                    boolean isAutoRenewing = json.getBoolean("isAutoRenewing");

                    if (res)
                    {
                        Utils.logFat("消耗成功 : %s", rspCode);
                    }
                    else
                    {
                        Utils.logFat("消耗失败 : %s", rspCode);
                    }
                }
                catch (JSONException ex)
                {
                    Utils.error(ex.toString());
                }
            }
        });
    }

    /**
     * 消耗
     */
    public void consume(String sku)
    {
        JSONObject rspMsg = new JSONObject();
        try
        {
            rspMsg.put("Sku", sku);
        }
        catch (JSONException ex)
        {
            Utils.log("pay : " + ex);
        }

        GooglePay.instance.reqConsume(rspMsg.toString(), new IConsume()
        {
            @Override
            public void ConsumeCb(boolean res, String str)
            {
                try
                {
                    JSONObject json = new JSONObject(str);
                    String rspCode = json.getString("rspCode");
                    String developerPayload = json.getString("developerPayload");
                    int purchaseState = json.getInt("purchaseState");
                    String purchaseToken = json.getString("purchaseToken");
                    String data = json.getString("data");
                    String signature = json.getString("signature");
                    String orderId = json.getString("orderId");
                    String accountId = json.getString("accountId");
                    String profileId = json.getString("profileId");
                    String packageName = json.getString("packageName");
                    long purchaseTime = json.getLong("purchaseTime");
                    int quantity = json.getInt("quantity");
                    boolean isAcknowledged = json.getBoolean("isAcknowledged");
                    boolean isAutoRenewing = json.getBoolean("isAutoRenewing");

                    if (res)
                    {
                        Utils.logFat("消耗成功 : %s", rspCode);
                    }
                    else
                    {
                        Utils.logFat("消耗失败 : %s", rspCode);
                    }
                }
                catch (JSONException ex)
                {
                    Utils.error(ex.toString());
                }
            }
        });

    }



}