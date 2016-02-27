package com.cqyw.goheadlines.share;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.cqyw.goheadlines.R;
import com.cqyw.goheadlines.config.Constant;
import com.cqyw.goheadlines.MonitoredActivity;
import com.cqyw.goheadlines.util.BitmapLoader;
import com.cqyw.goheadlines.util.FileUtils;
import com.umeng.analytics.MobclickAgent;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;


/**
 * Created by Kairong on 2015/9/22.
 * mail:wangkrhust@gmail.com
 */
public class ShareActivity extends MonitoredActivity implements RadioButton.OnCheckedChangeListener,View.OnClickListener/*,
        IWeiboHandler.Response,IWXAPIEventHandler*/{
    private Uri wmPath;
    private FileUtils fileUtils;
    private Bitmap showImage;
    private Button shareBtn;
    private RadioButton weibo,pengyouquan,qzone;
    private ImageView showImageView;
    private int checkedTxtColor,uncheckedTxtColor;
    private String sharePlatform;
    private int showWidth = 360,showHeight = 480;


    private boolean isFirstCheck = true;
    private boolean isSaved = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        fileUtils = new FileUtils(this);

        wmPath = getIntent().getParcelableExtra(Constant.WM_IMAGE_URI);
        showImage = BitmapLoader.decodeSampledBitmapFromUri(getContentResolver(), wmPath, showWidth, showHeight);

        checkedTxtColor = getResources().getColor(R.color.text_dark);
        uncheckedTxtColor = getResources().getColor(R.color.hint_color);

        initViews();

        showImageView.setImageBitmap(showImage);

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

    /**
     * @see {@link Activity#onNewIntent}
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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
                MobclickAgent.onEvent(this,Constant.stat_save_picture);
                break;
            case R.id.share:
                if(/*isFirstCheck||*/wmPath.getPath()==null||wmPath.getPath().equals("")){
                    return;
                }
                showShare(wmPath.getPath());
                break;
            default:
                break;

        }
    }

    private void showShare(String imagePath) {
        ShareSDK.initSDK(this);
        OnekeyShare oks = new OnekeyShare();
        //关闭sso授权
        oks.disableSSOWhenAuthorize();
        // 分享时Notification的图标和文字  2.5.9以后的版本不调用此方法
        //oks.setNotification(R.drawable.ic_launcher, getString(R.string.app_name));
        // title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间使用
        oks.setTitle("头条相机--我上头条啦");
        // titleUrl是标题的网络链接，仅在人人网和QQ空间使用
        oks.setTitleUrl("http://huanian.org/others.html");
        // text是分享文本，所有平台都需要这个字段
        oks.setText("");
        // imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
        oks.setImagePath(imagePath);//确保SDcard下面存在此张图片
        // url仅在微信（包括好友和朋友圈）中使用
//        oks.setUrl("http://huanian.org/others.html");
        // comment是我对这条分享的评论，仅在人人网和QQ空间使用
        oks.setComment("我上头条啦~");
        // site是分享此内容的网站名称，仅在QQ空间使用
        oks.setSite(getString(R.string.app_name));
        // siteUrl是分享此内容的网站地址，仅在QQ空间使用
//        oks.setSiteUrl("http://huanian.org/others.html");

        // 启动分享GUI
        oks.show(this);
    }
}
