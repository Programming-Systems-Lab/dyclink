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

import jmbench.impl.wrapper.OjAlgoBenchmarkMatrix;
import jmbench.interfaces.BenchmarkMatrix;
import jmbench.interfaces.MemoryFactory;
import jmbench.interfaces.MemoryProcessorInterface;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.matrix.decomposition.*;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PrimitiveDenseStore;
import org.ojalgo.matrix.store.TransposedStore;

/**
 * @author Peter Abeles
 * @author Anders Peterson (apete)
 */
public class OjAlgoMemoryFactory implements MemoryFactory {

    public static class Add implements MemoryProcessorInterface {

        @Override
        public void process(final BenchmarkMatrix[] inputs, final BenchmarkMatrix[] outputs, final long numTrials) {
            final MatrixStore<Double> A = inputs[0].getOriginal();
            final MatrixStore<Double> B = inputs[1].getOriginal();
            final PrimitiveDenseStore C = PrimitiveDenseStore.FACTORY.makeZero(A.countRows(), A.countColumns());

            for (int i = 0; i < numTrials; i++) {
                C.fillMatching(A, PrimitiveFunction.ADD, B);
            }
        }
    }

    public static class Eig implements MemoryProcessorInterface {

        @Override
        public void process(final BenchmarkMatrix[] inputs, final BenchmarkMatrix[] outputs, final long numTrials) {
            final MatrixStore<Double> A = inputs[0].getOriginal();

            final Eigenvalue<Double> eig = EigenvalueDecomposition.make(A);

            MatrixStore<Double> D = null, V = null;
            for (int i = 0; i < numTrials; i++) {
                if (!eig.compute(A)) {
                    throw new RuntimeException("Decomposition failed");
                }
                D = eig.getD();
                V = eig.getV();
            }
            if ((D == null) || (V == null)) {
                throw new RuntimeException("There is a null");
            }
        }
    }

    public static class Mult implements MemoryProcessorInterface {

        @Override
        public void process(final BenchmarkMatrix[] inputs, final BenchmarkMatrix[] outputs, final long numTrials) {
            final MatrixStore<Double> A = inputs[0].getOriginal();
            final MatrixStore<Double> B = inputs[1].getOriginal();
            final PrimitiveDenseStore C = PrimitiveDenseStore.FACTORY.makeZero(A.countRows(), B.countColumns());

            for (int i = 0; i < numTrials; i++) {
                C.fillByMultiplying(A, B);
            }
        }
    }

    public static class MultTransB implements MemoryProcessorInterface {

        @Override
        public void process(final BenchmarkMatrix[] inputs, final BenchmarkMatrix[] outputs, final long numTrials) {
            final MatrixStore<Double> A = inputs[0].getOriginal();
            final MatrixStore<Double> BT = new TransposedStore<Double>((MatrixStore<Double>) inputs[1].getOriginal());
            final PrimitiveDenseStore C = PrimitiveDenseStore.FACTORY.makeZero(A.countRows(), BT.countColumns());

            for (int i = 0; i < numTrials; i++) {
                C.fillByMultiplying(A, BT);
            }
        }
    }

    public static class OpCholInvertSymmPosDef implements MemoryProcessorInterface {

        @Override
        public void process(final BenchmarkMatrix[] inputs, final BenchmarkMatrix[] outputs, final long numTrials) {
            final MatrixStore<Double> A = inputs[0].getOriginal();

            final Cholesky<Double> chol = CholeskyDecomposition.make(A);

            final DecompositionStore<Double> tmpPreallocated = chol.preallocate(A, A);

            for (int i = 0; i < numTrials; i++) {
                if (!chol.compute(A)) {
                    throw new RuntimeException("Decomposition failed");
                }
                chol.getInverse(tmpPreallocated);
            }
        }
    }

    public static class SolveLinear implements MemoryProcessorInterface {

        @Override
        public void process(final BenchmarkMatrix[] inputs, final BenchmarkMatrix[] outputs, final long numTrials) {
            final MatrixStore<Double> A = inputs[0].getOriginal();
            final MatrixStore<Double> y = inputs[1].getOriginal();

            final LU<Double> lu = LUDecomposition.make(A);

            final DecompositionStore<Double> tmpPreallocated = lu.preallocate(A, y);

            for (int i = 0; i < numTrials; i++) {
                lu.compute(A);
                lu.solve(y, tmpPreallocated);
            }
        }
    }

    public static class SolveLS implements MemoryProcessorInterface {

        @Override
        public void process(final BenchmarkMatrix[] inputs, final BenchmarkMatrix[] outputs, final long numTrials) {
            final MatrixStore<Double> A = inputs[0].getOriginal();
            final MatrixStore<Double> y = inputs[1].getOriginal();

            final QR<Double> qr = QRDecomposition.make(A);

            final DecompositionStore<Double> tmpPreallocated = qr.preallocate(A, y);

            for (int i = 0; i < numTrials; i++) {
                qr.compute(A);
                qr.solve(y, tmpPreallocated);
            }
        }
    }

    public static class SVD implements MemoryProcessorInterface {

        @Override
        public void process(final BenchmarkMatrix[] inputs, final BenchmarkMatrix[] outputs, final long numTrials) {
            final MatrixStore<Double> A = inputs[0].getOriginal();

            final SingularValue<Double> svd = SingularValueDecomposition.make(A);

            MatrixStore<Double> U = null, S = null, V = null;
            for (int i = 0; i < numTrials; i++) {
                if (!svd.compute(A)) {
                    throw new RuntimeException("Decomposition failed");
                }
                U = svd.getQ1();
                S = svd.getD();
                V = svd.getQ2();
            }
            if ((U == null) || (S == null) || (V == null)) {
                throw new RuntimeException("There is a null");
            }
        }
    }

    @Override
    public MemoryProcessorInterface add() {
        return new Add();
    }

    @Override
    public BenchmarkMatrix create(final int numRows, final int numCols) {
        return this.wrap(PrimitiveDenseStore.FACTORY.makeZero(numRows, numCols));
    }

    @Override
    public MemoryProcessorInterface eig() {
        return new Eig();
    }

    @Override
    public MemoryProcessorInterface invertSymmPosDef() {
        return new OpCholInvertSymmPosDef();
    }

    @Override
    public MemoryProcessorInterface mult() {
        return new Mult();
    }

    @Override
    public MemoryProcessorInterface multTransB() {
        return new MultTransB();
    }

    @Override
    public MemoryProcessorInterface solveEq() {
        return new SolveLinear();
    }

    @Override
    public MemoryProcessorInterface solveLS() {
        return new SolveLS();
    }

    @Override
    public MemoryProcessorInterface svd() {
        return new SVD();
    }

    @Override
    public BenchmarkMatrix wrap(final Object matrix) {
        return new OjAlgoBenchmarkMatrix((MatrixStore<?>) matrix);
    }
}
