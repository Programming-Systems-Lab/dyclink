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

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import Jama.SingularValueDecomposition;
import jmbench.impl.wrapper.JamaBenchmarkMatrix;
import jmbench.interfaces.BenchmarkMatrix;
import jmbench.interfaces.MemoryFactory;
import jmbench.interfaces.MemoryProcessorInterface;


/**
 * @author Peter Abeles
 */
public class JamaMemoryFactory implements MemoryFactory {

    @Override
    public BenchmarkMatrix create(int numRows, int numCols) {
        return wrap(new Matrix(numRows,numCols));
    }

    @Override
    public BenchmarkMatrix wrap(Object matrix) {
        return new JamaBenchmarkMatrix((Matrix)matrix);
    }

    @Override
    public MemoryProcessorInterface invertSymmPosDef() {
        return new InvSymmPosDef();
    }

    public static class InvSymmPosDef implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            Matrix A = inputs[0].getOriginal();

            int N = A.getColumnDimension();

            for( int i = 0; i < numTrials; i++ ){
                A.chol().solve(Matrix.identity(N,N));
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
            Matrix A = inputs[0].getOriginal();
            Matrix B = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ ){
                A.times(B);
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
            Matrix A = inputs[0].getOriginal();
            Matrix B = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ ){
                A.times(B.transpose());
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
            Matrix A = inputs[0].getOriginal();
            Matrix B = inputs[1].getOriginal();


            for( int i = 0; i < numTrials; i++ )
                A.plus(B);
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
            Matrix A = inputs[0].getOriginal();
            Matrix y = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ )
                A.solve(y);
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
            Matrix A = inputs[0].getOriginal();
            Matrix y = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ )
                A.solve(y);
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
            Matrix A = inputs[0].getOriginal();

            Matrix S=null,U=null,V=null;
            for( int i = 0; i < numTrials; i++ ) {
                SingularValueDecomposition svd = A.svd();

                S = svd.getS();
                U = svd.getU();
                V = svd.getV();
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
            Matrix A = inputs[0].getOriginal();

            Matrix D=null,V=null;
            for( int i = 0; i < numTrials; i++ ) {
                EigenvalueDecomposition eig = A.eig();

                D = eig.getD();
                V = eig.getV();
            }
            if( D == null || V == null)
                throw new RuntimeException("There is a null") ;
        }
    }
}