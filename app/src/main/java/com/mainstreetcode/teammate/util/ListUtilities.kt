/*
 * MIT License
 *
 * Copyright (c) 2019 Adetunji Dahunsi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.mainstreetcode.teammate.util

import com.tunjid.androidx.recyclerview.diff.Differentiable
import java.util.*

fun asDifferentiables(subTypeList: List<Differentiable>): List<Differentiable> =
        ArrayList(subTypeList)

fun <T : Differentiable> preserveAscending(source: List<T>, additions: List<T>): List<T> =
        concatenateList(source, additions).sortedWith(FunctionalDiff.COMPARATOR)

fun <T : Differentiable> preserveDescending(source: List<T>, additions: List<T>): List<T> =
        concatenateList(source, additions).sortedWith(FunctionalDiff.DESCENDING_COMPARATOR)

fun <T : Differentiable> replaceList(@Suppress("UNUSED_PARAMETER") source: List<T>, additions: List<T>): List<T> =
        additions.sortedWith(FunctionalDiff.COMPARATOR)

private fun <T : Differentiable> concatenateList(source: List<T>, additions: List<T>): List<T> =
        HashSet(additions).run { addAll(source); toList() }
