/*
 * Copyright (c) 2009-2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of JMatrixBenchmark.
 *
 * JMatrixBenchmark is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * JMatrixBenchmark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JMatrixBenchmark.  If not, see <http://www.gnu.org/licenses/>.
 */

package jmbench.impl.wrapper;

import jmbench.interfaces.BenchmarkMatrix;
import org.la4j.matrix.Matrix;
import org.la4j.vector.Vector;

/**
 * @author Peter Abeles
 * @author Vladimir Kostyukov
 */
public class La4jBenchmarkMatrix implements BenchmarkMatrix  {

    Matrix matrix;

    public static Vector toVector(Matrix m) {
        return m.toColumnVector();
    }

    public static Matrix toMatrix(Vector v) {
        return v.toColumnMatrix();
    }
    
    public La4jBenchmarkMatrix(Matrix matrix) {
        this.matrix = matrix;
    }

    @Override
    public double get(int row, int col) {
        return matrix.get(row, col);
    }

    @Override
    public void set(int row, int col, double value) {
        matrix.set(row, col, value);
    }

    @Override
    public int numRows() {
        return matrix.rows();
    }

    @Override
    public int numCols() {
        return matrix.columns();
    }

    @Override
    public <T> T getOriginal() {
        return (T)matrix;
    }
}
