package com.example.zale.redpacketservice;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class MonitorService extends AccessibilityService {
    private ArrayList<AccessibilityNodeInfo> mNodeInfoList = new ArrayList<AccessibilityNodeInfo>();

    private boolean mLuckyClicked;
    private boolean mContainsLucky;
    private boolean mContainsOpenLucky;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();

        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            unlockScreen();
            mLuckyClicked = false;

            /**
             * for API >= 18, we use NotificationListenerService to detect the notifications
             * below API_18 we use AccessibilityService to detect
             */

            if (Build.VERSION.SDK_INT < 18) {
                Notification notification = (Notification) event.getParcelableData();
                List<String> textList = getText(notification);
                if (null != textList && textList.size() > 0) {
                    for (String text : textList) {
                        if (!TextUtils.isEmpty(text) && text.contains("[微信红包]")) {
                            final PendingIntent pendingIntent = notification.contentIntent;
                            try {
                                pendingIntent.send();
                            } catch (PendingIntent.CanceledException e) {
                            }
                            break;
                        }

                    }
                }
            }
        }

        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){
            Log.i("WINDOW_CONTENT_CHANGED", String.valueOf(event.getEventType()));
        }
        if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED){
            Log.i("TYPE_WINDOWS_CHANGED", String.valueOf(event.getEventType()));
        }

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {


            /**
             * 方式一：采用暴力根节点搜索法，直接将可见于不可见的节点的id都拿来遍历搜索
             * 此法能搜到不可见的内容
             */
//            AccessibilityNodeInfo nodeInfo = event.getSource();
//            if (null != nodeInfo) {
//                mNodeInfoList.clear();
//                traverseNode(nodeInfo);
//                if (mContainsLucky && !mLuckyClicked) {
//                    int size = mNodeInfoList.size();
//                    if (size > 0) {
//                        /** step1: get the last hongbao cell to fire click action */
//                        AccessibilityNodeInfo cellNode = mNodeInfoList.get(size - 1);
//                        cellNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                        mContainsLucky = false;
//                        mLuckyClicked = true;
//                    }
//                }
//                if (mContainsOpenLucky) {
//                    int size = mNodeInfoList.size();
//                    if (size > 0) {
//                        /** step2: when hongbao clicked we need to open it, so fire click action */
//                        AccessibilityNodeInfo cellNode = mNodeInfoList.get(size - 1);
//                        cellNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                        mContainsOpenLucky = false;
//                    }
//                }
//            }

            /**
             * 方式二：通过类名来获取事件，然后通过在遍历可见窗口的所有红包
             */
             System.out.println("TYPE_WINDOW_STATE_CHANGED --> "+event.getClassName());
            if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
                // 在聊天界面,去点中红包
                checkKey2();
            } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI"
                    .equals(event.getClassName())) {
                // 点中了红包，下一步就是去拆红包
                checkKey1();
            } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI"
                    .equals(event.getClassName())) {
                // 拆完红包后看详细的纪录界面

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                // ((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
            }

        }
    }


    /**
     * 方式一的遍历
     */
//    private void traverseNode(AccessibilityNodeInfo node) {
//        if (null == node) return;
//
//        Log.i("AccessibilityNodeInfo","viewIdName:"+node.getViewIdResourceName()+" "+
//        "text:"+node.getText()+" "+ node.isClickable());
//
//        final int count = node.getChildCount();
//        if (count > 0) {
//            for (int i = 0; i < count; i++) {
//                AccessibilityNodeInfo childNode = node.getChild(i);
//                traverseNode(childNode);
//            }
//        } else {
//            CharSequence text = node.getText();
//            if (null != text && text.length() > 0) {
//                String str = text.toString();
//                if (str.contains("领取红包")||str.contains("查看红包")) {
//                    mContainsLucky = true;
//                    AccessibilityNodeInfo cellNode = node.getParent().getParent().getParent().getParent();
//                    if (null != cellNode) mNodeInfoList.add(cellNode);
//                }
//
//                if (str.contains("拆红包")) {
//                    mContainsOpenLucky = true;
//                    mNodeInfoList.add(node);
//                }
//
//            }
//        }
//    }


    /**
     * 方式二的遍历，此法可能无法捕获不可见的红包
     */
    private void checkKey1() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {

            return;
        }
        List<AccessibilityNodeInfo> list = nodeInfo
                .findAccessibilityNodeInfosByText("拆红包"); // 获取包含 拆红包
        // 文字的控件，模拟点击事件，拆开红包
        for (AccessibilityNodeInfo n : list) {
            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private void checkKey2() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {

            return;
        }
        List<AccessibilityNodeInfo> list = nodeInfo
                .findAccessibilityNodeInfosByText("领取红包"); // 找到聊天界面中包含 领取红包
        // 字符的控件
        if (list.isEmpty()) {
//            list = nodeInfo.findAccessibilityNodeInfosByText("[微信红包]");
//            for (AccessibilityNodeInfo n : list) {
//
//                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                break;
//            }
        } else {
            // 最新的红包领起
            for (int i = list.size() - 1; i >= 0; i--) {
                AccessibilityNodeInfo parent = list.get(i).getParent().getParent().getParent().getParent();

                try {
                    // 调用performAction(AccessibilityNodeInfo.ACTION_CLICK)触发点击事件
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                    return;
                } catch (Exception e) {

                    e.printStackTrace();
                }

            }
        }

    }


    /**
     * 低版本android中获取通知状态栏中的text
     * @param notification
     * @return
     */
    public List<String> getText(Notification notification) {
        if (null == notification) return null;

        RemoteViews views = notification.bigContentView;
        if (views == null) views = notification.contentView;
        if (views == null) return null;

        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        List<String> text = new ArrayList<String>();
        try {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;

                // View ID
                parcel.readInt();

                String methodName = parcel.readString();
                if (null == methodName) {
                    continue;
                } else if (methodName.equals("setText")) {
                    // Parameter type (10 = Character Sequence)
                    parcel.readInt();

                    // Store the actual string
                    String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                    text.add(t);
                }
                parcel.recycle();
            }
        } catch (Exception e) {
        }

        return text;
    }

    /**
     * 点亮屏幕
     */
    private void unlockScreen() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");

        wakeLock.acquire();

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        final KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("MyKeyguardLock");
        keyguardLock.disableKeyguard();



    }

    @Override
    public void onInterrupt() {

    }
}
