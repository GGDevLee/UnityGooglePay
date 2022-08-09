# GooglePay

**联系作者：419731519（QQ）**

### ============GooglePay介绍============
### 基于com.android.billingclient:billing:5.0.0
#### 完全封装Google内购，使用简单的方法，即可完成内购流程
#### 单机游戏的情况下，推荐使用自动消耗的方式进行内购
#### 网络游戏的情况下，推荐使用服务器消耗的方式进行内购
#### 觉得我的插件能帮助到你，麻烦帮我点个Star支持一下❤️

### =================使用方法=================

- 引入GooglePay
- 在settings.gradle中添加仓库地址
- 因为插件需要LeeFramework基础类，所以也需要添加LeeFramework
- [LeeFramework](https://gitee.com/GameDevLee/LeeFramework/tree/master)是笔者关于Unity的游戏框架，感兴趣也可以看看
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://ggdevlee-maven.pkg.coding.net/repository/MyWorld/leeframework' }
        maven { url 'https://ggdevlee-maven.pkg.coding.net/repository/MyWorld/googlepay' }
    }
}
```

- 在build.gradle中添加依赖
```groovy
implementation 'com.nightgame.leeframework:leeframework:1.0.1'
implementation 'com.nightgame.googlepay:googlepay:1.0.0'
implementation 'com.android.billingclient:billing:5.0.0'
```

---
### =================响应码汇总([官方地址](https://developer.android.com/google/play/billing/billing_reference))=================

| 响应代码                                    | 值 | 说明                                                                                                                                 |
| ------------------------------------------- | -- | ------------------------------------------------------------------------------------------------------------------------------------ |
| BILLING_RESPONSE_RESULT_OK                  | 0  | 成功                                                                                                                                  |
| BILLING_RESPONSE_RESULT_USER_CANCELED       | 1  | 用户按上一步或取消对话框                                                                                                             |
| BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE | 2  | 网络连接断开                                                                                                                         |
| BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE | 3  | 所请求的类型不支持 Billing API 版本(内购环境问题)                                                                                         |
| BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE    | 4  | 请求的商品已不再出售。                                                                                                               |
| BILLING_RESPONSE_RESULT_DEVELOPER_ERROR     | 5  | 提供给 API 的参数无效。此错误也可能说明未在 Google Play 中针对应用内购买结算正确签署或设置应用，或者应用在其清单中不具备所需的权限。 |
| BILLING_RESPONSE_RESULT_ERROR               | 6  | API 操作期间出现严重错误                                                                                                             |
| BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED  | 7  | 未能购买，因为已经拥有此商品                                                                                                         |
| BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED      | 8  | 未能消费，因为尚未拥有此商品         

---
### =================常见问题=================

**1. 初始化失败，错误码:3，这是内购环境问题。** 

有以下可能：用的是模拟器，三件套版本太旧，应用的内购环境没配置(接入谷歌服务，内购权限)，vpn地域不支持，科学上网失败，Google账号未登录等。

解决方法：a.先验证环境。在商店下载一个有内购的应用，看能否进行内购。b.如果别人的能进行内购之后，再次测试你的应用，看是否正常，来确认应用的内购环境是否正常。

**2. 能够查询价格，但无法购买，提示"商品无法购买"之类。** 

这是基础配置问题，有以下可能：版本号与线上版本不对应，测试版本却不是测试账号(大概率)，签名不对应。

**3. 能够查询价格，但无法调起内购都没有弹窗，错误码:3，报错：Error:In-app billing error: Null data in IAB activity resul。** 

原因是没有给Google play商店弹窗权限，国内很多手机都有弹窗权限管理，特别是小米，如果没允许，是不会有任何提示，并且拦截了的。(这个问题在新版的gp商店已经不存在）

**4. 我们检测到您的应用使用的是旧版 Google Play Developer API。自 2019 年 12 月 1 日起，
 我们将不再支持此 API 的版本 1 和版本 2。请在该日期之前将您使用的 API 更新到版本 3。请注意，此变动与弃用 AIDL/结算库无关。**

 升级到com.android.billingclient:billing库，弃用AIDL相关代码。
---



- 初始化谷歌内购服务
```java
public class MainActivity extends AppCompatActivity
{
    private GooglePay _GooglePay;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        _GooglePay = GooglePay.build(this);
    }
}
```

- 设置是否自动消耗
```java
public class MainActivity extends AppCompatActivity
{
    private GooglePay _GooglePay;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        _GooglePay = GooglePay.build(this);
		//单机推荐自动消耗
		_GooglePay.setAutoConsume(true);
		//联网推荐手动消耗
		_GooglePay.setAutoConsume(false);
    }
}
```

- 创建与Google服务的连接
```java
/**
 * 连接Google服务
 */
public void connect()
{
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
```

- 断开与Google服务的连接
```java
/**
 * 断开与Google服务的连接
 */
public void disconnect()
{
	_GooglePay.disconnect();
}
```

- 必须在Activity中注册生命周期方法
```java
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
```

- 查询所有订单
查询所有订单会触发未完成的订单的，推荐进入游戏后，调用一次
可以触发内购成功，但是未消耗的订单的回调
```java
/**
 * 查询所有订单
 */
public void queryAll()
{
	_GooglePay.reqQueryAll();
}
```

- 查询历史订单
```java
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
```


- 拉起内购
```java
/**
 * 内购
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
				Utils.logFat("内购成功 : %s", str);
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
					Utils.logFat("内购失败 : %s", rspCode);
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

```

- 消耗订单
```java
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
```
