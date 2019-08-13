import org.apache.commons.exec.*
import java.io.*
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class Gnuplot : Closeable {
    val execResult = DefaultExecuteResultHandler()
    val pipe : PipedOutputStream // pipe feeding the executable's standard in
    private val nativeBuffer : ByteBuffer = ByteBuffer.allocate(4)
    var nextDataId = 1

    constructor(persist : Boolean = true, pipeOutputTo : OutputStream = System.out, pipeErrTo : OutputStream = System.err) {
        nativeBuffer.order(ByteOrder.nativeOrder())
        val execIn = PipedInputStream()
        val exec = DefaultExecutor()
        pipe = PipedOutputStream(execIn) // pipe feeding the executable's standard in
        val handler = PumpStreamHandler(pipeOutputTo, pipeErrTo, execIn)
        exec.streamHandler = handler
        val cl = CommandLine("gnuplot")
        if(persist) cl.addArgument("-p")
        exec.execute(cl, execResult)
        sleep(128) // wait for gnuplot instance to spin-up
    }


    // This form sends data in binary format
    // piping data over in binary is quicker, if speed is important
    fun plot(data : Sequence<Float>, nRecords : Int, ranges : String = "", plotStyle : String = "with lines title 'Binary data'", inferXCoord : Boolean = false): Gnuplot {
        val dataType = if(inferXCoord) "array" else "record"
        write("plot $ranges '-' binary $dataType=($nRecords) $plotStyle\n")
        write(data)
        return this
    }

    // This form sends data in binary format
    // piping data over in binary is quicker, if speed is important
    // data in order (x0,y0), (x0,y1)...(x0,yn), (x1,y0)...
    fun splot(data : Sequence<Float>, xSize : Int, ySize : Int, ranges : String = "", plotStyle : String = "with lines title 'Binary data'", inferXYCoords : Boolean = false): Gnuplot {
        val dataType = if(inferXYCoords) "array=($xSize,$ySize) transpose" else "record=($ySize,$xSize)"
        write("splot $ranges '-' binary $dataType $plotStyle\n")
        write(data)
        return this
    }

    fun plot(yData: Iterable<Number>, style: String="with lines") = plot(yData.asSequence(), style)
    fun plot(yData: Sequence<Number>, style: String="with lines"): Gnuplot {
        val dataId = getUniqueDataName()
        define(dataId, yData.map { it.toFloat() }, 1)
        write("plot \$$dataId $style\n")
        return(this)
    }


    fun replot(yData: Iterable<Number>, style: String="with lines") = replot(yData.asSequence(), style)
    fun replot(yData: Sequence<Number>, style: String="with lines"): Gnuplot {
        val dataId = getUniqueDataName()
        define(dataId, yData.map { it.toFloat() }, 1)
        write("replot \$$dataId $style\n")
        return(this)
    }


    fun plotxy(xyData: Iterable<Pair<Number,Number>>, style: String="with lines") = plotxy(xyData.asSequence(), style)
    fun plotxy(xyData: Sequence<Pair<Number,Number>>, style: String="with lines"): Gnuplot {
        val dataId = getUniqueDataName()
        define(dataId, xyData.flatMap { sequenceOf(it.first.toFloat(), it.second.toFloat()) }, 2)
        write("plot \$$dataId $style\n")
        return(this)
    }


    fun replotxy(xyData: Iterable<Pair<Number,Number>>, style: String="with lines") = replotxy(xyData.asSequence(), style)
    fun replotxy(xyData: Sequence<Pair<Number,Number>>, style: String="with lines"): Gnuplot {
        val dataId = getUniqueDataName()
        define(dataId, xyData.flatMap { sequenceOf(it.first.toFloat(), it.second.toFloat()) }, 2)
        write("replot \$$dataId $style\n")
        return(this)
    }

    fun splot(xyzData: Iterable<Triple<Number,Number,Number>>, ySize: Int, style: String="with lines") = splot(xyzData.asSequence(), ySize, style)
    fun splot(xyzData: Sequence<Triple<Number,Number,Number>>, ySize: Int, style: String="with lines"): Gnuplot {
        val dataId = getUniqueDataName()
        define(dataId, xyzData.flatMap {sequenceOf(it.first.toFloat(), it.second.toFloat(), it.third.toFloat())}, 3, ySize)
        write("splot \$$dataId $style\n")
        return(this)
    }

    fun replotxyz(xyzData: Iterable<Triple<Number,Number,Number>>, ySize: Int, style: String="with lines") = replotxyz(xyzData.asSequence(), ySize, style)
    fun replotxyz(xyzData: Sequence<Triple<Number,Number,Number>>, ySize: Int, style: String="with lines"): Gnuplot {
        val dataId = getUniqueDataName()
        define(dataId, xyzData.flatMap {sequenceOf(it.first.toFloat(), it.second.toFloat(), it.third.toFloat())}, 3, ySize)
        write("replot \$$dataId $style\n")
        return(this)
    }

    // define a here-document
    // N.B. this will only work with Gnuplot 5.0 upwards
    fun define(name : String, data : Sequence<Float>, nFields : Int, nRecords : Int = -1, nBlocks : Int = -1, nFrames : Int = -1): Gnuplot {
        val dataIt = data.iterator()
        write("\$$name << EOD\n")
        var frame = nFrames
        do {
            var block = nBlocks
            do {
                var record = nRecords
                do {
                    for(f in 1 until nFields) {
                        if(!dataIt.hasNext()) throw(IllegalArgumentException("not enough data points"))
                        write(dataIt.next().toString())
                        write(" ")
                    }
                    write(dataIt.next().toString())
                    write("\n")
                } while(if(nRecords ==-1) dataIt.hasNext() else --record !=0)
                write("\n")
            } while(if(nBlocks == -1) dataIt.hasNext() else --block != 0)
            write("\n")
        } while(if(nFrames == -1) dataIt.hasNext() else --frame != 0)
        write("EOD\n")
        if(dataIt.hasNext()) throw(IllegalArgumentException("too many data points"))
        return this
    }

    fun undefine(name : String): Gnuplot {
        write("undefine \$$name\n")
        return this
    }


    fun write(data : Sequence<Float>) {
        data.forEach {
            nativeBuffer.putFloat(0, it)
            pipe.write(nativeBuffer.array())
        }
    }

    fun write(s: String) = pipe.write(s.toByteArray())

    fun write(f: Float) {
        nativeBuffer.putFloat(0, f)
        pipe.write(nativeBuffer.array())
    }

    override fun close() = pipe.close()

    // Use this to force gnuplot to plot without having to close the connection
    // e.g. to do animation
    fun flush(): Gnuplot {
        for(i in 1..250) {
            write("# fill gnuplots buffer with comments\n") // this persuades gnuplot to read its input!
        }
        pipe.flush()
        return this
    }

    // invoke gnuplot command
    operator fun invoke(s : String): Gnuplot {
        pipe.write(s.toByteArray())
        pipe.write('\n'.toInt())
        return this
    }


    fun getUniqueDataName() : String {
        return "data${nextDataId++}"
    }

    // wait for termination of binary
    fun waitFor() = execResult.waitFor()
    fun waitFor(timeout : Long) = execResult.waitFor(timeout)

    companion object {
        data class XYCoord(val x: Int, val y: Int)
        fun generateXYSequence(xSize : Int, ySize : Int) =
                (0 until xSize*ySize).asSequence().map { XYCoord(it.div(ySize), it.rem(ySize)) }

        fun generateXSequence(xSize : Int) = (0 until xSize).asSequence()
    }
}
