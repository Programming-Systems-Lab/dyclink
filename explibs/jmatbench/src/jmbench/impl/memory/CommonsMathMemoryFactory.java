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

import jmbench.impl.wrapper.CommonsMathBenchmarkMatrix;
import jmbench.interfaces.BenchmarkMatrix;
import jmbench.interfaces.MemoryFactory;
import jmbench.interfaces.MemoryProcessorInterface;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.MathUtils;


/**
 * @author Peter Abeles
 */
public class CommonsMathMemoryFactory implements MemoryFactory {

    @Override
    public BenchmarkMatrix create(int numRows, int numCols) {
        return wrap(MatrixUtils.createRealMatrix(numRows, numCols));
    }

    @Override
    public BenchmarkMatrix wrap(Object matrix) {
        return new CommonsMathBenchmarkMatrix( (RealMatrix)matrix );
    }

    @Override
    public MemoryProcessorInterface invertSymmPosDef() {
        return new InvSymmPosDef();
    }

    public static class InvSymmPosDef implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            RealMatrix A = inputs[0].getOriginal();

            for( int i = 0; i < numTrials; i++ ) {
                CholeskyDecomposition chol =  new CholeskyDecomposition(A);
                chol.getSolver().getInverse();
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
            RealMatrix A = inputs[0].getOriginal();
            RealMatrix B = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ ) {
                A.multiply(B);
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
            RealMatrix A = inputs[0].getOriginal();
            RealMatrix B = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ ) {
                A.multiply(B.transpose());
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
            RealMatrix A = inputs[0].getOriginal();
            RealMatrix B = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ ) {
                A.add(B);
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
            RealMatrix A = inputs[0].getOriginal();
            RealMatrix y = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ ) {
                LUDecomposition lu = new LUDecomposition(A);
                lu.getSolver().solve(y);
            }

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
            RealMatrix A = inputs[0].getOriginal();
            RealMatrix y = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ ) {
                QRDecomposition qr = new QRDecomposition(A);
                qr.getSolver().solve(y);
            }
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
            RealMatrix A = inputs[0].getOriginal();

            RealMatrix U = null, S = null, V=null;
            for( int i = 0; i < numTrials; i++ ) {
                SingularValueDecomposition svd = new SingularValueDecomposition(A);
                // need to call this functions so that it performs the full decomposition
                U = svd.getU();
                S = svd.getS();
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
            RealMatrix A = inputs[0].getOriginal();

            RealMatrix V=null,D=null;
            for( int i = 0; i < numTrials; i++ ) {
                EigenDecomposition eig = new EigenDecomposition(A);
                // need to do this so that it computes the complete eigen vector
                V = eig.getV();
                D = eig.getD();
            }
            if( D == null || V == null)
                throw new RuntimeException("There is a null") ;
        }
    }
}