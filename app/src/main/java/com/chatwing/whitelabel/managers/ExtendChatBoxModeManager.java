package com.chatwing.whitelabel.managers;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.chatwing.whitelabel.Constants;
import com.chatwing.whitelabel.R;
import com.chatwing.whitelabel.activities.NoMenuWebViewActivity;
import com.chatwing.whitelabel.events.LoadOnlineUsersSuccessEvent;
import com.chatwingsdk.events.internal.CurrentChatBoxEvent;
import com.chatwingsdk.events.internal.UpdateSubscriptionEvent;
import com.chatwingsdk.managers.ChatboxModeManager;
import com.chatwingsdk.managers.CurrentChatBoxManager;
import com.chatwingsdk.managers.UserManager;
import com.chatwingsdk.pojos.ChatBox;
import com.chatwingsdk.validators.PermissionsValidator;
import com.readystatesoftware.viewbadger.BadgeView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

/**
 * Created by steve on 10/12/2014.
 */
public class ExtendChatBoxModeManager extends ChatboxModeManager {
    public static final int DRAWER_GRAVITY_ONLINE_USER = Gravity.RIGHT;

    private MenuItem mOnlineUsersItem;
    private BadgeView mOnlineUsersBadgeView;
    private ApiManager mApiManager;
    protected int mNumOfOnlineUser;
    protected Handler mRefreshOnlineUsersHandler;
    public static final int MSG_GET_ONLINE_USERS = 0;
    private static final long REFRESH_ONLINE_USERS_INTERVAL = 20 * DateUtils.SECOND_IN_MILLIS;


