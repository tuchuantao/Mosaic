package com.kevin.mosaic.model;

public class MosaicPointEntity {
	// 坐标
	private int pointX;
	private int pointY;

	private float rotate; // 旋转角度
	private float scaleRation; // 缩放比例，画笔的粗细
	private int imgColor;

	public int getPointX() {
		return pointX;
	}

	public void setPointX(int pointX) {
		this.pointX = pointX;
	}

	public int getPointY() {
		return pointY;
	}

	public void setPointY(int pointY) {
		this.pointY = pointY;
	}

	public float getRotate() {
		return rotate;
	}

	public void setRotate(float rotate) {
		this.rotate = rotate;
	}

	public float getScaleRation() {
		return scaleRation;
	}

	public void setScaleRation(float scaleRation) {
		this.scaleRation = scaleRation;
	}

	public int getImgColor() {
		return imgColor;
	}

	public void setImgColor(int imgColor) {
		this.imgColor = imgColor;
	}
}
