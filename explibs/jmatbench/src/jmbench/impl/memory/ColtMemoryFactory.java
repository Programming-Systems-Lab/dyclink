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

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.CholeskyDecomposition;
import cern.colt.matrix.linalg.EigenvalueDecomposition;
import cern.colt.matrix.linalg.SingularValueDecomposition;
import jmbench.impl.wrapper.ColtBenchmarkMatrix;
import jmbench.interfaces.BenchmarkMatrix;
import jmbench.interfaces.MemoryFactory;
import jmbench.interfaces.MemoryProcessorInterface;


/**
 * @author Peter Abeles
 */
public class ColtMemoryFactory implements MemoryFactory {

    @Override
    public BenchmarkMatrix create(int numRows, int numCols) {
        DenseDoubleMatrix2D mat = new DenseDoubleMatrix2D(numRows,numCols);

        return wrap(mat);
    }

    @Override
    public BenchmarkMatrix wrap(Object matrix) {
        return new ColtBenchmarkMatrix((DenseDoubleMatrix2D)matrix);
    }

    @Override
    public MemoryProcessorInterface invertSymmPosDef() {
        return new InvSymmPosDef();
    }

    public static class InvSymmPosDef implements MemoryProcessorInterface {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {

            DenseDoubleMatrix2D A = inputs[0].getOriginal();

            for( int i = 0; i < numTrials; i++ ) {
                CholeskyDecomposition chol = new CholeskyDecomposition(A);

                chol.solve(DoubleFactory2D.dense.identity(A.rows()));
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
            DenseDoubleMatrix2D A = inputs[0].getOriginal();
            DenseDoubleMatrix2D B = inputs[1].getOriginal();

            Algebra alg = new Algebra();

            for( int i = 0; i < numTrials; i++ ) {
                DoubleMatrix2D C = alg.mult(A,B);
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
            DenseDoubleMatrix2D A = inputs[0].getOriginal();
            DenseDoubleMatrix2D B = inputs[1].getOriginal();

            Algebra alg = new Algebra();

            DoubleMatrix2D result = new DenseDoubleMatrix2D(A.columns(),B.columns());

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
            DenseDoubleMatrix2D A = inputs[0].getOriginal();
            DenseDoubleMatrix2D B = inputs[1].getOriginal();
            DenseDoubleMatrix2D C = new DenseDoubleMatrix2D(A.rows(),A.columns());

            for( int i = 0; i < numTrials; i++ ) {
                C.assign(A);
                C.assign(B, cern.jet.math.Functions.plus);
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
            DenseDoubleMatrix2D A = inputs[0].getOriginal();
            DenseDoubleMatrix2D y = inputs[1].getOriginal();

            Algebra alg = new Algebra();

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
            DenseDoubleMatrix2D A = inputs[0].getOriginal();
            DenseDoubleMatrix2D y = inputs[1].getOriginal();

            Algebra alg = new Algebra();

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
            DenseDoubleMatrix2D A = inputs[0].getOriginal();

            DoubleMatrix2D U=null,S=null,V=null;
            for( int i = 0; i < numTrials; i++ ) {
                SingularValueDecomposition s = new SingularValueDecomposition(A);
                U = s.getU();
                S = s.getS();
                V = s.getV();
            }
            if( U == null || S == null || V == null)
                throw new RuntimeException("There is a null") ;
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
            DenseDoubleMatrix2D A = inputs[0].getOriginal();

            DoubleMatrix2D D=null, V=null;
            for( int i = 0; i < numTrials; i++ ) {
                EigenvalueDecomposition eig = new EigenvalueDecomposition(A);

                D = eig.getD();
                V = eig.getV();
            }
            if( D == null || V == null)
                throw new RuntimeException("There is a null") ;
        }
    }
}