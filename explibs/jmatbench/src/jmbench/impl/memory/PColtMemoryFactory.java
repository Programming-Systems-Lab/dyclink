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

package jmbench.impl.memory;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.algo.decomposition.DenseDoubleCholeskyDecomposition;
import cern.colt.matrix.tdouble.algo.decomposition.DenseDoubleEigenvalueDecomposition;
import cern.colt.matrix.tdouble.algo.decomposition.DenseDoubleSingularValueDecomposition;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import jmbench.impl.wrapper.PColtBenchmarkMatrix;
import jmbench.interfaces.BenchmarkMatrix;
import jmbench.interfaces.MemoryFactory;
import jmbench.interfaces.MemoryProcessorInterface;


/**
 * @author Peter Abeles
 */
public class PColtMemoryFactory implements MemoryFactory {

    @Override
    public BenchmarkMatrix create(int numRows, int numCols) {
        return wrap(new DenseDoubleMatrix2D(numRows,numCols));
    }

    @Override
    public BenchmarkMatrix wrap(Object matrix) {
        return new PColtBenchmarkMatrix((DoubleMatrix2D)matrix);
    }

    @Override
    public MemoryProcessorInterface invertSymmPosDef() {
        return new InvSymmPosDef();
    }

    public static class InvSymmPosDef implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix2D A = inputs[0].getOriginal();

            DenseDoubleAlgebra alg = new DenseDoubleAlgebra();

            for( int i = 0; i < numTrials; i++ ) {
                // can't decompose a matrix with the same decomposition algorithm
                DenseDoubleCholeskyDecomposition chol = alg.chol(A);

                DoubleMatrix2D result = DoubleFactory2D.dense.identity(A.rows());
                chol.solve(result);
            }
        }
    }

    @Override
    public MemoryProcessorInterface mult() {
        return new Mult();
    }

    public static class Mult implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix2D A = inputs[0].getOriginal();
            DoubleMatrix2D B = inputs[1].getOriginal();

            DenseDoubleAlgebra alg = new DenseDoubleAlgebra();

            for( int i = 0; i < numTrials; i++ ) {
                alg.mult(A,B);
            }
        }
    }

    @Override
    public MemoryProcessorInterface multTransB() {
        return new MultTransB();
    }

    public static class MultTransB implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix2D A = inputs[0].getOriginal();
            DoubleMatrix2D B = inputs[1].getOriginal();

            DoubleMatrix2D result = new DenseDoubleMatrix2D(A.rows(),B.rows());

            for( int i = 0; i < numTrials; i++ ) {
                result = A.zMult(B, result, 1, 0, false, true);
            }
        }
    }

    @Override
    public MemoryProcessorInterface add() {
        return new Add();
    }

    public static class Add implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix2D A = inputs[0].getOriginal();
            DoubleMatrix2D B = inputs[1].getOriginal();
            DoubleMatrix2D C = new DenseDoubleMatrix2D(A.rows(),A.columns());

            for( int i = 0; i < numTrials; i++ ) {
                C.assign(A);
                C.assign(B, DoubleFunctions.plus);
            }
        }
    }

    @Override
    public MemoryProcessorInterface solveEq() {
        return new SolveLinear();
    }

    public static class SolveLinear implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix2D A = inputs[0].getOriginal();
            DoubleMatrix2D y = inputs[1].getOriginal();

            DenseDoubleAlgebra alg = new DenseDoubleAlgebra();

            for( int i = 0; i < numTrials; i++ )
                alg.solve(A,y);
        }
    }

    @Override
    public MemoryProcessorInterface solveLS() {
        return new SolveLS();
    }

    public static class SolveLS implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix2D A = inputs[0].getOriginal();
            DoubleMatrix2D y = inputs[1].getOriginal();

            DenseDoubleAlgebra alg = new DenseDoubleAlgebra();

            for( int i = 0; i < numTrials; i++ )
                alg.solve(A,y);
        }
    }

    @Override
    public MemoryProcessorInterface svd() {
        return new SVD();
    }

    public static class SVD implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix2D A = inputs[0].getOriginal();

            DenseDoubleAlgebra alg = new DenseDoubleAlgebra();

            DoubleMatrix2D U=null,V=null,S=null;
            for( int i = 0; i < numTrials; i++ ) {
                DenseDoubleSingularValueDecomposition s = alg.svd(A);
                U = s.getU();
                S = s.getS();
                V = s.getV();
            }
            if( U == null || S == null || V == null )
                throw new RuntimeException("There is a null");
        }
    }

    @Override
    public MemoryProcessorInterface eig() {
        return new Eig();
    }

    public static class Eig implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix2D A = inputs[0].getOriginal();

            DenseDoubleAlgebra alg = new DenseDoubleAlgebra();

            DoubleMatrix2D D=null,V=null;
            for( int i = 0; i < numTrials; i++ ) {
                DenseDoubleEigenvalueDecomposition eig = alg.eig(A);

                D = eig.getD();
                V = eig.getV();
            }
            if( D == null || V == null)
                throw new RuntimeException("There is a null") ;
        }
    }
}