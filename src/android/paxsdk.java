/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

package com.mrboss.paxsdk;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;

import org.apache.cordova.*;
import org.apache.cordova.engine.*;

import java.io.IOException;
import java.io.InputStream;

import android.os.AsyncTask;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Context;
import android.app.AlertDialog;

import java.io.*;

import java.util.Set;
import android.util.Base64;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import com.pax.poslink.*;
import com.pax.poslink.ProcessTransResult.ProcessTransResultCode;

import com.google.gson.Gson;

public class paxsdk extends CordovaPlugin {
  private static final String LOG_TAG = "paxsdkPlugin";
  private PosLink poslink = new PosLink();
  private String actiontimeout = "60000";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    try {
      if ("POSLinkPaymentMethod".equals(action)) {
        String tenderType = args.getString(0);
        String transType = args.getString(1);
        String shopid = args.getString(2);
        String totalMoneys = args.getString(3);
        String destPort = args.getString(4);
        String destIP = args.getString(5);
        String serialPort = args.getString(6);
        String commType = args.getString(7);
        String baudRate = args.getString(8);
        String res = POSLinkPaymentMethod(tenderType, transType, shopid, totalMoneys, destPort, destIP, serialPort, commType, baudRate);
        callbackContext.success(res);
        return true;
      }
      else if("POSLinkBatchMethod".equals(action)){
        String edcType = args.getString(0);
        String transType = args.getString(1);
        String destPort = args.getString(2);
        String destIP = args.getString(3);
        String serialPort = args.getString(4);
        String commType = args.getString(5);
        String baudRate = args.getString(6);
        String res = POSLinkBatchMethod(edcType, transType, destPort, destIP, serialPort, commType, baudRate);
        callbackContext.success(res);
        return true;
      }
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      return false;
    } catch (Throwable e1) {
      callbackContext.error(e1.getMessage());
      return false;
    }
    callbackContext.error("No This Method");
    return false;
  }

  public String POSLinkPaymentMethod(String tenderType, String transType, String shopid, String totalMoneys, String destPort, String destIP, String serialPort, String commType, String baudRate) throws InterruptedException {
    String result = "";
    double amount = Math.round(Double.parseDouble(totalMoneys) * 100) / 100;
    String outTradeNo = GenerateOutTradeNo(Integer.parseInt(shopid));

    PaymentRequest paymentRequest = new PaymentRequest();
    CommSetting commSetting = new CommSetting();

    commSetting.setTimeOut(actiontimeout);
    if (!"".equals(destPort)) {
      commSetting.setDestPort(destPort.trim());
    }
    if (!"".equals(destIP)) {
      commSetting.setDestIP(destIP.trim());
    }
    if (!"".equals(serialPort)) {
      commSetting.setSerialPort(serialPort.trim());
    }
    if (!"".equals(commType)) {
      commSetting.setCommType(commType.trim());
    }
    if (!"".equals(baudRate)) {
      commSetting.setBaudRate(baudRate.trim());
    }

    paymentRequest.TenderType = paymentRequest.ParseTenderType(tenderType);
    paymentRequest.TransType = paymentRequest.ParseTransType(transType);
    paymentRequest.Amount = String.valueOf(amount * 100);
    paymentRequest.ECRRefNum = outTradeNo;
    paymentRequest.InvNum = outTradeNo;

    poslink.SetCommSetting(commSetting);
    poslink.PaymentRequest = paymentRequest;
    ProcessTransResult presult = poslink.ProcessTrans();

    // 5. Show the result
    if (presult.Code == ProcessTransResultCode.OK) {
      PaymentResponse paymentResponse = poslink.PaymentResponse;
      if (paymentResponse != null && paymentResponse.ResultCode != null) {
        result = "OK||" + outTradeNo + "||" + ToJsonStr(paymentResponse);
      } else {
        result = "Null Response";
      }
    } else if (presult.Code == ProcessTransResultCode.TimeOut) {
      result = "Action Timeout.";
    } else
    {
      result = presult.Msg;
    }
    return result;
  }

  public String POSLinkBatchMethod(String edcType, String transType, String destPort, String destIP, String serialPort, String commType, String baudRate) throws InterruptedException {
    String result = "";

    BatchRequest batchRequest = new BatchRequest();
    CommSetting commSetting = new CommSetting();

    commSetting.setTimeOut(actiontimeout);
    if (!"".equals(destPort)) {
      commSetting.setDestPort(destPort.trim());
    }
    if (!"".equals(destIP)) {
      commSetting.setDestIP(destIP.trim());
    }
    if (!"".equals(serialPort)) {
      commSetting.setSerialPort(serialPort.trim());
    }
    if (!"".equals(commType)) {
      commSetting.setCommType(commType.trim());
    }
    if (!"".equals(baudRate)) {
      commSetting.setBaudRate(baudRate.trim());
    }

    batchRequest.EDCType = batchRequest.ParseEDCType(edcType);
    batchRequest.TransType = batchRequest.ParseTransType(transType);

    poslink.SetCommSetting(commSetting);
    poslink.BatchRequest = batchRequest;
    ProcessTransResult presult = poslink.ProcessTrans();

    // 5. Show the result
    if (presult.Code == ProcessTransResultCode.OK) {
      BatchResponse batchResponse = poslink.BatchResponse;
      if (batchResponse != null && batchResponse.ResultCode != null) {
        result = "OK||" + ToJsonStr(batchResponse);
      } else {
        result = "Unknown error: BatchResponse is null.";
      }
    } else if (presult.Code == ProcessTransResultCode.TimeOut) {
      result = "Action Timeout.";
    } else
    {
      result = "Error Processing Manage:" + presult.Msg;
    }
    return result;
  }

  private String GenerateOutTradeNo(int shopid) {
    Random ran = new Random();
    String strshopid = String.valueOf(shopid);
    if (strshopid.length() > 3) {
      strshopid = strshopid.substring(strshopid.length() - 3, strshopid.length() - 1);
    }
    Date currentTime = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");
    String dateString = formatter.format(currentTime);

    String res = strshopid + dateString + ran.nextInt(10);
    if (res.length() > 16) {
      res = res.substring(0, 15);
    }
    return res;
  }

  private String ToJsonStr(Object obj) {
    Gson gson = new Gson();
    String jsondate = gson.toJson(obj);
    return jsondate;
  }

  private void Alert(String msg) {
    Dialog alertDialog = new AlertDialog.Builder(this.cordova.getActivity()).
    setTitle("对话框的标题").
    setMessage(msg).
    setCancelable(false).
    setNegativeButton("确定", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        // TODO Auto-generated method stub
      }
    }).
    create();
    alertDialog.show();
  }
}