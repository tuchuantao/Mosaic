package com.kevin.mosaic.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Administrator on 2017/10/10.
 */

public class FileUtils {

    public static String saveFile(Context context, Bitmap bm, String filename, String path) {
        File fileFolder;
        if (TextUtils.equals(Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED)) {
            fileFolder = new File(path);
        } else {
            fileFolder = context.getCacheDir();
        }
        if (!fileFolder.exists()) { // 如果目录不存在，则创建一个目录
            fileFolder.mkdir();
        }
        if (bm == null) {
            return "";
        }
        File myCaptureFile = new File(fileFolder + "/" + filename);
        BufferedOutputStream bos;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return myCaptureFile.getAbsolutePath();
    }
}
