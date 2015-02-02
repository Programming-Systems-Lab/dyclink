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

package jmbench.interfaces;

import java.io.Serializable;


/**
 * Memory tests see how much memory it takes a library to perform the specified operation. For
 * each operation tested the inputs should be created and filled with random elements in a row
 * major fashion.
 *
 * @author Peter Abeles
 */
public interface MemoryFactory extends LibraryFactory, MatrixFactory, Serializable {

    public MemoryProcessorInterface mult();

    public MemoryProcessorInterface multTransB();

    public MemoryProcessorInterface add();

    public MemoryProcessorInterface solveEq();
    
    public MemoryProcessorInterface solveLS();

    public MemoryProcessorInterface invertSymmPosDef();

    public MemoryProcessorInterface svd();

    public MemoryProcessorInterface eig();
}
