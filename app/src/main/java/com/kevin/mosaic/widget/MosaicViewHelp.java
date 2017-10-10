package com.kevin.mosaic.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

public class MosaicViewHelp {

	/**
	 * 计算两点之间的角度
	 * @param lastX
	 * @param lastY
	 * @param x
	 * @param y
	 * @return
	 */
	public static float calculateOrientation(int lastX, int lastY, int x, int y) {
		double orientation = Math.atan2(y - lastY, x - lastX) / Math.PI * 180;
		return (float) orientation;
	}

	/**
	 * 计算两点之间的距离
	 * @param lastX
	 * @param lastY
	 * @param x
	 * @param y
	 * @return
	 */
	public static double calculateDistance(int lastX, int lastY, int x, int y) {
		double distanceX = Math.abs(lastX - x);
		double distanceY = Math.abs(lastY - y);
		return Math.sqrt(distanceX * distanceX + distanceY * distanceY);
	}

	/**
	 * 改变Bitmap的颜色、大小和方向
	 * @param baseBitmap
	 * @param filterColor
	 * @param scale
	 * @param rotate
	 * @return
	 */
	public static Bitmap createMosaicBitmap(Bitmap baseBitmap, int filterColor, float scale, float rotate) {
		Bitmap outBitmap = Bitmap.createBitmap (baseBitmap.getWidth(), baseBitmap.getHeight() , baseBitmap.getConfig());
		Canvas canvas = new Canvas(outBitmap);
		Paint paint = new Paint();
		paint.setColorFilter( new PorterDuffColorFilter(filterColor, PorterDuff.Mode.SRC_IN)) ;
		canvas.drawBitmap(baseBitmap , 0, 0, paint);

		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		matrix.postRotate(rotate);
		outBitmap = Bitmap.createBitmap(outBitmap, 0, 0, outBitmap.getWidth(), outBitmap.getHeight(), matrix, true);
		return outBitmap ;
	}
}
