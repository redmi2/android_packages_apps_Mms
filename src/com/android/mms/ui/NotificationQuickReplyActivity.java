/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.mms.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.ui.ComposeMessageActivity;

public class NotificationQuickReplyActivity extends Activity {

    private static final String TAG = "NotificationQuickReplyActivity";

    public static final String MSG_SENDER = "msg_sender";
    public static final String MSG_THREAD_ID = "msg_thread_id";

    private static Drawable sDefaultContactImage;

    private TextView mReplyToTextView;
    private TextView mViewMessageTextView;
    private ImageButton mBackImageButton;
    private ImageButton mSendImageButton;
    private EditText mReplyContent;

    private long mMsgThreadId;
    private String mMsgSenderAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(MessagingNotification.NOTIFICATION_ID);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        setContentView(R.layout.quick_reply_message_activity);

        mMsgThreadId = intent.getLongExtra(MSG_THREAD_ID, -1);
        mMsgSenderAddress = intent.getStringExtra(MSG_SENDER);

        initViews();
        setSmsOrMmsSendButtonImage();
    }

    private void initViews() {
        mReplyToTextView = (TextView) findViewById(R.id.reply_to_text);
        mReplyToTextView.setText(getString(R.string.notification_reply_to,
                mMsgSenderAddress));

        mBackImageButton = (ImageButton) findViewById(R.id.back_arrow);
        mBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mViewMessageTextView = (TextView) findViewById(R.id.view_message_text);
        mViewMessageTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viewIntent = ComposeMessageActivity.createIntent(
                        NotificationQuickReplyActivity.this, mMsgThreadId);
                viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(viewIntent);
                finish();
            }
        });

        mSendImageButton = (ImageButton) findViewById(R.id.send_button);
        mSendImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String replyContent = mReplyContent.getText().toString();
                if (!TextUtils.isEmpty(replyContent)) {
                    sendSms(replyContent, mMsgSenderAddress, mMsgThreadId);
                }
            }
        });

        mReplyContent = (EditText) findViewById(R.id.embedded_text_editor);
        mReplyContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setSmsOrMmsSendButtonImage();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void sendSms(String msgText, String address, long threadId) {
        String[] dests = TextUtils.split(address, ";");
        int subId = SubscriptionManager.getDefaultSmsSubId();
        SmsMessageSender sender = new SmsMessageSender(this, dests, msgText, threadId, subId);
        try {
            sender.sendMessage(threadId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS message, threadId=" + threadId, e);
        } finally {
            finish();
        }
    }

    private void setSmsOrMmsSendButtonImage() {
        Contact contact = Contact.getMe(true);
        if (sDefaultContactImage == null) {
            sDefaultContactImage = this.getResources().getDrawable(R.drawable.default_avatar);
        }

        if (!TextUtils.isEmpty(mReplyContent.getText().toString())) {
            mSendImageButton.setEnabled(true);
        } else {
            mSendImageButton.setEnabled(false);
        }

        Drawable mAvatarDrawable = contact.getAvatar(this, sDefaultContactImage);
        if (mAvatarDrawable.equals(sDefaultContactImage)) {
            mSendImageButton.setImageDrawable(this.getResources().getDrawable(
                    R.drawable.send_button_selector));
            mSendImageButton.setBackground(this.getResources().getDrawable(
                    R.drawable.send_arrow_background));
            mSendImageButton.setScaleType(ImageButton.ScaleType.CENTER);
        } else {
            if (mSendImageButton.isEnabled()) {
                mSendImageButton.setScaleType(ImageButton.ScaleType.CENTER);
                mSendImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_send));
                mSendImageButton.setBackground(this.getResources().getDrawable(
                        R.drawable.send_arrow_background));
            } else {
                mSendImageButton.setScaleType(ImageButton.ScaleType.FIT_CENTER);
                mSendImageButton.setImageDrawable(mAvatarDrawable);
                mSendImageButton.setBackground(null);
            }
        }
    }
}
