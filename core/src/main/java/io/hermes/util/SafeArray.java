/*******************************************************************************
 * Copyright 2021 spancer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package io.hermes.util;

/**
 * An array that is safe in terms of size.
 *
 * @author spancer.ray
 */
public interface SafeArray<T> {

  T get(int index);

  int size();

  void add(T value);

  void add(int index, T value);

  void clear();

  /**
   * Applies the procedure to each value in the list in ascending (front to back) order.
   *
   * @param procedure a <code>Procedure</code> value
   * @return true if the procedure did not terminate prematurely.
   */
  boolean forEach(Procedure<T> procedure);

  interface Procedure<T> {

    /**
     * Executes this procedure. A false return value indicates that the application executing this
     * procedure should not invoke this procedure again.
     *
     * @param value a value
     * @return true if additional invocations of the procedure are allowed.
     */
    boolean execute(T value);
  }
}
