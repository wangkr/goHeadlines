package com.cqyw.goheadlines;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.cqyw.goheadlines.config.Constant;
import com.cqyw.goheadlines.util.BitmapLoader;
import com.cqyw.goheadlines.util.FileUtils;
import com.cqyw.goheadlines.util.Logger;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXImageObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by Kairong on 2015/9/22.
 * mail:wangkrhust@gmail.com
 */
public class ShareActivity extends Activity implements RadioButton.OnCheckedChangeListener,View.OnClickListener{
    private Uri wmPath;
    private FileUtils fileUtils;
    private Bitmap showImage;
    private Button shareBtn;
    private RadioButton weibo,pengyouquan,qzone;
    private ImageView showImageView;
    private int checkedTxtColor,uncheckedTxtColor;
    private String sharePlatform;
    private int showWidth = 360,showHeight = 480;

    private IWXAPI iwxapi;

    private boolean isFirstCheck = true;
    private boolean isSaved = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        fileUtils = new FileUtils(this);

        iwxapi = WXAPIFactory.createWXAPI(this,Constant.WX_APP_ID,true);
        iwxapi.registerApp(Constant.WX_APP_ID);

        wmPath = getIntent().getParcelableExtra(Constant.WM_IMAGE_URI);
        showImage = BitmapLoader.decodeSampledBitmapFromUri(getContentResolver(), wmPath, showWidth, showHeight);

        checkedTxtColor = getResources().getColor(R.color.text_dark);
        uncheckedTxtColor = getResources().getColor(R.color.hint_color);

        initViews();

        showImageView.setImageBitmap(showImage);

        System.gc();
    }
    private void initViews(){
        weibo = (RadioButton) findViewById(R.id.weibo);
        pengyouquan = (RadioButton) findViewById(R.id.pengyouquan);
        qzone = (RadioButton) findViewById(R.id.qzone);

        weibo.setOnCheckedChangeListener(this);
        pengyouquan.setOnCheckedChangeListener(this);
        qzone.setOnCheckedChangeListener(this);

        shareBtn = (Button)findViewById(R.id.share);

        showImageView = (ImageView)findViewById(R.id.wm_image);

        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);
        shareBtn.setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isFirstCheck) {
            // 只第一次改变背景为绿色
            shareBtn.setBackgroundResource(R.drawable.green_rounded_bg);
            isFirstCheck = false;
        }
        switch (buttonView.getId()){
            case R.id.weibo:
                weibo.setTextColor(isChecked?checkedTxtColor:uncheckedTxtColor);
                if(isChecked){
                    sharePlatform = Constant.WEIBO;
                }
                break;
            case R.id.pengyouquan:
                pengyouquan.setTextColor(isChecked?checkedTxtColor:uncheckedTxtColor);
                if(isChecked){
                    sharePlatform = Constant.WEIXIN;
                }
                break;
            case R.id.qzone:
                qzone.setTextColor(isChecked?checkedTxtColor:uncheckedTxtColor);
                if(isChecked){
                    sharePlatform = Constant.QZONE;
                }
                break;
            default:
                break;
        }

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
                String filename = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())+".jpg";
                Bitmap wmBitmap = BitmapFactory.decodeFile(wmPath.getPath());
                try {
                    Toast.makeText(this,"保存成功:"+fileUtils.savaBitmap(filename, wmBitmap),Toast.LENGTH_LONG).show();
                    wmBitmap.recycle();
                    wmBitmap = null;
                    isSaved = true;
                }catch (IOException e){
                    e.printStackTrace();
                }
                break;
            case R.id.share:
                if(isFirstCheck||wmPath.getPath()==null||wmPath.getPath().equals("")){
                    return;
                }
                Toast.makeText(this,sharePlatform,Toast.LENGTH_SHORT).show();
                shareToSocialize(sharePlatform,wmPath.getPath());
                break;
            default:
                break;

        }
    }
    private void shareToSocialize(String paltform,String imagePath){
        if(paltform.equals(Constant.WEIXIN)){

            WXImageObject imgObj = new WXImageObject();
            imgObj.setImagePath(imagePath);

            WXMediaMessage msg = new WXMediaMessage();
            msg.mediaObject = imgObj;

            Bitmap bmp = BitmapFactory.decodeFile(imagePath);
            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, showWidth, showHeight, true);
            bmp.recycle();
            msg.thumbData = BitmapLoader.bmpToByteArray(thumbBmp, true);

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = buildTransaction("img");
            req.message = msg;
            req.scene =  SendMessageToWX.Req.WXSceneTimeline;
            iwxapi.sendReq(req);
        }
    }
    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
}
