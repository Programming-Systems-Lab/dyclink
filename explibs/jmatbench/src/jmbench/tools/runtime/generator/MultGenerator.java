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

package jmbench.tools.runtime.generator;

import jmbench.interfaces.BenchmarkMatrix;
import jmbench.interfaces.MatrixFactory;
import jmbench.tools.OutputError;
import jmbench.tools.runtime.InputOutputGenerator;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.Random;

import static jmbench.misc.RandomizeMatrices.convertToEjml;
import static jmbench.misc.RandomizeMatrices.randomize;


/**
 * @author Peter Abeles
 */
public class MultGenerator implements InputOutputGenerator {

    DenseMatrix64F C;


    @Override
    public BenchmarkMatrix[] createInputs( MatrixFactory factory , Random rand ,
                                           boolean checkResults , int size ) {
        BenchmarkMatrix[] inputs = new  BenchmarkMatrix[2];

        inputs[0] = factory.create(size,size);
        inputs[1] = factory.create(size,size);

        randomize(inputs[0],-1,1,rand);
        randomize(inputs[1],-1,1,rand);

        if( checkResults ) {
            DenseMatrix64F A = convertToEjml(inputs[0]);
            DenseMatrix64F B = convertToEjml(inputs[1]);

            C = new DenseMatrix64F(A.numRows,A.numCols);
            CommonOps.mult(A,B,C);
        }

        return inputs;
    }

    @Override
    public OutputError checkResults(BenchmarkMatrix[] output, double tol) {
        return ResultsChecking.checkResult(convertToEjml(output[0]),C,tol);
    }

    @Override
    public int numOutputs() {
        return 1;
    }

    @Override
    public long getRequiredMemory( int matrixSize ) {
        return 8L*matrixSize*matrixSize*8L;
    }
}