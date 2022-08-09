package com.nightgame.googlepay;

import android.annotation.SuppressLint;
import android.app.Activity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.nightgame.googlepay.Interface.IConn;
import com.nightgame.googlepay.Interface.IConsume;
import com.nightgame.googlepay.Interface.IGoogleLifecycle;
import com.nightgame.googlepay.Interface.IGooglePay;
import com.nightgame.googlepay.Interface.IHistory;
import com.nightgame.googlepay.Interface.IPay;
import com.nightgame.leeframework.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class GooglePay implements IGooglePay, IGoogleLifecycle
{
    @SuppressLint("StaticFieldLeak")
    public static GooglePay instance;
    @SuppressLint("StaticFieldLeak")
    private static Activity _MainAct;
    @SuppressLint("StaticFieldLeak")
    private static PayMgr _PayMgr = new PayMgr();

    public static GooglePay build(Activity act)
    {
        GooglePay google = new GooglePay();
        instance = google;
        _MainAct = act;
        return google;
    }

    /**
     * 连接Google服务
     */
    public void connect(IConn connCb)
    {
        _PayMgr.connect(_MainAct, connCb);
    }

    /**
     * 断开与Google的连接
     */
    public void disconnect()
    {
        _PayMgr.disconnect();
    }

    /**
     * Google服务是否准备好
     */
    public boolean isReady()
    {
        return _PayMgr.isReady();
    }


    // region 生命周期

    public void onCreate()
    {
        if (_PayMgr != null)
        {
            _PayMgr.onCreate();
        }
    }

    public void onResume()
    {
        if (_PayMgr != null)
        {
            _PayMgr.onResume();
        }
    }

    public void onDestroy()
    {
        if (_PayMgr != null)
        {
            _PayMgr.onDestroy();
            _PayMgr = null;
        }
    }

    //endregion

    //region 查询订单

    /**
     * 请求 订单查询(InApp)
     */
    public void reqQueryInApp(String skus)
    {
        if (_PayMgr != null)
        {
            _PayMgr.querySkuDetail(skus, BillingClient.SkuType.INAPP);
        }
    }

    /**
     * 请求 订单查询(Subs)
     */
    public void reqQuerySubs(String skus)
    {
        if (_PayMgr != null)
        {
            _PayMgr.querySkuDetail(skus, BillingClient.SkuType.SUBS);
        }
    }

    /**
     * 请求 查询所有订单
     */
    public void reqQueryAll()
    {
        if (_PayMgr != null)
        {
            _PayMgr.queryPurchasesAll();
        }
    }

    /**
     * 请求 查询App历史
     */
    public void reqQueryHistoryInApp(IHistory history)
    {
        if (_PayMgr != null)
        {
            _PayMgr.queryHistory(BillingClient.SkuType.INAPP, history);
        }
    }

    /**
     * 请求 查询订阅历史
     */
    public void reqQueryHistorySubs(IHistory history)
    {
        if (_PayMgr != null)
        {
            _PayMgr.queryHistory(BillingClient.SkuType.SUBS, history);
        }
    }

    /**
     * 请求 查询全部历史
     */
    public void reqQueryHistoryAll(IHistory history)
    {
        if (_PayMgr != null)
        {
            _PayMgr.queryHistoryAll(history);
        }
    }

    //endregion

    //region 支付

    public void setAutoConsume(boolean value)
    {
        if (_PayMgr != null)
        {
            _PayMgr.setAutoConsume(value);
        }
    }

    /**
     * 请求支付
     */
    public void reqPay(String msg, IPay payCb, IConsume consumeCb)
    {
        Utils.log("reqPayInApp : " + msg);
        try
        {
            JSONObject json = new JSONObject(msg);
            String orderId = json.getString("OrderId");
            String sku = json.getString("Sku");

            Utils.logFat("GooglePay reqPay OrderId : %s,sku : %s", orderId, sku);
            if (_PayMgr != null)
            {
                SkuDetails skuDetails = null;
                if (_PayMgr.getSkuDetails().getValue() != null)
                {
                    skuDetails = _PayMgr.getSkuDetails().getValue().get(sku);
                }
                if (skuDetails == null)
                {
                    _PayMgr.payRspFail(BillingClient.BillingResponseCode.ERROR);
                    Utils.log("Could not find SkuDetails to make purchase.");
                    return;
                }
                BillingFlowParams.Builder billingBuilder = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).setObfuscatedAccountId(orderId);
                BillingFlowParams billingParams = billingBuilder.build();
                _PayMgr.launchBillingFlow(billingParams, payCb, consumeCb);
            }
        }
        catch (JSONException ex)
        {
            Utils.error("reqPayInApp : " + ex);
        }
    }


    /**
     * 请求 消耗
     */
    public void reqConsume(String msg, IConsume consumeCb)
    {
        Utils.log("reqConsume : " + msg);
        try
        {
            JSONObject json = new JSONObject(msg);
            String sku = json.getString("Sku");
            Utils.logFat("GoogleConsume reqConsume sku : %s", sku);
            if (_PayMgr != null)
            {
                Purchase purchase = null;
                for (Purchase purchaseTmp : _PayMgr.getPurchases().getValue())
                {
                    if (purchaseTmp.getSkus().contains(sku))
                    {
                        purchase = purchaseTmp;
                        break;
                    }
                }
                if (purchase != null)
                {
                    _PayMgr.consumePurchase(purchase, consumeCb);
                }
            }
        }
        catch (JSONException ex)
        {
            Utils.error("reqConsume : " + ex);
        }
    }


    //endregion


}