/*
 
    Copyright IBM Corp. 2011, 2016
    This file is part of Anomaly Detection Engine for Linux Logs (ADE).

    ADE is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ADE is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ADE.  If not, see <http://www.gnu.org/licenses/>.
 
*/
package org.openmainframe.ade.core.matrix;

/** A vector of double values */
public abstract class AbstractDoubleVector implements IDoubleVector {

    @Override
    public void setAll(double val) {
        for (int i = 0; i < getLength(); ++i) {
            set(i, val);
        }
    }

    @Override
    public double sum() {
        double res = 0;
        for (int i = 0; i < getLength(); ++i) {
            res += get(i);
        }
        return res;
    }

    @Override
    public double normalize() {
        final double sum = sum();
        if (Double.isNaN(sum)) {
            return sum;
        }
        for (int i = 0; i < getLength(); ++i) {
            set(i, get(i) / sum);
        }
        return sum;
    }

    @Override
    public double max() {
        double res = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < getLength(); ++i) {
            final double v = get(i);
            if (v > res) {
                res = v;
            }
        }
        return res;
    }

    @Override
    public double min() {
        double res = Double.POSITIVE_INFINITY;
        for (int i = 0; i < getLength(); ++i) {
            final double v = get(i);
            if (v < res) {
                res = v;
            }
        }
        return res;
    }

    @Override
    public double mean() {
        return sum() / getLength();
    }

    @Override
    public void normalizeScore() {
        final double vecmean = mean();
        final double vecstd = std();
        for (int i = 0; i < getLength(); ++i) {
            set(i, (get(i) - vecmean) / vecstd);
        }
    }

    public double var() {
        double a = 0;
        double q = 0;
        final int n = getLength();
        for (int i = 0; i < n; ++i) {
            final double xi = get(i);
            final double tmp = (xi - a) * (xi - a);
            q += ((double) i / (double) (i + 1)) * (tmp);
            a += (1.0 / (i + 1)) * (xi - a);
        }
        if (n == 0) {
            return Double.NaN;
        }
        if (n == 1) {
            return 0;
        }
        return q / (n - 1);
    }

    public double std() {
        return Math.sqrt(var());
    }

}
