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

package jmbench.impl.runtime;

import jmbench.impl.wrapper.JBlasBenchmarkMatrix;
import jmbench.interfaces.AlgorithmInterface;
import jmbench.interfaces.BenchmarkMatrix;
import jmbench.interfaces.RuntimePerformanceFactory;
import jmbench.tools.runtime.generator.ScaleGenerator;
import org.ejml.data.DenseMatrix64F;
import org.jblas.*;


/**
 * @author Peter Abeles
 */
public class JBlasAlgorithmFactory implements RuntimePerformanceFactory {

    @Override
    public BenchmarkMatrix create(int numRows, int numCols) {
        return wrap(new DoubleMatrix(numRows,numCols));
    }

    @Override
    public BenchmarkMatrix wrap(Object matrix) {
        return new JBlasBenchmarkMatrix((DoubleMatrix)matrix);
    }

    @Override
    public AlgorithmInterface chol() {
        return new Chol();
    }

    public static class Chol implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();

            DoubleMatrix U = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                U = Decompose.cholesky(matA);
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(U.transpose());
            return elapsed;
        }
    }

    @Override
    public AlgorithmInterface lu() {
        return new LU();
    }

    public static class LU implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();

            DoubleMatrix L = null;
            DoubleMatrix U = null;
            DoubleMatrix P = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                Decompose.LUDecomposition<DoubleMatrix> lu = Decompose.lu(matA);
                L = lu.l;
                U = lu.u;
                P = lu.p;
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(L);
            outputs[1] = new JBlasBenchmarkMatrix(U);
            outputs[2] = new JBlasBenchmarkMatrix(P.transpose());
            return elapsed;
        }
    }

    @Override
    public AlgorithmInterface svd() {
        return new MySvd();
    }

    public static class MySvd implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();

            DoubleMatrix U = null;
            DoubleMatrix S = null;
            DoubleMatrix Vt = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                DoubleMatrix[] evd = Singular.fullSVD(matA);
                U = evd[0];
                S = evd[1];
                Vt = evd[2];
            }

            long elapsed = System.nanoTime()-prev;

            // S is a vector, need to convert into a matrix
            DoubleMatrix SM = new DoubleMatrix(U.getColumns(), Vt.getRows());
            for( int i = 0; i < S.rows; i++ )
                SM.put(i,i,S.get(i));

            outputs[0] = new JBlasBenchmarkMatrix(U);
            outputs[1] = new JBlasBenchmarkMatrix(SM);
            outputs[2] = new JBlasBenchmarkMatrix(Vt);
            return elapsed;
        }
    }


    @Override
    public AlgorithmInterface eigSymm() {
        return new MyEig();
    }

    public static class MyEig implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();

            DoubleMatrix D = null;
            DoubleMatrix V = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                DoubleMatrix[] evd = Eigen.symmetricEigenvectors(matA);
                D = evd[1];
                V = evd[0];
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(D);
            outputs[1] = new JBlasBenchmarkMatrix(V);
            return elapsed;
        }
    }

    @Override
    public AlgorithmInterface qr() {
        return null;
    }

    @Override
    public AlgorithmInterface det() {
        return null;
    }

    @Override
    public AlgorithmInterface invert() {
        return new Inv();
    }

    public static class Inv implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();

            DoubleMatrix I = DoubleMatrix.eye(matA.getRows());
            DoubleMatrix result = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                result = Solve.solve(matA,I);
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(result);
            return elapsed;
        }
    }

    @Override
    public AlgorithmInterface invertSymmPosDef() {
        return new InvSymmPosDef();
    }

    public static class InvSymmPosDef implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();

            DoubleMatrix I = DoubleMatrix.eye(matA.getRows());
            DoubleMatrix result = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                result = Solve.solvePositive(matA,I);
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(result);
            return elapsed;
        }
    }

    @Override
    public AlgorithmInterface add() {
        return new Add();
    }

    public static class Add implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();
            DoubleMatrix matB = inputs[1].getOriginal();

            DoubleMatrix result = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                result = matA.add(matB);
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(result);
            return elapsed;
        }
    }

    @Override
    public AlgorithmInterface mult() {
        return new Mult();
    }

    public static class Mult implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();
            DoubleMatrix matB = inputs[1].getOriginal();

            long prev = System.nanoTime();

            DoubleMatrix result = null;

            for( long i = 0; i < numTrials; i++ ) {
                result = matA.mmul(matB);
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(result);
            return elapsed;
        }
    }

    @Override
    public AlgorithmInterface multTransB() {
        return new MulTranB();
    }

    public static class MulTranB implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();
            DoubleMatrix matB = inputs[1].getOriginal();

            DoubleMatrix result = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                result = matA.mmul(matB.transpose());
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(result);
            return elapsed;
        }
    }

    @Override
    public AlgorithmInterface scale() {
        return new Scale();
    }

    public static class Scale implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();

            DoubleMatrix result = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                result = matA.mul(ScaleGenerator.SCALE);
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(result);
            return elapsed;
        }
    }

    @Override
    public AlgorithmInterface solveExact() {
        return new MySolve();
    }

    @Override
    public AlgorithmInterface solveOver() {
        return null;
    }

    public static class MySolve implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();
            DoubleMatrix matB = inputs[1].getOriginal();

            DoubleMatrix result = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                result = Solve.solve(matA,matB);
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(result);
            return elapsed;
        }
    }

    @Override
    public AlgorithmInterface transpose() {
        return new Transpose();
    }

    public static class Transpose implements AlgorithmInterface {
        @Override
        public long process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix matA = inputs[0].getOriginal();

            DoubleMatrix result = null;

            long prev = System.nanoTime();

            for( long i = 0; i < numTrials; i++ ) {
                result = matA.transpose();
            }

            long elapsed = System.nanoTime()-prev;
            outputs[0] = new JBlasBenchmarkMatrix(result);
            return elapsed;
        }
    }

    @Override
    public BenchmarkMatrix convertToLib(DenseMatrix64F input) {
        return new JBlasBenchmarkMatrix(convertToJBlas(input));
    }

    @Override
    public DenseMatrix64F convertToEjml(BenchmarkMatrix input) {
        DoubleMatrix orig = input.getOriginal();
        return jblasToEjml(orig);
    }

    public static DoubleMatrix convertToJBlas( DenseMatrix64F orig )
    {
        DoubleMatrix ret = new DoubleMatrix(orig.getNumRows(),orig.getNumCols());

        for( int i = 0; i < orig.numRows; i++ ) {
            for( int j = 0; j < orig.numCols; j++ ) {
                ret.put(i,j,orig.get(i,j)) ;
            }
        }

        return ret;
    }

    public static DenseMatrix64F jblasToEjml( DoubleMatrix orig )
    {
        if( orig == null )
            return null;

        DenseMatrix64F ret = new DenseMatrix64F(orig.getRows(),orig.getColumns());

        for( int i = 0; i < ret.numRows; i++ ) {
            for( int j = 0; j < ret.numCols; j++ ) {
                ret.set(i,j,orig.get(i,j));
            }
        }

        return ret;
    }
}