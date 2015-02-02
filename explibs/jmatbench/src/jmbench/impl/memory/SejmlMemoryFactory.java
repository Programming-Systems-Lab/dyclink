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

import jmbench.impl.wrapper.SejmlBenchmarkMatrix;
import jmbench.interfaces.BenchmarkMatrix;
import jmbench.interfaces.MemoryFactory;
import jmbench.interfaces.MemoryProcessorInterface;
import org.ejml.simple.SimpleMatrix;


/**
 * @author Peter Abeles
 */
public class SejmlMemoryFactory implements MemoryFactory {

    @Override
    public BenchmarkMatrix create(int numRows, int numCols) {
        return wrap( new SimpleMatrix(numRows,numCols));
    }

    @Override
    public BenchmarkMatrix wrap(Object matrix) {
        return new SejmlBenchmarkMatrix((SimpleMatrix)matrix);
    }

    @Override
    public MemoryProcessorInterface invertSymmPosDef() {
        return null;
    }

    @Override
    public MemoryProcessorInterface mult() {
        return new Mult();
    }

    public static class Mult implements MemoryProcessorInterface
    {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            SimpleMatrix A = inputs[0].getOriginal();
            SimpleMatrix B = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ )
                A.mult(B);
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
            SimpleMatrix A = inputs[0].getOriginal();
            SimpleMatrix B = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ )
                A.mult(B.transpose());
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
            SimpleMatrix A = inputs[0].getOriginal();
            SimpleMatrix B = inputs[1].getOriginal();

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
            SimpleMatrix A = inputs[0].getOriginal();
            SimpleMatrix y = inputs[1].getOriginal();

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
            SimpleMatrix A = inputs[0].getOriginal();
            SimpleMatrix y = inputs[1].getOriginal();

            for( int i = 0; i < numTrials; i++ )
                A.solve(y);
        }
    }

    @Override
    public MemoryProcessorInterface svd() {
        return null;
    }


    @Override
    public MemoryProcessorInterface eig() {
        return null;
    }

}