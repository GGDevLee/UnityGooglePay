package com.nightgame.googlepay;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.nightgame.googlepay.Interface.IConn;
import com.nightgame.googlepay.Interface.IConsume;
import com.nightgame.googlepay.Interface.IHistory;
import com.nightgame.googlepay.Interface.IPay;
import com.nightgame.leeframework.utils.Utils;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayMgr implements LifecycleObserver, SkuDetailsResponseListener, PurchasesUpdatedListener, BillingClientStateListener, PurchasesResponseListener, PurchaseHistoryResponseListener
{
    private BillingClient _BillingClient;
    private Activity _MainAct;
    private Application _App;
    private MutableLiveData<List<Purchase>> _Purchases = new MutableLiveData<>();
    private MutableLiveData<Map<String, SkuDetails>> _SkuDetails = new MutableLiveData<>();
    private boolean _IsAutoConsume = true;


    private IConn _ConnCb;
    private IPay _PayCb;
    private IConsume _ConsumeCb;
    private IHistory _History;

    /**
     * 创建跟Google服务连接
     */
    public void connect(Activity activity, IConn connCb)
    {
        _MainAct = activity;
        _App = activity.getApplication();

        if (connCb != null)
        {
            _ConnCb = connCb;
        }
        _BillingClient = BillingClient.newBuilder(activity).setListener(this).enablePendingPurchases().build();
        startConnection();
    }

    private boolean startConnection()
    {
        if (_BillingClient == null)
        {
            Utils.error("_BillingClient == null");
            return false;
        }

        if (!_BillingClient.isReady())
        {
            _BillingClient.startConnection(this);
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * 不建议断开Google服务
     */
    public void disconnect()
    {
        if (_BillingClient != null)
        {
            if (_BillingClient.isReady())
            {
                _BillingClient.endConnection();
                _BillingClient = null;
            }
        }
    }

    /**
     * Google内购服务是否准备好
     */
    public boolean isReady()
    {
        return _BillingClient != null && _BillingClient.isReady();
    }


    // region 生命周期

    public void onCreate()
    {
        queryPurchasesAll();
    }

    public void onResume()
    {
        queryPurchasesAll();
    }

    public void onDestroy()
    {
        Utils.log("destroy");
        if (_BillingClient.isReady())
        {
            Utils.log("BillingClient: Closing Connection");
            _BillingClient.endConnection();
        }
    }

    //endregion


    //region 对外接口

    public MutableLiveData<List<Purchase>> getPurchases()
    {
        return _Purchases;
    }

    public MutableLiveData<Map<String, SkuDetails>> getSkuDetails()
    {
        return _SkuDetails;
    }


    //region 查询订单


    /**
     * 查询 商品细节
     */
    public void querySkuDetail(String skus, String type)
    {
        try
        {
            JSONArray jArray = new JSONArray(skus);
            List<String> productList = new ArrayList<String>();
            for (int i = 0; i < jArray.length(); i++)
            {
                String jb = jArray.getString(i);
                productList.add(jb);
            }
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            if (type == BillingClient.SkuType.INAPP)
            {
                params.setSkusList(productList).setType(BillingClient.SkuType.INAPP);
                queryPurchases(BillingClient.SkuType.INAPP);
            }
            else
            {
                params.setSkusList(productList).setType(BillingClient.SkuType.SUBS);
                queryPurchases(BillingClient.SkuType.SUBS);
            }
            _BillingClient.querySkuDetailsAsync(params.build(), this);
            Utils.log("reqInAppQuery skus : " + skus);
        }
        catch (JSONException ex)
        {
            Utils.log("querySkuDetail JSONException : " + ex);
        }
    }

    /**
     * 查询 商品
     */
    private void queryPurchases(String type)
    {
        if (!_BillingClient.isReady())
        {
            Utils.log("queryPurchases: BillingClient is not ready");
            startConnection();
            return;
        }
        if (type == BillingClient.SkuType.INAPP)
        {
            _BillingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, this);
        }
        else
        {
            _BillingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, this);
        }
    }

    /**
     * 查询 全部商品
     */
    public void queryPurchasesAll()
    {
        if (!_BillingClient.isReady())
        {
            Utils.log("queryPurchases: BillingClient is not ready");
            startConnection();
            return;
        }
        _BillingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, this);
        _BillingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, this);
    }

    /**
     * 查询 历史订单
     */
    public void queryHistory(String type,IHistory history)
    {
        if (history != null)
        {
            _History = history;
        }
        if (!_BillingClient.isReady())
        {
            Utils.log("queryHistory: BillingClient is not ready");
            startConnection();
            return;
        }
        _BillingClient.queryPurchaseHistoryAsync(type, this);
    }

    public void queryHistoryAll(IHistory history)
    {
        if (history != null)
        {
            _History = history;
        }
        if (!_BillingClient.isReady())
        {
            Utils.log("queryHistoryAll: BillingClient is not ready");
            startConnection();
            return;
        }
        _BillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, this);
        _BillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, this);
    }

    //endregion


    /**
     * 调起支付
     */
    public int launchBillingFlow(BillingFlowParams params, IPay payCb, IConsume consumeCb)
    {
        if (payCb != null)
        {
            _PayCb = payCb;
        }
        if (consumeCb != null)
        {
            _ConsumeCb = consumeCb;
        }
        if (!_BillingClient.isReady())
        {
            Utils.log("launchBillingFlow : BillingClient is not ready");
            startConnection();
            return BillingClient.BillingResponseCode.ERROR;
        }

        //调起支付
        BillingResult billingResult = _BillingClient.launchBillingFlow(_MainAct, params);
        int rspCode = billingResult.getResponseCode();
        String debugMsg = billingResult.getDebugMessage();

        Utils.logFat("launchBillingFlow: BillingResponse rspCode :%s , debugMsg :%s", rspCode, debugMsg);

        return rspCode;
    }


    /**
     * 支付成功后自动消耗
     */
    public void setAutoConsume(boolean isAuto)
    {
        _IsAutoConsume = isAuto;
    }


    /**
     * 消耗商品
     */
    public void consumePurchase(Purchase purchase, IConsume consumeCb)
    {
        if (consumeCb != null)
        {
            _ConsumeCb = consumeCb;
        }

        final Purchase tmpPurchase = purchase;

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
        {
            ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
            ConsumeResponseListener listener = new ConsumeResponseListener()
            {
                /**
                 * 监听 消耗回调
                 */
                @Override
                public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String purchaseToken)
                {
                    int rspCode = billingResult.getResponseCode();
                    String debugMsg = billingResult.getDebugMessage();

                    Utils.logFat("onConsumeResponse: rspCode = %s ,debugMsg = %s", rspCode, debugMsg);
                    JSONObject rspMsg = new JSONObject();
                    try
                    {
                        rspMsg.put("rspCode", rspCode);

                        PurchaseToJson(tmpPurchase, rspMsg);

                        if (rspCode == BillingClient.BillingResponseCode.OK)
                        {
                            if (_ConsumeCb != null)
                            {
                                _ConsumeCb.ConsumeCb(true, rspMsg.toString());
                            }
                        }
                        else
                        {
                            if (_ConsumeCb != null)
                            {
                                _ConsumeCb.ConsumeCb(false, rspMsg.toString());
                            }
                        }
                    }
                    catch (JSONException ex)
                    {
                        Utils.log("onConsumeResponse Succ : " + ex);
                    }
                }
            };
            _BillingClient.consumeAsync(consumeParams, listener);
        }
        else
        {
            JSONObject rspMsg = new JSONObject();
            try
            {
                rspMsg.put("rspCode", BillingClient.BillingResponseCode.ERROR);
                PurchaseToJson(purchase, rspMsg);
                if (_ConsumeCb != null)
                {
                    _ConsumeCb.ConsumeCb(false, rspMsg.toString());
                }
            }
            catch (Exception ex)
            {
                Utils.log("onConsumeResponse Fail : " + ex);
            }
        }
    }

    //endregion


    //region 回调处理

    /**
     * 启动成功
     */
    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult)
    {
        Utils.log("onBillingSetupFinished ..");
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
        {
            JSONObject rspMsg = new JSONObject();
            try
            {
                rspMsg.put("debugMsg", billingResult.getDebugMessage());
                if (_ConnCb != null)
                {
                    _ConnCb.ConnCb(true, rspMsg.toString());
                }
            }
            catch (JSONException ex)
            {
                Utils.log("onBillingSetupFinished JSONException : " + ex);
                ex.printStackTrace();
            }
        }
    }


    /**
     * 连接失败
     */
    @Override
    public void onBillingServiceDisconnected()
    {
        Utils.log("onBillingServiceDisconnected ..");
        if (_ConnCb != null)
        {
            _ConnCb.ConnCb(false, "");
        }
        Utils.log("startConnection ..");
        _BillingClient.startConnection(this);
    }


    /**
     * 查询商品 回调
     */
    @Override
    public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> list)
    {
        if (billingResult == null)
        {
            Utils.log("查询商品异常 ：billingResult == null ");
            return;
        }
        int rspCode = billingResult.getResponseCode();
        String debugMsg = billingResult.getDebugMessage();
        Utils.logFat("查询订单回调 rspCode : %s DebugMsg : %s ", rspCode, debugMsg);
        switch (rspCode)
        {
            case BillingClient.BillingResponseCode.OK:
                if (list == null)
                {
                    Utils.log("onSkuDetailsResponse: null SkuDetails list");
                    _SkuDetails.postValue(Collections.<String, SkuDetails>emptyMap());
                    return;
                }
                else
                {
                    Map<String, SkuDetails> newSkusDetailList = new HashMap<String, SkuDetails>();
                    for (SkuDetails skuDetails : list)
                    {
                        Utils.log(skuDetails.getSku());
                        newSkusDetailList.put(skuDetails.getSku(), skuDetails);
                    }
                    _SkuDetails.postValue(newSkusDetailList);
                    return;
                }
        }
        //payRspFail(rspCode);
    }


    /**
     * 监听 支付回调
     */
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases)
    {
        if (billingResult == null)
        {
            Utils.log("onPurchasesUpdated: null BillingResult");
            return;
        }
        int rspCode = billingResult.getResponseCode();
        String debugMsg = billingResult.getDebugMessage();
        Utils.logFat("onPurchasesUpdated: rspCode = %s ,debugMsg = %s", rspCode, debugMsg);

        if (purchases != null)
        {
            _Purchases.postValue(purchases);
        }
        //支付成功
        if (rspCode == BillingClient.BillingResponseCode.OK && purchases != null)
        {
            for (Purchase purchase : purchases)
            {
                payRspSucc(purchase, rspCode);
            }
        }
        else
        {
            switch (rspCode)
            {
                case BillingClient.BillingResponseCode.USER_CANCELED:
                    Utils.log("onPurchasesUpdated: User Cancel");
                    break;
                case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                    Utils.log("onPurchasesUpdated: The user already owns this item");
                    break;
                case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                    Utils.log("onPurchasesUpdated: Developer error");
                    break;
            }
            payRspFail(rspCode);
        }
    }


    /**
     * 支付成功
     */
    public void payRspSucc(Purchase purchase, int rspCode)
    {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED)
        {
            Utils.error("Purchase.PurchaseState.PENDING 请检查...");
            payRspFail(BillingClient.BillingResponseCode.ERROR);
            return;
        }
        Utils.log("payRspSucc : " + rspCode);
        JSONObject rspMsg = new JSONObject();
        try
        {
            rspMsg.put("rspCode", rspCode);

            PurchaseToJson(purchase, rspMsg);

            if (_PayCb != null)
            {
                _PayCb.PayCb(true, rspMsg.toString());
            }
            if (_IsAutoConsume)
            {
                consumePurchase(purchase, _ConsumeCb);
            }
        }
        catch (JSONException ex)
        {
            Utils.log("payRspSucc : " + ex);
        }
    }


    /**
     * 支付失败
     */
    public void payRspFail(int rspCode)
    {
        Utils.log("payRspFail : " + rspCode);
        JSONObject rspMsg = new JSONObject();

        try
        {
            rspMsg.put("rspCode", rspCode);
            if (_PayCb != null)
            {
                _PayCb.PayCb(false, rspMsg.toString());
            }
        }
        catch (JSONException ex)
        {
            Utils.log("payRspFail JSONException : " + ex);
            ex.printStackTrace();
        }
    }


    /**
     * 查询订单回调
     */
    @Override
    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> purchases)
    {
        if (billingResult == null)
        {
            Utils.log("onQueryPurchasesResponse: null BillingResult");
            return;
        }
        int rspCode = billingResult.getResponseCode();
        String debugMsg = billingResult.getDebugMessage();
        Utils.logFat("onQueryPurchasesResponse: rspCode = %s ,debugMsg = %s", rspCode, debugMsg);

        if (purchases != null)
        {
            _Purchases.postValue(purchases);
        }

        //支付成功
        if (rspCode == BillingClient.BillingResponseCode.OK && purchases != null)
        {
            for (Purchase purchase : purchases)
            {
                payRspSucc(purchase, rspCode);
            }
        }
        else
        {
            switch (rspCode)
            {
                case BillingClient.BillingResponseCode.USER_CANCELED:
                    Utils.log("onQueryPurchasesResponse: User Cancel");
                    break;
                case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                    Utils.log("onQueryPurchasesResponse: The user already owns this item");
                    break;
                case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                    Utils.log("onQueryPurchasesResponse: Developer error");
                    break;
            }
            payRspFail(rspCode);
        }
    }


    /**
     * 查询历史回调
     */
    @Override
    public void onPurchaseHistoryResponse(@NonNull BillingResult billingResult, @Nullable List<PurchaseHistoryRecord> list)
    {
        if (billingResult == null)
        {
            Utils.log("onPurchaseHistoryResponse: null BillingResult");
            return;
        }

        int rspCode = billingResult.getResponseCode();
        String debugMsg = billingResult.getDebugMessage();

        Utils.logFat("onPurchaseHistoryResponse: rspCode = %s ,debugMsg = %s", rspCode, debugMsg);

        JSONObject allJson = new JSONObject();
        JSONArray jsonArr = new JSONArray();

        try
        {
            allJson.put("rspCode", rspCode);
            allJson.put("debugMsg", debugMsg);

            for (PurchaseHistoryRecord history : list)
            {
                JSONObject json = new JSONObject();
                HistoryToJson(history, json);
                jsonArr.put(json);
            }

            allJson.put("arr", jsonArr);
        }
        catch (JSONException ex)
        {
            Utils.log("payRspSucc : " + ex);
        }

        if (rspCode == BillingClient.BillingResponseCode.OK && list != null)
        {
            if (_History != null)
            {
                _History.HistoryCb(true, allJson.toString());
            }
        }
        else
        {
            if (_History != null)
            {
                _History.HistoryCb(false, allJson.toString());
            }
        }
    }


    /**
     * 订单，封装成Json
     */
    private void PurchaseToJson(Purchase purchase, JSONObject json)
    {
        try
        {
            json.put("developerPayload", purchase.getDeveloperPayload());
            json.put("purchaseState", purchase.getPurchaseState());
            json.put("purchaseToken", purchase.getPurchaseToken());
            json.put("data", purchase.getOriginalJson());
            json.put("signature", purchase.getSignature());
            json.put("skus", purchase.getSkus());
            json.put("orderId", purchase.getOrderId());
            json.put("accountId", purchase.getAccountIdentifiers().getObfuscatedAccountId());
            json.put("profileId", purchase.getAccountIdentifiers().getObfuscatedProfileId());
            json.put("packageName", purchase.getPackageName());
            json.put("purchaseTime", purchase.getPurchaseTime());
            json.put("quantity", purchase.getQuantity());
            json.put("isAcknowledged", purchase.isAcknowledged());
            json.put("isAutoRenewing", purchase.isAutoRenewing());
        }
        catch (JSONException ex)
        {
            Utils.log("PurchaseJson : " + ex);
        }
    }


    /**
     * 历史，封装成Json
     */
    private void HistoryToJson(PurchaseHistoryRecord history, JSONObject json)
    {
        try
        {
            json.put("purchaseToken", history.getPurchaseToken());
            json.put("originalJson", history.getOriginalJson());
            json.put("signature", history.getSignature());
            json.put("purchaseTime", history.getPurchaseTime());
            json.put("developerPayload", history.getDeveloperPayload());
            json.put("quantity", history.getQuantity());
            json.put("skus", history.getSkus());
        }
        catch (JSONException ex)
        {
            Utils.log("HistoryToJson : " + ex);
        }

    }
    //endregion
}
