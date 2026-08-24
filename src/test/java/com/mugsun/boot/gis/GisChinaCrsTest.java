package com.mugsun.boot.gis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 高德 GCJ-02 与 WGS84 互转：北京城区应有偏移，往返误差在米级。
 */
class GisChinaCrsTest {

	@Test
	void beijingRoundTripAndOffset() {
		double wgsLon = 116.391358;
		double wgsLat = 39.904966;
		double[] gcj = GisChinaCrs.toGcj02(wgsLon, wgsLat);
		assertThat(Math.hypot(gcj[0] - wgsLon, gcj[1] - wgsLat)).isGreaterThan(0.001d);
		double[] back = GisChinaCrs.toWgs84(gcj[0], gcj[1]);
		assertThat(back[0]).isCloseTo(wgsLon, within(1e-5));
		assertThat(back[1]).isCloseTo(wgsLat, within(1e-5));
	}

	@Test
	void outsideChinaUnchanged() {
		double[] gcj = GisChinaCrs.toGcj02(0, 0);
		assertThat(gcj).containsExactly(0d, 0d);
		assertThat(GisChinaCrs.toWgs84(10, 10)).containsExactly(10d, 10d);
	}

	@Test
	void beijingBd09RoundTrip() {
		double wgsLon = 116.391358;
		double wgsLat = 39.904966;
		double[] bd = GisChinaCrs.toBd09(wgsLon, wgsLat);
		assertThat(Math.hypot(bd[0] - wgsLon, bd[1] - wgsLat)).isGreaterThan(0.002d);
		double[] back = GisChinaCrs.bd09ToWgs84(bd[0], bd[1]);
		assertThat(back[0]).isCloseTo(wgsLon, within(1e-5));
		assertThat(back[1]).isCloseTo(wgsLat, within(1e-5));
	}
}
