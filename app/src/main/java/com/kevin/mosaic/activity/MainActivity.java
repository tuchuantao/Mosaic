package com.kevin.mosaic.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.kevin.mosaic.R;
import com.kevin.mosaic.util.FileUtils;
import com.kevin.mosaic.widget.MosaicView;

import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MosaicView.HandleListener, RadioGroup.OnCheckedChangeListener {

    private final static int SELECT_PHOTO = 123;
    private final static int REQUEST_PERMISSION_REQUEST_CODE = 321;

    private MosaicView mosaicView;
    private ImageView lastBtn;
    private ImageView nextBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mosaicView = (MosaicView) findViewById(R.id.mosaic_view);
        mosaicView.setDrawingCacheEnabled(true);

        lastBtn = (ImageView) findViewById(R.id.last);
        lastBtn.setOnClickListener(this);
        nextBtn = (ImageView) findViewById(R.id.next);
        nextBtn.setOnClickListener(this);
        RadioButton defaultChecked = (RadioButton) findViewById(R.id.paint_size_level_three);
        defaultChecked.setChecked(true);
        RadioGroup paintSizeGroup = (RadioGroup) findViewById(R.id.paint_size_level);
        paintSizeGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_act, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_photo:
                selectPhoto();
                break;
            case R.id.save_photo:
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissionIfNeed();
                } else  {
                    savePhoto();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectPhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, SELECT_PHOTO);
    }

    private void savePhoto() {
        Bitmap bitmap = mosaicView.createViewBitmap();
        if (null != bitmap) {
            String filePath = FileUtils.saveFile(MainActivity.this, bitmap, System.currentTimeMillis() + ".png", Environment.getExternalStorageDirectory() + "/mosaic/");
            Toast.makeText(MainActivity.this, TextUtils.isEmpty(filePath) ? "save photo is failed..." : "successed", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPermissionIfNeed() {
        int checkPermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (checkPermission == PackageManager.PERMISSION_GRANTED) { // 判断是否获得相应权限
            savePhoto();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_REQUEST_CODE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                savePhoto();
            } else {
                Toast.makeText(this,"拒绝授权，请授权",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PHOTO) {
                Uri uri = data.getData();
                ContentResolver cr = this.getContentResolver();
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                    mosaicView.initImg(bitmap, MainActivity.this);
                } catch (FileNotFoundException e) {
                    android.util.Log.e("Exception", e.getMessage(),e);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.last:
                mosaicView.callbackLastState();
                break;
            case R.id.next:
                mosaicView.recoverNextState();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mosaicView) {
            mosaicView.clear();
            mosaicView = null;
        }
    }

    @Override
    public void state(boolean canCallback, boolean canRecover) {
        lastBtn.setClickable(canCallback);
        lastBtn.setBackgroundResource(canCallback ? R.drawable.icon_mosaic_last_selector : R.drawable.icon_mosaic_last_pressed);
        nextBtn.setClickable(canRecover);
        nextBtn.setBackgroundResource(canRecover ? R.drawable.icon_mosaic_next_selector : R.drawable.icon_mosaic_next_pressed);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (checkedId) {
            case R.id.paint_size_level_one:
                mosaicView.setBrushworkSize(0.75F);
                break;
            case R.id.paint_size_level_two:
                mosaicView.setBrushworkSize(1F);
                break;
            case R.id.paint_size_level_three:
                mosaicView.setBrushworkSize(1.5F);
                break;
            case R.id.paint_size_level_four:
                mosaicView.setBrushworkSize(2F);
                break;
        }
    }
}
