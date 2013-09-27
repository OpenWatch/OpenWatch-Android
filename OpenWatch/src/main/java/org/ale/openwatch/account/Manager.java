package org.ale.openwatch.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by davidbrodsky on 7/5/13.
 */
public class Manager {

    public void getAccountManagerToken(Activity act, String ACCOUNT_TYPE){
        AccountManager am = AccountManager.get(act);
        Account[] accts = am.getAccountsByType(ACCOUNT_TYPE);
        if(accts.length > 0) {
            Account acct = accts[0];
            am.getAuthToken(acct, "oauth"/*what goes here*/, null, act, new AccountManagerCallback<Bundle>() {

                @Override
                public void run(AccountManagerFuture<Bundle> arg0) {
                    try {
                        Bundle b = arg0.getResult();
                        Log.e("TrendDroid", "THIS AUTHTOKEN: " + b.getString(AccountManager.KEY_AUTHTOKEN));
                    } catch (Exception e) {
                        Log.e("TrendDroid", "EXCEPTION@AUTHTOKEN");
                    }
                }}, null);
        }
    }
}
