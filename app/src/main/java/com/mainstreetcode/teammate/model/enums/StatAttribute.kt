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

package com.mainstreetcode.teammate.model.enums

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonObject
import java.util.*

class StatAttribute internal constructor(code: String, name: String) : MetaData(code, name) {

    override fun toString(): String = code

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StatAttribute) return false
        val variant = other as StatAttribute?
        return code == variant!!.code && name == variant.name
    }

    override fun hashCode(): Int = Objects.hash(code, name)

    class GsonAdapter : MetaData.GsonAdapter<StatAttribute>() {
        override fun fromJson(code: String, name: String, body: JsonObject, context: JsonDeserializationContext): StatAttribute =
                StatAttribute(code, name)
    }

    companion object {

        fun empty(): StatAttribute = StatAttribute("", "")
    }
}
