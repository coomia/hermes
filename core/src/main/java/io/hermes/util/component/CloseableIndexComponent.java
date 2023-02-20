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

package io.hermes.util.component;

import io.hermes.HermesException;

/**
 * @author spancer.ray
 */
public interface CloseableIndexComponent {

  /**
   * Closes the index component. A boolean indicating if its part of an actual index deletion or not
   * is passed.
   *
   * @param delete <tt>true</tt> if the index is being deleted.
   * @throws HermesException
   */
  void close(boolean delete) throws HermesException;
}
