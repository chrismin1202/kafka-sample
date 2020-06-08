/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chrism.util

import scala.reflect.ClassTag

object ClassUtils {

  /** Returns the fully-qualified name of the class.
    *
    * Note that Scala objects are compiled as 2 classes where one has the name of the object as is and
    * the other has "$" appended to the name.
    * When {{{getClass}}} is invoked on a Scala object, it returns the one with "$" appended.
    *
    * @param clazz an instance of [[Class]]
    * @return the fully-qualified name of the class
    */
  def classNameOf(clazz: Class[_]): String = {
    val rawName = clazz.getName
    if (rawName.endsWith("$")) rawName.substring(0, rawName.length - 1)
    else rawName
  }

  def nameEquals(clazz1: Class[_], clazz2: Class[_]): Boolean =
    (clazz1 == clazz2) || (clazz1.getName == clazz2.getName) || (classNameOf(clazz1) == classNameOf(clazz2))

  def fromName(className: String): Class[_] = Class.forName(className)

  /** Reflectively retrieves the instance of an {{{ object }}}.
    *
    * @param clazz the [[Class]] instance of the object to retrieve
    * @return the instance of the object as [[T]]
    * @throws NoSuchFieldException        thrown if a field with the specified name is not found
    * @throws NullPointerException        thrown if the given class and or name is null
    * @throws SecurityException           thrown if a security manager of type [[SecurityManager]], {{{ s }}},
    *                                     is present and the caller's class loader is not the same as or an ancestor of
    *                                     the class loader for the current class and invocation of
    *                                     {{{ s.checkPackageAccess() }}} denies access to the package of the class
    * @throws IllegalAccessException      thrown if the [[java.lang.reflect.Field]] object is enforcing
    *                                     Java language access control and the underlying field is inaccessible
    * @throws IllegalArgumentException    thrown if the specified object is not an instance of the class or interface
    *                                     declaring the underlying field (or a subclass or implementor thereof)
    * @throws NullPointerException        thrown if the specified object is null and the field is an instance field
    * @throws ExceptionInInitializerError thrown if the initialization provoked by {{{ f.get(clazz) }}},
    *                                     where {{{ f }}} is an instance of [[java.lang.reflect.Field]], fails
    * @throws ClassCastException          thrown if the retrieve object instance cannot be cast to [[T]]
    */
  def objectInstanceOf[T: ClassTag](clazz: Class[_]): T =
    clazz.getField("MODULE$").get(clazz) match {
      case obj: T => obj
      case other  => throw new ClassCastException(s"$other cannot be cast to the expected type!")
    }
}
