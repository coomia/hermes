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

package io.hermes.util.concurrent;

import io.hermes.HermesException;
import io.hermes.HermesInterruptedException;
import java.util.concurrent.ExecutionException;

/**
 * @author spancer.ray
 */
public class Futures {

  private Futures() {

  }

  public static HermesException convert(Exception e) {
    if (e instanceof ExecutionException) {
      if (e.getCause() instanceof HermesException) {
        return (HermesException) e.getCause();
      }
      return new UncategorizedExecutionException(e.getCause().getMessage(), e.getCause());
    }
    if (e instanceof InterruptedException) {
      return new HermesInterruptedException(e.getMessage(), e.getCause());
    }
    if (e instanceof HermesException) {
      return (HermesException) e;
    }
    return new UncategorizedExecutionException(e.getMessage(), e);
  }
}
