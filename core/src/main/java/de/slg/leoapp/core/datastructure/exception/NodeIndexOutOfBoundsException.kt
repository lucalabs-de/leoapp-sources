@file:Suppress("unused")

package de.slg.leoapp.core.datastructure.exception

class NodeIndexOutOfBoundsException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}