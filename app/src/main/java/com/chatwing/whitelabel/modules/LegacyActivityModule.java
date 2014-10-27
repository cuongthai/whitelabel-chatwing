package com.chatwing.whitelabel.modules;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;

import com.chatwing.whitelabel.LegacyLoginActivity;
import com.chatwing.whitelabel.fragments.AuthenticateFragment;
import com.chatwing.whitelabel.fragments.ForgotPasswordFragment;
import com.chatwing.whitelabel.fragments.GuestLoginFragment;
import com.chatwing.whitelabel.fragments.LoginFragment;
import com.chatwing.whitelabel.fragments.LoginTwitterFragment;
import com.chatwingsdk.modules.AuthenticateActivityModule;
import com.chatwingsdk.modules.ChatWingModule;
import com.chatwingsdk.modules.ForActivity;
import com.google.android.gms.plus.PlusClient;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;


/**
 * Created by cuongthai on 15/10/2014.
 */
@Module(
        injects = {
                LegacyLoginActivity.class,
                AuthenticateFragment.class,
                LoginFragment.class,
                LoginTwitterFragment.class,
                GuestLoginFragment.class,
                ForgotPasswordFragment.class
        },
        addsTo = ChatWingModule.class,
        overrides = true
)
public class LegacyActivityModule {

    private LegacyLoginActivity mActivity;

    public LegacyActivityModule(LegacyLoginActivity activity) {
        this.mActivity = activity;
    }
    @Provides
    @Singleton
    @ForActivity
    LayoutInflater provideLayoutInflater() {
        return (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Provides
    @Singleton
    Resources provideResources() {
        return mActivity.getResources();
    }

    @Provides
    @Singleton
    @ForActivity
    Context provideContext() {
        return mActivity;
    }

    @Provides
    @Singleton
    @ForActivity
    AccountManager provideAccountManager() {
        return AccountManager.get(mActivity);
    }

    @Provides
    @Singleton
    LoginFragment provideLoginFragment() {
        return new LoginFragment();
    }

    @Provides
    @Singleton
    PlusClient providePlusClient() {
        return new PlusClient.Builder(mActivity, mActivity, mActivity)
                .setVisibleActivities("http://schemas.google.com/AddActivity")
                .build();
    }

    @Provides
    @Singleton
    String[] provideAvatars(){
        return new String[]{
                "Aladie.png", "Amie.png"
                , "Andie.png", "Beetie.png", "Benie.png", "Billie.png"
                , "Bobie.png", "Bridie.png", "Castrie.png", "Chegie.png"
                , "Christie.png", "Cindie.png", "Clintie.png", "Cohie.png"
                , "Connie.png", "Corlie.png", "Croftie.png", "Dexie.png"
                , "Dukie.png", "Einie.png", "Elie.png", "Fishie.png"
                , "Fridie.png", "Hookie.png", "Indie.png", "Leeie.png"
                , "Leie.png", "Linkie.png", "Luckie.png", "Lukie.png"
                , "Madie3.png", "Maradie.png", "Powie.png", "Putie.png"
                , "Rockie.png"
        };
    }
}