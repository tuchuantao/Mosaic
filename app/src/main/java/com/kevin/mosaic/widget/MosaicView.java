package com.kevin.mosaic.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ViewGroup;

import com.kevin.mosaic.R;
import com.kevin.mosaic.model.HistoryPath;
import com.kevin.mosaic.model.MosaicPointEntity;

import java.io.File;
import java.util.ArrayList;

public class MosaicView extends ViewGroup implements OnScaleGestureListener {

	private static final String TAG = MosaicView.class.getSimpleName();

	private Context context;
	private Bitmap mosaicBaseBitmap; // 马赛克笔触图片
	private Paint paint;
	private Paint changeLessPaint;
	private HandleListener listener;
	private String path;

	/**
	 * 缩放相关属性
	 */
	private ScaleGestureDetector mScaleGestureDetector;
	private float scaleFactor = 1.0f;
	private float maxScaleFactor;
	private Rect mInitImageRect;
	private boolean isMultiPointer = false;
	private int lastPointerCount = 0;
	private float mLastX;
	private float mLastY;
	private boolean isCanDrag;

	private Rect mImageRect;
	private float scaleRation = 1.5F; // 笔触的缩放率
	private float scale = 1F; // 背景图片的缩放率
	private Matrix scaleMatrix;

	private Bitmap bmBaseLayer; // 底下的背景图
	private Bitmap bmMosaicLayer;
	private Bitmap bmChangelessLayer; // 可以回退列表之前的所以马赛克

	// 图片原始宽高
	private int mImageWidth;
	private int mImageHeight;

	//防误触相关变量
	private long lastCheckDrawTime = 0;
	private boolean isCanDrawPath = false;

	private ArrayList<HistoryPath> canBackPaths; // 只保存最近的十次，如果回退，就是就此
	private ArrayList<HistoryPath> cancelPaths;

	public MosaicView(Context context) {
		this(context, null);
	}

