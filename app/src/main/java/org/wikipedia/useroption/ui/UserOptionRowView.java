package org.wikipedia.useroption.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.database.async.AsyncConstant;
import org.wikipedia.useroption.database.UserOptionRow;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UserOptionRowView extends LinearLayout {
    @BindView(R.id.view_user_option_id) TextView id;
    @BindView(R.id.view_user_option_key) TextView key;
    @BindView(R.id.view_user_option_value) TextView value;
    @BindView(R.id.view_user_option_status) TextView status;
    @BindView(R.id.view_user_option_timestamp) TextView timestamp;
    @BindView(R.id.view_user_option_transaction_id) TextView transactionId;

    public UserOptionRowView(Context context) {
        super(context);
        init();
    }

    public UserOptionRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UserOptionRowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public UserOptionRowView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void set(long id, UserOptionRow row) {
        this.id.setText(String.valueOf(id));
        key.setText(row.key());
        value.setText(row.dat() == null ? "DEL" : row.dat().val());
        status.setText(row.status().toString());
        status.setVisibility(row.status().synced() ? GONE : VISIBLE);
        long age = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - row.timestamp());
        timestamp.setText(String.valueOf(age));
        timestamp.setVisibility(row.timestamp() == 0 ? GONE : VISIBLE);
        transactionId.setText(String.valueOf(row.transactionId()));
        transactionId.setVisibility(row.transactionId() == AsyncConstant.NO_TRANSACTION_ID ? GONE : VISIBLE);
    }

    private void init() {
        setGravity(Gravity.CENTER_VERTICAL);
        inflate(getContext(), R.layout.view_user_option_row, this);
        ButterKnife.bind(this);
    }
}
