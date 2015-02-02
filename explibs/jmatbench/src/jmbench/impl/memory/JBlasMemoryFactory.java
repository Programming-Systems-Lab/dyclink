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

import jmbench.impl.wrapper.JBlasBenchmarkMatrix;
import jmbench.interfaces.BenchmarkMatrix;
import jmbench.interfaces.MemoryFactory;
import jmbench.interfaces.MemoryProcessorInterface;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jblas.Singular;
import org.jblas.Solve;


/**
 * @author Peter Abeles
 */
public class JBlasMemoryFactory implements MemoryFactory {

    @Override
    public BenchmarkMatrix create(int numRows, int numCols) {
        return wrap(new DoubleMatrix(numRows,numCols));
    }

    @Override
    public BenchmarkMatrix wrap(Object matrix) {
        return new JBlasBenchmarkMatrix((DoubleMatrix)matrix);
    }

    @Override
    public MemoryProcessorInterface invertSymmPosDef() {
        return new InvSymmPosDef();
    }

    public static class InvSymmPosDef implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix A = inputs[0].getOriginal();

            DoubleMatrix I = DoubleMatrix.eye(A.getRows());

            for( int i = 0; i < numTrials; i++ )
                Solve.solvePositive(A,I);
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
            DoubleMatrix A = inputs[0].getOriginal();
            DoubleMatrix B = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ )
                A.mmul(B);
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
            DoubleMatrix A = inputs[0].getOriginal();
            DoubleMatrix B = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ )
                A.mmul(B.transpose());
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
            DoubleMatrix A = inputs[0].getOriginal();
            DoubleMatrix B = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ )
                A.add(B);
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
            DoubleMatrix A = inputs[0].getOriginal();
            DoubleMatrix y = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ )
                Solve.solve(A,y);
        }
    }

    @Override
    public MemoryProcessorInterface solveLS() {
        return null;
    }

    @Override
    public MemoryProcessorInterface svd() {
        return new SVD();
    }

    public static class SVD implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            DoubleMatrix A = inputs[0].getOriginal();

            DoubleMatrix[] s = null;
            for( int i = 0; i < numTrials; i++ ) {
                s = Singular.fullSVD(A);
            }
            if( s == null )
                throw new RuntimeException("there is a null");
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
            DoubleMatrix A = inputs[0].getOriginal();

            DoubleMatrix[] e = null;
            for( int i = 0; i < numTrials; i++ ) {
                e = Eigen.symmetricEigenvectors(A);
            }
            if( e == null )
                throw new RuntimeException("there is a null");
        }
    }
}