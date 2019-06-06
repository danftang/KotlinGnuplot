import org.apache.commons.exec.*
import java.io.*

open class Gnuplot {
    val execResult = DefaultExecuteResultHandler()
    var pipe : DataOutputStream // pipe feeding the executable's standard in

    constructor(persist : Boolean = true, pipeOutputTo : OutputStream = System.out, pipeErrTo : OutputStream = System.err) {
        val execIn = PipedInputStream()
        val exec = DefaultExecutor()
        pipe = DataOutputStream(PipedOutputStream(execIn)) // pipe feeding the executable's standard in
        val handler = PumpStreamHandler(pipeOutputTo, pipeErrTo, execIn)
        exec.streamHandler = handler
        val cl = CommandLine("gnuplot")
        if(persist) cl.addArgument("-p")
        exec.execute(cl, execResult)
    }

    // default plotters

    fun plot(data : Sequence<Float>, nRecords : Int=-1, plotStyle : String = "with lines title 'Kotlin data'", inferXCoord : Boolean = false) {
        val dataType = if(inferXCoord) "array" else "record"
        write("plot '-' binary endian=big $dataType=($nRecords) $plotStyle\n")
        data.forEach { write(it) }
    }

    // data in order (x0,y0), (x0,y1)...(x0,yn), (x1,y0)...
    fun splot(data : Sequence<Float>, xSize : Int = -1, ySize : Int, plotStyle : String = "with lines title 'Kotlin data'", inferXYCoords : Boolean = false) {
        val dataType = if(inferXYCoords) "array=($xSize,$ySize) transpose" else "record=($ySize,$xSize)"
        write("splot '-' binary endian=big $dataType $plotStyle\n")
        data.forEach { write(it) }
    }

    class XYIterator(xSize: Int, ySize: Int) : Iterator<XYIterator> {
        val x : Float
            get() = xi.toFloat()
        val y : Float
            get() = yi.toFloat()
        var xi = 0
            private set
        var yi = -1
            private set
        var xSize : Int = xSize
            private set
        var ySize : Int = ySize
            private set

        override fun hasNext() = ((xi < xSize-1) || (yi < ySize-1))
        override fun next(): XYIterator {
            ++yi
            if(yi == ySize) {
                yi = 0
                ++xi
            }
            if(xi == xSize) {
                xi = 0
            }
            return this
        }
    }

    fun generateXYSequence(xSize : Int, ySize : Int) :Sequence<XYIterator> {
        return Sequence { XYIterator(xSize, ySize) }
    }

    fun generateXSequence(xSize : Int) :Sequence<Int> {
        return (0..xSize).asSequence()
    }

    fun write(data : Sequence<Float>) {
        data.forEach { pipe.writeFloat(it) }
    }

    fun write(data : Iterable<Float>) {
        data.forEach { pipe.writeFloat(it) }
    }

    fun write(vararg data : Float) {
        for(f in data) {
            pipe.writeFloat(f)
        }
    }


    fun write(s: String) = pipe.write(s.toByteArray())

    fun write(f: Float) = pipe.writeFloat(f)

    fun close() = pipe.close()

    // Use this to force gnuplot to plot without having to close the connection
    // e.g. to do animation
    fun flush() {
        for(i in 1..250) {
            write("# fill gnuplots buffer with comments\n") // this persuades gnuplot to read its input!
        }
        pipe.flush()
    }

    // invoke gnuplot command
    operator fun invoke(s : String) {
        pipe.write(s.toByteArray())
        pipe.write('\n'.toInt())
    }

    // wait for termination of binary
    fun waitFor() = execResult.waitFor()
    fun waitFor(timeout : Long) = execResult.waitFor(timeout)

}
