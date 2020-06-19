package no.ntnu.ihb.acco.core


typealias StringArray = Array<String>

fun StringArray(size: Int = 0) = StringArray(size) { "" }

enum class Causality {
    LOCAL, INPUT, OUTPUT, PARAMETER, CALCULATED_PARAMETER
}

fun interface ReferenceProvider<E> {
    fun invoke(values: E)
}

sealed class Var<E> {

    abstract val size: Int
    abstract val causality: Causality

    abstract fun read(values: E): E
    abstract fun write(values: E)

}

abstract class IntVar : Var<IntArray>()
abstract class RealVar : Var<DoubleArray>()
abstract class StrVar : Var<StringArray>()
abstract class BoolVar : Var<BooleanArray>()

class IntLambdaVar(
    override val size: Int,
    private val getter: ReferenceProvider<IntArray>,
    private val setter: ReferenceProvider<IntArray>? = null,
    override val causality: Causality = Causality.LOCAL
) : IntVar() {

    override fun read(values: IntArray): IntArray {
        require(size == values.size)
        getter.invoke(values)
        return values
    }

    override fun write(values: IntArray) {
        require(size == values.size)
        if (setter == null) throw IllegalStateException("No setter defined")
        setter.invoke(values)
    }
}

class RealLambdaVar(
    override val size: Int,
    private val getter: ReferenceProvider<DoubleArray>,
    private val setter: ReferenceProvider<DoubleArray>? = null,
    override val causality: Causality = Causality.LOCAL
) : RealVar() {

    override fun read(values: DoubleArray): DoubleArray {
        require(size == values.size)
        getter.invoke(values)
        return values
    }

    override fun write(values: DoubleArray) {
        require(size == values.size)
        if (setter == null) throw IllegalStateException("No setter defined")
        setter.invoke(values)
    }

}

class StrLambdaVar(
    override val size: Int,
    private val getter: ReferenceProvider<StringArray>,
    private val setter: ReferenceProvider<StringArray>? = null,
    override val causality: Causality = Causality.LOCAL
) : StrVar() {

    override fun read(values: StringArray): StringArray {
        require(size == values.size)
        getter.invoke(values)
        return values
    }

    override fun write(values: StringArray) {
        require(size == values.size)
        if (setter == null) throw IllegalStateException("No setter defined")
        setter.invoke(values)
    }

}

class BoolLambdaVar(
    override val size: Int,
    private val getter: ReferenceProvider<BooleanArray>,
    private val setter: ReferenceProvider<BooleanArray>? = null,
    override val causality: Causality = Causality.LOCAL
) : BoolVar() {

    override fun read(values: BooleanArray): BooleanArray {
        require(size == values.size)
        getter.invoke(values)
        return values
    }

    override fun write(values: BooleanArray) {
        require(size == values.size)
        if (setter == null) throw IllegalStateException("No setter defined")
        setter.invoke(values)
    }

}


/*private fun IntVar.buffered() = BufferedIntVar(this)
private fun RealVar.buffered() = BufferedRealVar(this)*/


/*class BufferedIntVar(
    private val v: IntVar
) : IntVar by v {

    private val readBuffer = IntArray(size)
    private val writeBuffer = IntArray(size)

    override fun read(values: IntArray) {
        require(values.size == size)
        readBuffer.copyInto(values)
    }

    override fun write(values: IntArray) {
        require(values.size == size)
        values.copyInto(writeBuffer)
    }

    fun swapBuffers() {
        v.read(readBuffer)
        v.write(writeBuffer)
    }

}*/

/*class BufferedRealVar(
    private val real: RealVar
) : RealVar by real {

    private val readBuffer = DoubleArray(size)
    private val writeBuffer = DoubleArray(size)

    override fun read(values: DoubleArray) {
        require(values.size == size)
        readBuffer.copyInto(values)
    }

    override fun write(values: DoubleArray) {
        require(values.size == size)
        values.copyInto(writeBuffer)
    }

    fun swapBuffers() {
        real.read(readBuffer)
        real.write(writeBuffer)
    }

}*/