    public ExtendChatBoxModeManager(Bus bus, Delegate delegate,
                                    UserManager userManager,
                                    ApiManager apiManager,
                                    CurrentChatBoxManager currentChatBoxManager) {
        super(bus, delegate, userManager, apiManager, currentChatBoxManager);
        mApiManager = apiManager;
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        if (!mIsActive) {
            return;
        }

        if (mCurrentChatBoxManager.getCurrentChatBox() != null) {
            ((ExtendCurrentChatboxManager) mCurrentChatBoxManager).loadOnlineUsers();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.copy_alias:
                copyAliasCurrentChatBox();
                return true;
            case R.id.manage_blacklist:
                manageBlackList();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        ActionBarActivity activity = mActivityDelegate.getActivity();
        final DrawerLayout drawerLayout = mActivityDelegate.getDrawerLayout();
        activity.getMenuInflater().inflate(R.menu.chatbox_menu, menu);
        mOnlineUsersItem = menu.findItem(R.id.online_users);

        /**
         * Create badge view for online user item
         */
        ImageButton iconView = new ImageButton(activity, null,
                R.style.Widget_AppCompat_ActionButton);
        iconView.setImageDrawable(mOnlineUsersItem.getIcon());
        iconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(DRAWER_GRAVITY_ONLINE_USER)) {
                    drawerLayout.closeDrawer(DRAWER_GRAVITY_ONLINE_USER);
                } else {
                    drawerLayout.openDrawer(DRAWER_GRAVITY_ONLINE_USER);
                }
            }
        });

        // The badge view requires target view (iconView in this case)
        // to have a ViewGroup parent
        LinearLayout container = new LinearLayout(activity);
        container.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(iconView);

        Resources res = activity.getResources();
        mOnlineUsersBadgeView = new BadgeView(activity, iconView);
        mOnlineUsersBadgeView.setBadgePosition(BadgeView.POSITION_TOP_RIGHT);
        mOnlineUsersBadgeView.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                res.getDimension(R.dimen.badge_view_text_size));
        mOnlineUsersBadgeView.setBadgeMargin(res.getDimensionPixelSize(
                R.dimen.default_margin));

        mOnlineUsersItem.setActionView(container);

        return true;
    }

    @Override
    public void activate() {
        super.activate();
        setTitle(getString(R.string.title_chat_boxes));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean chatBoxesDrawerOpened = mActivityDelegate.getDrawerLayout().isDrawerOpen(DRAWER_GRAVITY_CHAT_BOXES);

        MenuItem shareChatBoxItem = menu.findItem(R.id.share_chat_box);
        MenuItem copyAliasItem = menu.findItem(R.id.copy_alias);
        MenuItem manageBlackListItem = menu.findItem(R.id.manage_blacklist);

        // Invalidate all menu related objects
        mOnlineUsersItem.setVisible(false);
        mOnlineUsersBadgeView.hide();
        shareChatBoxItem.setVisible(false);
        copyAliasItem.setVisible(false);
        manageBlackListItem.setVisible(false);

        // Now config them
        if (mCurrentChatBoxManager.getCurrentChatBox() != null) {
            // When main view or online users drawer is opened
            // and current chat box is available.
            mOnlineUsersItem.setVisible(true);
            shareChatBoxItem.setVisible(true  && Constants.ALLOW_SHARE_CHATBOX);
            if (mNumOfOnlineUser > 0) {
                mOnlineUsersBadgeView.setText(Integer.toString(mNumOfOnlineUser));
                mOnlineUsersBadgeView.show();
            }

            // Config share intent for share chat box item
            ActionBarActivity activity = mActivityDelegate.getActivity();
            ChatBox chatBox = mCurrentChatBoxManager.getCurrentChatBox();
            String shareText = mApiManager.getChatBoxUrl(chatBox.getKey());
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.message_share_chat_box_subject));
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            ShareActionProvider shareChatBoxActionProvider = (ShareActionProvider)
                    MenuItemCompat.getActionProvider(shareChatBoxItem);
            shareChatBoxActionProvider.setShareIntent(intent);


            if (chatBox.getAlias() != null && Constants.ALLOW_SHARE_CHATBOX) {
                copyAliasItem.setVisible(true);
            }
            if (mUserManager.userHasPermission(chatBox, PermissionsValidator.Permission.UNBLOCK_USER)) {
                manageBlackListItem.setVisible(true);
            }

        }

        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRefreshOnlineUsersHandler != null) {
            mRefreshOnlineUsersHandler.removeMessages(MSG_GET_ONLINE_USERS);
        }
    }

    @Subscribe
    public void onUpdateSubscriptionEvent(UpdateSubscriptionEvent event) {
        super.onUpdateSubscriptionEvent(event);
    }

    @Subscribe
    public void onLoadOnlineUsersSuccess(LoadOnlineUsersSuccessEvent event) {
        // Reload options menu to update number of onlines users
        mNumOfOnlineUser = event.getCount();
        invalidateOptionsMenu();
        // Recursively get online users.
        // Also, check and only post new message to the handler if
        // its queue is empty. That ensures that the update interval
        // is correct.
        if (mRefreshOnlineUsersHandler == null) {
            mRefreshOnlineUsersHandler = new Handler() {
                @Override
                public void handleMessage(android.os.Message msg) {
                    ((ExtendCurrentChatboxManager) mCurrentChatBoxManager).loadOnlineUsers();
                }
            };
        }
        if (!mRefreshOnlineUsersHandler.hasMessages(MSG_GET_ONLINE_USERS)) {
            android.os.Message msg = android.os.Message.obtain(
                    mRefreshOnlineUsersHandler, MSG_GET_ONLINE_USERS);
            mRefreshOnlineUsersHandler.sendMessageDelayed(msg,
                    REFRESH_ONLINE_USERS_INTERVAL);
        }
    }

    @com.squareup.otto.Subscribe
    public void onCurrentChatBoxChanged(com.chatwingsdk.events.internal.CurrentChatBoxEvent event) {
        CurrentChatBoxEvent.Status status = event.getStatus();
        ChatBox chatBox = event.getChatbox();
        if (status == CurrentChatBoxEvent.Status.REMOVED) {

            if (mRefreshOnlineUsersHandler != null) {
                mRefreshOnlineUsersHandler.removeMessages(MSG_GET_ONLINE_USERS);
            }
            mNumOfOnlineUser = 0;
        } else if (status == CurrentChatBoxEvent.Status.LOADED) {
            setTitle(chatBox.getName());
            if (chatBox.getAlias() != null) setSubTitle(constructAliasLink(chatBox.getAlias()));
            ((ExtendCurrentChatboxManager) mCurrentChatBoxManager).loadOnlineUsers();
        }

        super.onCurrentChatBoxChanged(event);
    }

    @com.squareup.otto.Subscribe
    public void onLoadCurrentChatBoxFailed(com.chatwingsdk.events.internal.LoadCurrentChatBoxFailedEvent event) {
        super.onLoadCurrentChatBoxFailed(event);
    }

    @com.squareup.otto.Subscribe
    public void onUserSelectedChatBox(com.chatwingsdk.events.internal.UserSelectedChatBoxEvent event) {
        super.onUserSelectedChatBox(event);
    }

    @Override
    public boolean isSecondaryDrawerOpening() {
        return mActivityDelegate.getDrawerLayout().isDrawerOpen(DRAWER_GRAVITY_ONLINE_USER);
    }

    public void closeSecondaryDrawer() {
        mActivityDelegate.getDrawerLayout().closeDrawer(DRAWER_GRAVITY_ONLINE_USER);
        mActivityDelegate.getActivity().invalidateOptionsMenu();
    }

    @TargetApi(11)
    private void copyAliasCurrentChatBox() {
        ChatBox chatBox = mCurrentChatBoxManager.getCurrentChatBox();
        if (chatBox == null || chatBox.getAlias() == null) {
            return;
        }
        ActionBarActivity activity = mActivityDelegate.getActivity();

        if (Build.VERSION.SDK_INT >= 11) {
            ClipboardManager clipboard = (ClipboardManager)
                    activity.getSystemService(activity.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("alias_copied",
                    constructAliasLink(chatBox.getAlias())));
        } else {
            android.text.ClipboardManager oldClipboard = (android.text.ClipboardManager)
                    activity.getSystemService(activity.CLIPBOARD_SERVICE);
            oldClipboard.setText(constructAliasLink(chatBox.getAlias()));
        }

        Toast.makeText(activity, R.string.message_current_chat_box_alias_copied,
                Toast.LENGTH_SHORT)
                .show();
    }

    private String constructAliasLink(String alias) {
        return Constants.CHATWING_BASE_URL + "/" + alias;
    }

    private void manageBlackList() {
        ActionBarActivity activity = mActivityDelegate.getActivity();
        Intent i = new Intent(activity, NoMenuWebViewActivity.class);
        i.putExtra(NoMenuWebViewActivity.EXTRA_URL, String.format(ApiManager.MANAGE_BLACKLIST_URL,
                mCurrentChatBoxManager.getCurrentChatBox().getKey(),
                mUserManager.getCurrentUser().getAccessToken()));
        activity.startActivity(i);
    }

}
