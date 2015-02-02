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

import jmbench.impl.wrapper.La4jBenchmarkMatrix;
import jmbench.interfaces.BenchmarkMatrix;
import jmbench.interfaces.MemoryFactory;
import jmbench.interfaces.MemoryProcessorInterface;
import org.la4j.LinearAlgebra;
import org.la4j.decomposition.MatrixDecompositor;
import org.la4j.inversion.MatrixInverter;
import org.la4j.linear.LinearSystemSolver;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;

/**
 * @author Peter Abeles
 * @author Vladimir Kostyukov
 */
public class La4jMemoryFactory implements MemoryFactory {

    @Override
    public MemoryProcessorInterface svd() {
        return new SVD();
    }

    public static class SVD implements MemoryProcessorInterface {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            Matrix a = inputs[0].getOriginal();

            for( long i = 0; i < numTrials; i++ ) {
                MatrixDecompositor decompositor = a.withDecompositor(LinearAlgebra.SVD);
                decompositor.decompose();
            }
        }
    }

    @Override
    public MemoryProcessorInterface eig() {
        return new Eig();
    }

    public static class Eig implements MemoryProcessorInterface {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            Matrix a = inputs[0].getOriginal();

            for( long i = 0; i < numTrials; i++ ) {
                MatrixDecompositor decompositor = a.withDecompositor(LinearAlgebra.EIGEN);
                decompositor.decompose();
            }
        }
    }

    @Override
    public MemoryProcessorInterface invertSymmPosDef() {
        return new InvSymmPosDef();
    }

    public static class InvSymmPosDef implements MemoryProcessorInterface {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            Matrix a = inputs[0].getOriginal();

            for( long i = 0; i < numTrials; i++ ) {
                MatrixInverter inverter = a.withInverter(LinearAlgebra.INVERTER);
                inverter.inverse();
            }
        }
    }

    @Override
    public MemoryProcessorInterface add() {
        return new Add();
    }

    public static class Add implements MemoryProcessorInterface {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            Matrix a = inputs[0].getOriginal();
            Matrix b = inputs[1].getOriginal();

            for( long i = 0; i < numTrials; i++ ) {
                a.add(b);
            }
        }
    }

    @Override
    public MemoryProcessorInterface mult() {
        return new Mult();
    }

    public static class Mult implements MemoryProcessorInterface {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            Matrix a = inputs[0].getOriginal();
            Matrix b = inputs[1].getOriginal();

            for( long i = 0; i < numTrials; i++ ) {
                a.multiply(b);
            }
        }
    }

    @Override
    public MemoryProcessorInterface multTransB() {
        return new MulTranB();
    }

    public static class MulTranB implements MemoryProcessorInterface {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            Matrix a = inputs[0].getOriginal();
            Matrix b = inputs[1].getOriginal();

            for( long i = 0; i < numTrials; i++ ) {
                a.multiply(b.transpose());
            }
        }
    }

    @Override
    public MemoryProcessorInterface solveEq() {
        return new SmartSolve();
    }

    @Override
    public MemoryProcessorInterface solveLS() {
        return new SmartSolve();
    }

    public static class SmartSolve implements MemoryProcessorInterface {
        @Override
        public void process(BenchmarkMatrix[] inputs, BenchmarkMatrix[] outputs, long numTrials) {
            Matrix a = inputs[0].getOriginal();
            Vector b = La4jBenchmarkMatrix.toVector((Matrix) inputs[1].getOriginal());

            for( long i = 0; i < numTrials; i++ ) {
                LinearSystemSolver solver = a.withSolver(LinearAlgebra.SOLVER);
                solver.solve(b);
            }
        }
    }

    @Override
    public BenchmarkMatrix create(int numRows, int numCols) {
        return new La4jBenchmarkMatrix(new Basic2DMatrix(numRows, numCols));
    }

    @Override
    public BenchmarkMatrix wrap(Object matrix) {
        return new La4jBenchmarkMatrix((Matrix) matrix);
    }
}