	public MosaicView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
	}

	private void init() {
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		mImageRect = new Rect();
		mInitImageRect = new Rect();
		canBackPaths = new ArrayList<>();
		cancelPaths = new ArrayList<>();
		mosaicBaseBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mosaic_paint);
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setAlpha(255);
		changeLessPaint = new Paint();
		changeLessPaint.setAntiAlias(true);
		changeLessPaint.setAlpha(255);
		setWillNotDraw(false);
	}

	public void initImg(Bitmap bitmap, HandleListener listener) {
		if (null != bitmap) {
			this.listener = listener;
			int ratio = bitmap.getHeight() / getHeight();
			maxScaleFactor = ratio + 1;
			android.util.Log.d(TAG, "initImgByPath() ratio: " + ratio);
			if (ratio >= 5) {
				scale = 0.4F;
			} else if (ratio == 4) {
				scale = 0.5F;
			} else if (ratio == 3) {
				scale = 0.8F;
			}
			scaleMatrix = new Matrix();
			scaleMatrix.postScale(scale, scale);
			// 产生缩放后的Bitmap对象
			bmBaseLayer = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), scaleMatrix, true);
			bmMosaicLayer = null;

			mImageWidth = bmBaseLayer.getWidth();
			mImageHeight = bmBaseLayer.getHeight();

			requestLayout();
			invalidate();
		}
	}

	public void initImgByPath(String path, HandleListener listener) {
		this.path = path;
		File file = new File(path);
		if (file == null || !file.exists()) {
			android.util.Log.e(TAG, "invalid file path " + path);
			return;
		}
		Bitmap bitmap = BitmapFactory.decodeFile(path);
		initImg(bitmap, listener);
	}

	public void initMosaicBaseBitmap(int sourceId) {
		mosaicBaseBitmap = BitmapFactory.decodeResource(getResources(), sourceId);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (bmBaseLayer != null) {
			canvas.drawBitmap(bmBaseLayer, null, mImageRect, null);
		}
		if (bmMosaicLayer != null) {
			canvas.drawBitmap(bmMosaicLayer, null, mImageRect, null);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleGestureDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	public boolean dispatchTouchEvent(MotionEvent event) {
		super.dispatchTouchEvent(event);
		float pointerX = 0, pointerY = 0;

		int pointerCount = event.getPointerCount();
		//计算多个触摸点的平均值
		for (int i = 0; i < pointerCount; i++) {
			pointerX += event.getX(i);
			pointerY += event.getY(i);
		}
		pointerX = pointerX / pointerCount;
		pointerY = pointerY / pointerCount;
		if (pointerCount > 1) {
			isMultiPointer = true;
			//在多指模式，防误触变量重置
			isCanDrawPath = false;
			lastCheckDrawTime = 0;
		}
		if (lastPointerCount != pointerCount) {
			mLastX = pointerX;
			mLastY = pointerY;
			isCanDrag = false;
			lastPointerCount = pointerCount;
		}
		if (isMultiPointer) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE:
					if (pointerCount == 1) break;
					if (mImageRect.width() > mInitImageRect.width()) { //仅仅在放大的状态，图片才可移动
						int dx = (int) (pointerX - mLastX);
						int dy = (int) (pointerY - mLastY);
						if (!isCanDrag) isCanDrag = isCanDrag(dx, dy);
						if (isCanDrag) {
							if (mImageRect.left + dx > mInitImageRect.left) dx = mInitImageRect.left - mImageRect.left;
							if (mImageRect.right + dx < mInitImageRect.right) dx = mInitImageRect.right - mImageRect.right;
							if (mImageRect.top + dy > mInitImageRect.top) dy = mInitImageRect.top - mImageRect.top;
							if (mImageRect.bottom + dy < mInitImageRect.bottom) dy = mInitImageRect.bottom - mImageRect.bottom;
							mImageRect.offset(dx, dy);
						}
					}
					mLastX = pointerX;
					mLastY = pointerY;
					invalidate();
					break;
				case MotionEvent.ACTION_UP:
					lastPointerCount = 0;
					isMultiPointer = false;
					break;
			}
			return true;
		}
		//防误触
		if (!isCanDrawPath) {
			if (lastCheckDrawTime == 0) {
				lastCheckDrawTime = System.currentTimeMillis();
			}
			if (System.currentTimeMillis() - lastCheckDrawTime > 25) { //大于50ms为有效值
				isCanDrawPath = true;
				lastCheckDrawTime = System.currentTimeMillis();
			}
		}
		onMosaicEvent(event.getAction(), (int) event.getX(), (int) event.getY());
		return true;
	}

	private boolean isCanDrag(int dx, int dy) {
		return Math.sqrt((dx * dx) + (dy * dy)) >= 5.0f;
	}

	private void onMosaicEvent(int action, int x, int y) {
		if (mImageWidth <= 0 || mImageHeight <= 0) {
			return;
		}
		if (x < mImageRect.left || x > mImageRect.right || y < mImageRect.top || y > mImageRect.bottom) {
			action = MotionEvent.ACTION_UP;
		} else {
			float ratio = (mImageRect.right - mImageRect.left) / (float) mImageWidth;
			x = (int) ((x - mImageRect.left) / ratio);
			y = (int) ((y - mImageRect.top) / ratio);
			android.util.Log.v(TAG, "drawMosaic action: " + action + " ratio: " + ratio + "  x: " + x + "  y: " + y + "  bmBaseLayer.getHeight(): " + bmBaseLayer.getHeight() + " mosaicBaseBitmap.getHeight(): " + mosaicBaseBitmap.getHeight() + "  mosaicBaseBitmap.getWidth(): " + mosaicBaseBitmap.getWidth());
			if (x > bmBaseLayer.getWidth() - 10 || y > bmBaseLayer.getHeight() - 10) {
				action = MotionEvent.ACTION_UP;
			}
		}

		if (action != MotionEvent.ACTION_UP) {
			int color = bmBaseLayer.getPixel(x, y);
			MosaicPointEntity pointEntity = new MosaicPointEntity();
			pointEntity.setImgColor(color);
			pointEntity.setPointX(x);
			pointEntity.setPointY(y);
			pointEntity.setScaleRation(scaleRation);
			if (action == MotionEvent.ACTION_DOWN) {
				HistoryPath path = new HistoryPath();
				pointEntity.setRotate(0);
				path.getPoints().add(pointEntity);
				canBackPaths.add(path);
			} else if (action == MotionEvent.ACTION_MOVE) {
				android.util.Log.v(TAG, "drawMosaic ACTION_MOVE");
				if (canBackPaths.size() > 0 && (canBackPaths.get(canBackPaths.size() - 1).getPoints().size() == 1 || isCanDrawPath)) {
					HistoryPath path = canBackPaths.get(canBackPaths.size() - 1);
					MosaicPointEntity lastPoint = path.getPoints().get(path.getPoints().size() - 1);
					//if (MosaicViewHelp.calculateDistance(lastPoint.getPointX(), lastPoint.getPointY(), x, y) > (mosaicBtWidth / 2)) {
					if (x != lastPoint.getPointX() || y != lastPoint.getPointY()) {
						cancelPaths.clear();
						pointEntity.setRotate(MosaicViewHelp.calculateOrientation(lastPoint.getPointX(), lastPoint.getPointY(), x, y));
						path.getPoints().add(pointEntity);

						// 绘制
						if (bmMosaicLayer == null) {
							bmMosaicLayer = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_4444);
						}
						Canvas canvas = new Canvas(bmMosaicLayer);
						drawMosaic(canvas, pointEntity);
						invalidate();
					}
				}
			}
		} else {
			finishOnceDraw();
		}
		isCanDrawPath = false;
	}

	/**
	 * 一次绘画结束时调用
	 */
	private void finishOnceDraw() {
		lastCheckDrawTime = 0;
		// 去除无效HistoryPath
		if (canBackPaths.size() > 0 && canBackPaths.get(canBackPaths.size() - 1).getPoints().size() == 1) {
			canBackPaths.remove(canBackPaths.size() - 1);
		}
		// 将最近十次前的马赛克缓存在同一bitmap中
		if (canBackPaths.size() > 10) {
			HistoryPath path = canBackPaths.remove(0);
			if (bmChangelessLayer == null) {
				bmChangelessLayer = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_4444);
			}
			Canvas canvas = new Canvas(bmChangelessLayer);
			for (int i = 1; i < path.getPoints().size(); i++) {
				drawMosaic(canvas, path.getPoints().get(i));
			}
		}
		if (null != listener) {
			listener.state(canBackPaths.size() > 0, cancelPaths.size() > 0);
		}
	}

	/**
	 * 绘制单个马赛克，滑动或恢复时调用
	 * @param canvas
	 * @param pointEntity
	 */
	private void drawMosaic(Canvas canvas, MosaicPointEntity pointEntity) {
		Bitmap bitmap = MosaicViewHelp.createMosaicBitmap(mosaicBaseBitmap, pointEntity.getImgColor(), pointEntity.getScaleRation(), pointEntity.getRotate());
		// 产生缩放后的Bitmap对象
		bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), scaleMatrix, true);
		// 计算具体地址
		int x = pointEntity.getPointX() - bitmap.getHeight() / 2;
		int y = pointEntity.getPointY() - bitmap.getWidth() / 2;
		canvas.drawBitmap(bitmap, x, y, paint);
	}

	/**
	 * 绘制所有的马赛克，回退时调用
	 */
	private void drawMosaic() {
		if (canBackPaths.size() > 0) {
			bmMosaicLayer.recycle();
			bmMosaicLayer = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_4444);
			Canvas canvas = new Canvas(bmMosaicLayer);
			if (null != bmChangelessLayer) { // 绘制十次之前的
				canvas.drawBitmap(bmChangelessLayer, 0, 0, changeLessPaint);
			}
			for (HistoryPath path: canBackPaths) {
				if (path.getPoints().size() > 1) {
					for (int i = 1; i < path.getPoints().size(); i++) {
						drawMosaic(canvas, path.getPoints().get(i));
					}
				}
			}
		} else {
			bmMosaicLayer.recycle();
			bmMosaicLayer = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_4444);
			Canvas canvas = new Canvas(bmMosaicLayer);
			if (null != bmChangelessLayer) { // 绘制十次之前的
				canvas.drawBitmap(bmChangelessLayer, 0, 0, changeLessPaint);
			}
		}
	}


	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		finishOnceDraw();
		float scale = detector.getScaleFactor();
		scaleFactor *= scale;
		if (scaleFactor < 1.0f) {
			scaleFactor = 1.0f;
		}
		if (scaleFactor >= maxScaleFactor) scaleFactor = maxScaleFactor;

		if (mImageRect != null) {
			int addWidth = (int) (mInitImageRect.width() * scaleFactor) - mImageRect.width();
			int addHeight = (int) (mInitImageRect.height() * scaleFactor) - mImageRect.height();
			float centerWidthRatio = (detector.getFocusX() - mImageRect.left) / mImageRect.width();
			float centerHeightRatio = (detector.getFocusY() - mImageRect.left) / mImageRect.height();

			int leftAdd = (int) (addWidth * centerWidthRatio);
			int topAdd = (int) (addHeight * centerHeightRatio);

			mImageRect.left = mImageRect.left - leftAdd;
			mImageRect.right = mImageRect.right + (addWidth - leftAdd);
			mImageRect.top = mImageRect.top - topAdd;
			mImageRect.bottom = mImageRect.bottom + (addHeight - topAdd);
			checkCenterWhenScale();
		}
		invalidate();
		return true;
	}

	private void checkCenterWhenScale() {
		int deltaX = 0;
		int deltaY = 0;
		if (mImageRect.left > mInitImageRect.left) {
			deltaX = mInitImageRect.left - mImageRect.left;
		}
		if (mImageRect.right < mInitImageRect.right) {
			deltaX = mInitImageRect.right - mImageRect.right;
		}
		if (mImageRect.top > mInitImageRect.top) {
			deltaY = mInitImageRect.top - mImageRect.top;
		}
		if (mImageRect.bottom < mInitImageRect.bottom) {
			deltaY = mInitImageRect.bottom - mImageRect.bottom;
		}
		mImageRect.offset(deltaX, deltaY);
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (mImageWidth <= 0 || mImageHeight <= 0) {
			return;
		}
		int viewWidth = right - left;
		int viewHeight = bottom - top;
		float widthRatio = viewWidth / ((float) mImageWidth);
		float heightRatio = viewHeight / ((float) mImageHeight);
		float ratio = widthRatio < heightRatio ? widthRatio : heightRatio;
		int realWidth = (int) (mImageWidth * ratio);
		int realHeight = (int) (mImageHeight * ratio);

		int imageLeft = (viewWidth - realWidth) / 2;
		int imageTop = (viewHeight - realHeight) / 2;
		int imageRight = imageLeft + realWidth;
		int imageBottom = imageTop + realHeight;
		mImageRect.set(imageLeft, imageTop, imageRight, imageBottom);
		mInitImageRect.set(imageLeft, imageTop, imageRight, imageBottom);
	}

	/**
	 * 回退
	 */
	public void callbackLastState() {
		if (canBackPaths.size() > 0) {
			cancelPaths.add(canBackPaths.remove(canBackPaths.size() - 1));
			drawMosaic();
			invalidate();
		}
		if (null != listener) {
			listener.state(canBackPaths.size() > 0, cancelPaths.size() > 0);
		}
	}

	/**
	 * 恢复
	 */
	public void recoverNextState() {
		if (cancelPaths.size() > 0) {
			HistoryPath path = cancelPaths.remove(cancelPaths.size() - 1);
			if (path.getPoints().size() > 1) {
				canBackPaths.add(path);
				if (bmMosaicLayer == null) {
					bmMosaicLayer = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_4444);
				}
				Canvas canvas = new Canvas(bmMosaicLayer);
				for (int i = 1; i < path.getPoints().size(); i++) {
					drawMosaic(canvas, path.getPoints().get(i));
				}
				invalidate();
			}
		}
		if (null != listener) {
			listener.state(canBackPaths.size() > 0, cancelPaths.size() > 0);
		}
	}

	public void setBrushworkSize(float scale) {
		scaleRation = scale;
		// mosaicBtWidth = (int) (mosaicBaseBitmap.getWidth() * scale);
	}

	public interface HandleListener {
		void state(boolean canCallback, boolean canRecover);
	}

	public Bitmap createViewBitmap() {
		Bitmap bitmap = Bitmap.createBitmap((int)(mImageWidth / scale), (int)(mImageHeight / scale), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		// 判断是否有缩放
		if (scale != 1) {
			bmBaseLayer = BitmapFactory.decodeFile(path);
			scaleMatrix = new Matrix();
			scaleMatrix.postScale(1F / scale,  1F / scale);
			bmMosaicLayer = Bitmap.createBitmap(bmMosaicLayer, 0, 0, bmMosaicLayer.getWidth(), bmMosaicLayer.getHeight(), scaleMatrix, true);
		}
		canvas.drawBitmap(bmBaseLayer, 0, 0, null);
		canvas.drawBitmap(bmMosaicLayer, 0, 0, null);
		canvas.save();
		return bitmap;
	}

	/**
	 * 判断是否改变
	 * @return
	 */
	public boolean hasChange() {
		return null != bmChangelessLayer || canBackPaths.size() > 0;
	}

	public void clear() {
		if (null != bmBaseLayer) {
			bmBaseLayer.recycle();
			bmBaseLayer = null;
		}
		if (null != bmMosaicLayer) {
			bmMosaicLayer.recycle();
			bmMosaicLayer = null;
		}
		if (null != bmChangelessLayer) {
			bmChangelessLayer.recycle();
			bmChangelessLayer = null;
		}
		canBackPaths.clear();
		canBackPaths = null;
		cancelPaths.clear();
		cancelPaths.clear();
	}
}

