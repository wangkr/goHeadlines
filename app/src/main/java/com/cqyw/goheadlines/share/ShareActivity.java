package com.cqyw.goheadlines.share;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.cqyw.goheadlines.R;
import com.cqyw.goheadlines.config.Constant;
import com.cqyw.goheadlines.picture.MonitoredActivity;
import com.cqyw.goheadlines.util.BitmapLoader;
import com.cqyw.goheadlines.util.FileUtils;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by Kairong on 2015/9/22.
 * mail:wangkrhust@gmail.com
 */
public class ShareActivity extends MonitoredActivity implements RadioButton.OnCheckedChangeListener,View.OnClickListener{
    private Uri wmPath;
    private FileUtils fileUtils;
    private Bitmap showImage;
    private Button shareBtn;
//    private RadioButton weibo,pengyouquan,qzone;
    private ImageView showImageView;
//    private int checkedTxtColor,uncheckedTxtColor;
//    private String sharePlatform;
    private int showWidth = 360,showHeight = 480;

    /** 微信分享接口*/
//    private IWXAPI iwxapi;
    /** 微博分享接口 */
//    private IWeiboShareAPI mWeiboShareAPI;
    private boolean isInstalledWeibo;

    private boolean isFirstCheck = true;
    private boolean isSaved = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        fileUtils = new FileUtils(this);

//        iwxapi = WXAPIFactory.createWXAPI(this,Constant.WX_APP_ID,true);
//        iwxapi.registerApp(Constant.WX_APP_ID);

        wmPath = getIntent().getParcelableExtra(Constant.WM_IMAGE_URI);
        showImage = BitmapLoader.decodeSampledBitmapFromUri(getContentResolver(), wmPath, showWidth, showHeight);

//        checkedTxtColor = getResources().getColor(R.color.text_dark);
//        uncheckedTxtColor = getResources().getColor(R.color.hint_color);

        initViews();

        showImageView.setImageBitmap(showImage);

//        if (savedInstanceState != null) {
//            mWeiboShareAPI.handleWeiboResponse(getIntent(), this);
//        }
        System.gc();
    }
//    private void initWeibo(){
//        // 创建微博 SDK 接口实例
//        mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(this, Constants.APP_KEY);
//        // 注册
//        mWeiboShareAPI.registerApp();
//
//        // 获取微博客户端相关信息，如是否安装、支持 SDK 的版本
//        isInstalledWeibo = mWeiboShareAPI.isWeiboAppInstalled();
//
//
//    }
    private void initViews(){
//        weibo = (RadioButton) findViewById(R.id.weibo);
//        pengyouquan = (RadioButton) findViewById(R.id.pengyouquan);
//        qzone = (RadioButton) findViewById(R.id.qzone);
//
//        weibo.setOnCheckedChangeListener(this);
//        pengyouquan.setOnCheckedChangeListener(this);
//        qzone.setOnCheckedChangeListener(this);

        shareBtn = (Button)findViewById(R.id.share);

        showImageView = (ImageView)findViewById(R.id.wm_image);

        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);
        shareBtn.setOnClickListener(this);
    }

//    @Override
//    public void onResponse(BaseResponse baseResponse) {
//
//        switch (baseResponse.errCode){
//            case WBConstants.ErrorCode.ERR_OK:
//                Toast.makeText(this, "分享成功", Toast.LENGTH_LONG).show();
//                break;
//            case WBConstants.ErrorCode.ERR_FAIL:
//                Toast.makeText(this, "分享失败", Toast.LENGTH_LONG).show();
//                break;
//            case WBConstants.ErrorCode.ERR_CANCEL:
//                Toast.makeText(this,"分享取消",Toast.LENGTH_LONG).show();
//                break;
//        }
//    }

//    /**
//     * @see {@link Activity#onNewIntent}
//     */
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//
//        // 从当前应用唤起微博并进行分享后，返回到当前应用时，需要在此处调用该函数
//        // 来接收微博客户端返回的数据；执行成功，返回 true，并调用
//        // {@link IWeiboHandler.Response#onResponse}；失败返回 false，不调用上述回调
//        mWeiboShareAPI.handleWeiboResponse(intent, this);
//    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//        if(isFirstCheck) {
//            // 只第一次改变背景为绿色
//            shareBtn.setBackgroundResource(R.drawable.green_rounded_bg);
//            isFirstCheck = false;
//        }
//        switch (buttonView.getId()){
//            case R.id.weibo:
//                weibo.setTextColor(isChecked?checkedTxtColor:uncheckedTxtColor);
//                if(isChecked){
//                    sharePlatform = Constant.WEIBO;
//                }
//                break;
//            case R.id.pengyouquan:
//                pengyouquan.setTextColor(isChecked?checkedTxtColor:uncheckedTxtColor);
//                if(isChecked){
//                    sharePlatform = Constant.WEIXIN;
//                }
//                break;
//            case R.id.qzone:
//                qzone.setTextColor(isChecked?checkedTxtColor:uncheckedTxtColor);
//                if(isChecked){
//                    sharePlatform = Constant.QZONE;
//                }
//                break;
//            default:
//                break;
//        }

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.back:
                if(showImage!=null&&!showImage.isRecycled()){
                    showImage.recycle();
                    showImage = null;
                    System.gc();
                }
                finish();
                break;
            case R.id.save:
                if(isSaved){
                    Toast.makeText(this,"文件已保存成功!",Toast.LENGTH_SHORT).show();
                    return;
                }
                final String filename = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())+".jpg";
                Bitmap wmBitmap = BitmapFactory.decodeFile(wmPath.getPath());
                try {
                    Toast.makeText(this,"保存成功:"+fileUtils.savaBitmap(filename, wmBitmap),Toast.LENGTH_LONG).show();
                    wmBitmap.recycle();
                    System.gc();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isSaved = true;
                // 统计保存次数
                MobclickAgent.onEvent(this,Constant.stat_share_fun,Constant.stat_save_picture);
                break;
            case R.id.share:
//                if(isFirstCheck||wmPath.getPath()==null||wmPath.getPath().equals("")){
//                    return;
//                }
//                Toast.makeText(this,sharePlatform,Toast.LENGTH_SHORT).show();
//                shareToSocialize(sharePlatform,wmPath.getPath());

                // 统计分享次数
                MobclickAgent.onEvent(this,Constant.stat_share_fun,Constant.stat_share_picture);
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, wmPath);
                shareIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(shareIntent, "分享至"));
                break;
            default:
                break;

        }
    }
//    private void shareToSocialize(String paltform,String imagePath){
//        if(paltform.equals(Constant.WEIXIN)){
//
//            WXImageObject imgObj = new WXImageObject();
//            imgObj.setImagePath(imagePath);
//
//            WXMediaMessage msg = new WXMediaMessage();
//            msg.mediaObject = imgObj;
//
//            Bitmap bmp = BitmapFactory.decodeFile(imagePath);
//            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, showWidth, showHeight, true);
//            bmp.recycle();
//            msg.thumbData = BitmapLoader.bmpToByteArray(thumbBmp, true);
//
//            SendMessageToWX.Req req = new SendMessageToWX.Req();
//            req.transaction = buildTransaction("img");
//            req.message = msg;
//            req.scene =  SendMessageToWX.Req.WXSceneTimeline;
//            iwxapi.sendReq(req);
//        }
//    }
//    private String buildTransaction(final String type) {
//        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
//    }
}
