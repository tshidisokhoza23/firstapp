package com.tpsk.tj.auth.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class SmsBroadcastReceiver : BroadcastReceiver() {

    private var smsBroadcastReceiverListener: SmsBroadcastReceiverListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
            val extras = intent.extras ?: return
            val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status

            when (status?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    val message = extras.get(SmsRetriever.EXTRA_SMS_MESSAGE) as? String
                    if (message != null) {
                        smsBroadcastReceiverListener?.onSuccess(message)
                    }
                }
                CommonStatusCodes.TIMEOUT -> {
                    smsBroadcastReceiverListener?.onFailure()
                }
            }
        }
    }

    interface SmsBroadcastReceiverListener {
        fun onSuccess(message: String)
        fun onFailure()
    }

    fun setListener(listener: SmsBroadcastReceiverListener) {
        this.smsBroadcastReceiverListener = listener
    }
}
