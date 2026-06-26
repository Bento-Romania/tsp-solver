package com.bento.tsp.service;

import com.bento.tsp.model.Coordinate;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HaversineUtilTest {

    @Test
    void samePointIsZero() {
        var p = new Coordinate(44.4268, 26.1025);
        assertThat(HaversineUtil.distanceMeters(p, p)).isEqualTo(0.0);
    }

    @Test
    void bucharestToClujIsApprox324km() {
        var bucharest = new Coordinate(44.4268, 26.1025);
        var cluj = new Coordinate(46.7712, 23.6236);
        assertThat(HaversineUtil.distanceMeters(bucharest, cluj))
                .isCloseTo(324_000, Offset.offset(5_000.0));
    }

    @Test
    void distanceIsSymmetric() {
        var a = new Coordinate(44.4268, 26.1025);
        var b = new Coordinate(46.7712, 23.6236);
        assertThat(HaversineUtil.distanceMeters(a, b))
                .isEqualTo(HaversineUtil.distanceMeters(b, a));
    }

    @Test
    void durationMatchesSpeedFormula() {
        var a = new Coordinate(44.4268, 26.1025);
        var b = new Coordinate(46.7712, 23.6236);
        double speedKmh = 20.0;
        double distM = HaversineUtil.distanceMeters(a, b);
        double expectedSec = distM / (speedKmh / 3.6);
        assertThat(HaversineUtil.durationSeconds(a, b, speedKmh))
                .isCloseTo(expectedSec, Offset.offset(0.001));
    }
}
