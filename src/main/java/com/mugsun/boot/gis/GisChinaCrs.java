package com.mugsun.boot.gis;

/**
 * 中国地区 WGS84 ↔ GCJ-02 ↔ BD-09。存储仍是 WGS84；高德入出 GCJ-02，百度入出 BD-09。
 */
public final class GisChinaCrs {

	private static final double A = 6378245.0d;
	private static final double EE = 0.00669342162296594323d;
	private static final double X_PI = Math.PI * 3000.0d / 180.0d;

	private GisChinaCrs() {
	}

	public static boolean outOfChina(double lon, double lat) {
		return lon < 72.004d || lon > 137.8347d || lat < 0.8293d || lat > 55.8271d;
	}

	public static double[] toGcj02(double wgsLon, double wgsLat) {
		if (outOfChina(wgsLon, wgsLat)) {
			return new double[] { wgsLon, wgsLat };
		}
		double dLat = transformLat(wgsLon - 105.0d, wgsLat - 35.0d);
		double dLon = transformLon(wgsLon - 105.0d, wgsLat - 35.0d);
		double radLat = wgsLat / 180.0d * Math.PI;
		double magic = Math.sin(radLat);
		magic = 1 - EE * magic * magic;
		double sqrtMagic = Math.sqrt(magic);
		dLat = (dLat * 180.0d) / ((A * (1 - EE)) / (magic * sqrtMagic) * Math.PI);
		dLon = (dLon * 180.0d) / (A / sqrtMagic * Math.cos(radLat) * Math.PI);
		return new double[] { wgsLon + dLon, wgsLat + dLat };
	}

	/** 高德返回的 GCJ-02 → WGS84，供前端再按底图做显示投影。 */
	public static double[] toWgs84(double gcjLon, double gcjLat) {
		if (outOfChina(gcjLon, gcjLat)) {
			return new double[] { gcjLon, gcjLat };
		}
		double[] g = toGcj02(gcjLon, gcjLat);
		return new double[] { gcjLon * 2.0d - g[0], gcjLat * 2.0d - g[1] };
	}

	public static double[] toBd09(double wgsLon, double wgsLat) {
		if (outOfChina(wgsLon, wgsLat)) {
			return new double[] { wgsLon, wgsLat };
		}
		double[] gcj = toGcj02(wgsLon, wgsLat);
		return gcjToBd09(gcj[0], gcj[1]);
	}

	public static double[] bd09ToWgs84(double bdLon, double bdLat) {
		if (outOfChina(bdLon, bdLat)) {
			return new double[] { bdLon, bdLat };
		}
		double[] gcj = bd09ToGcj(bdLon, bdLat);
		return toWgs84(gcj[0], gcj[1]);
	}

	private static double[] gcjToBd09(double gcjLon, double gcjLat) {
		double z = Math.sqrt(gcjLon * gcjLon + gcjLat * gcjLat) + 0.00002d * Math.sin(gcjLat * X_PI);
		double theta = Math.atan2(gcjLat, gcjLon) + 0.000003d * Math.cos(gcjLon * X_PI);
		return new double[] { z * Math.cos(theta) + 0.0065d, z * Math.sin(theta) + 0.006d };
	}

	private static double[] bd09ToGcj(double bdLon, double bdLat) {
		double x = bdLon - 0.0065d;
		double y = bdLat - 0.006d;
		double z = Math.sqrt(x * x + y * y) - 0.00002d * Math.sin(y * X_PI);
		double theta = Math.atan2(y, x) - 0.000003d * Math.cos(x * X_PI);
		return new double[] { z * Math.cos(theta), z * Math.sin(theta) };
	}

	private static double transformLat(double x, double y) {
		double ret = -100.0d + 2.0d * x + 3.0d * y + 0.2d * y * y + 0.1d * x * y
			+ 0.2d * Math.sqrt(Math.abs(x));
		ret += (20.0d * Math.sin(6.0d * x * Math.PI) + 20.0d * Math.sin(2.0d * x * Math.PI)) * 2.0d / 3.0d;
		ret += (20.0d * Math.sin(y * Math.PI) + 40.0d * Math.sin(y / 3.0d * Math.PI)) * 2.0d / 3.0d;
		ret += (160.0d * Math.sin(y / 12.0d * Math.PI) + 320.0d * Math.sin(y * Math.PI / 30.0d)) * 2.0d / 3.0d;
		return ret;
	}

	private static double transformLon(double x, double y) {
		double ret = 300.0d + x + 2.0d * y + 0.1d * x * x + 0.1d * x * y
			+ 0.1d * Math.sqrt(Math.abs(x));
		ret += (20.0d * Math.sin(6.0d * x * Math.PI) + 20.0d * Math.sin(2.0d * x * Math.PI)) * 2.0d / 3.0d;
		ret += (20.0d * Math.sin(x * Math.PI) + 40.0d * Math.sin(x / 3.0d * Math.PI)) * 2.0d / 3.0d;
		ret += (150.0d * Math.sin(x / 12.0d * Math.PI) + 300.0d * Math.sin(x / 30.0d * Math.PI)) * 2.0d / 3.0d;
		return ret;
	}
}
