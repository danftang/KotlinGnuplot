package examples

import Gnuplot
import java.io.FileOutputStream
import kotlin.math.sin

fun main() {
    Examples.pointPlot()
    Examples.surfacePlot()
    Examples.contourPlot()
    Examples.heredocumentPlot()
}

object Examples {
    const val XSIZE = 50
    const val YSIZE = 100
    val data1D = Array(XSIZE) { x -> sin(x*0.1f) }.asList()
    val data2D = Array(XSIZE) { x ->
        Array(YSIZE) { y ->
            sin(x*0.1f)*sin(y*0.1f)
        }
    }.asList()


    fun pointPlot() {
        Gnuplot()
            .invoke("set title 'Simple Line Plot'")
            .plot(data1D, "with points")
            .close()
    }

    fun linePlotToPng() {
        val file = FileOutputStream("examples/img/plot.png")
        Gnuplot(pipeOutputTo = file)
            .invoke("set term png")
            .plot(data1D)
            .close()
    }

    fun surfacePlot() {
        val plotData = Gnuplot.generateXYSequence(XSIZE, YSIZE).map {
            Triple(it.x,it.y,data2D[it.x][it.y])
        }
        Gnuplot()
            .invoke("set title 'Simple surface plot'")
            .splot(plotData, YSIZE)
            .close()
    }

    fun contourPlot() {
        val plotData = data2D.asSequence().flatMap { it.asSequence() }.map { it*10.0f }
        Gnuplot()
            .invoke("set contour;" +
                    "unset surface;" +
                    "set view map;" +
                    "set cntrparam levels incremental -10,2,10;" +
                    "set key off;" +
                    "set xlabel 'xaxis';" +
                    "set ylabel 'yaxis'"
            )
            .splot(plotData, XSIZE, YSIZE, inferXYCoords = true)
            .close()
    }

    fun heredocumentPlot() {
        val plotData = Gnuplot.generateXYSequence(XSIZE, YSIZE).flatMap { coord ->
            sequenceOf(coord.x.toFloat(), coord.y.toFloat(), sin(coord.x*0.1f)*sin(coord.y*0.1f))
        }
        Gnuplot()
            .define("data", plotData, 3, YSIZE)
            .invoke("splot \$data with pm3d")
            .undefine("data")
            .close()
    }
}
