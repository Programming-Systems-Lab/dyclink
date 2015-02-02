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

package jmbench.interfaces;

import org.ejml.data.DenseMatrix64F;

import java.io.Serializable;


/**
 * <p>
 * An interface implemented for each benchmarked library that is used to measure
 * the library's runtime performance.
 * </p>
 * <p>
 * NOTE: Not all of these operations are currently being benchmarked in the publicly released
 * results.  It still is a good idea to implement them all.
 * </p>
 * @author Peter Abeles
 */
public interface RuntimePerformanceFactory extends LibraryFactory , MatrixFactory , Serializable  {

    /**
     * Cholesky decomposition
     */
    AlgorithmInterface chol();

    /**
     * LU decomposition
     */
    AlgorithmInterface lu();

    /**
     * Singular Value Decomposition
     */
    AlgorithmInterface svd();

    /**
     * QR Decomposition
     */
    AlgorithmInterface qr();

    /**
     * Eigenvalue Decomposition
     */
    AlgorithmInterface eigSymm();

    // should it test against asymmetric matrices?
//    AlgorithmInterface eigASymm();


    /**
     * Computes the determinant of a matrix.
     */
    AlgorithmInterface det();

    /**
     * Inverts a square matrix.
     */
    AlgorithmInterface invert();

    /**
     * Inverts a square positive definite matrix.
     */
    AlgorithmInterface invertSymmPosDef();

    /**
     * <p>
     * Matrix addition :<br>
     * <br>
     * C = A + B
     * </p>
     */
    AlgorithmInterface add();

    /**
     * <p>
     * Matrix multiplication :<br>
     * <br>
     * C = A*B
     * </p>
     */
    AlgorithmInterface mult();

    /**
     * <p>
     * Matrix multiplication where B is transposed:<br>
     * <br>
     * C = A*B^T
     * </p>
     */
    AlgorithmInterface multTransB();

    /**
     * <p>
     * Multiplies each element in the matrix by a constant value.<br>
     * <br>
     * b<sub>i,j</sub> = &gamma;a<sub>i,j</sub>
     * </p>
     */
    AlgorithmInterface scale();

    /**
     * Solve a system with square input matrix:<br>
     * <br>
     * A*X = B<br>
     * <br>
     * where A is an m by m matrix.
     */
    AlgorithmInterface solveExact();

    /**
     * Solve a system with a "tall" input matrix:<br>
     * <br>
     * A*X = B<br>
     * <br>
     * where A is an m by n matrix and m > n.
     */
    AlgorithmInterface solveOver();

    /**
     * Matrix transpose
     */
    AlgorithmInterface transpose();

    BenchmarkMatrix convertToLib( DenseMatrix64F input );

    DenseMatrix64F convertToEjml( BenchmarkMatrix input );
}
