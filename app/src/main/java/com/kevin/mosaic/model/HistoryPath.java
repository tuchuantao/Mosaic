package com.kevin.mosaic.model;


import java.util.ArrayList;

public class HistoryPath {
	private ArrayList<MosaicPointEntity> points;

	public HistoryPath() {
		points = new ArrayList<>();
	}

	public ArrayList<MosaicPointEntity> getPoints() {
		return points;
	}

	public void setPoints(ArrayList<MosaicPointEntity> points) {
		this.points = points;
	}
}
