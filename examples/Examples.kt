package examples

import Gnuplot
import java.io.FileOutputStream
import kotlin.math.sin

fun main() {
    Examples.linePlot()
    Examples.surfacePlot()
    Examples.contourPlot()
}

object Examples {
    const val XSIZE = 50
    const val YSIZE = 100
    val data1D = Array(XSIZE) { x -> sin(x*0.1f) }
    val data2D = Array(XSIZE) { x ->
        Array(YSIZE) { y ->
            sin(x*0.1f)*sin(y*0.1f)
        }
    }

    fun linePlot() {
        val gnuplot = Gnuplot()
        gnuplot("set title 'Simple Line Plot'")
        gnuplot.plot(data1D.asSequence(), inferXCoord = true)
        gnuplot.close()
    }

    fun linePlotToPng() {
        val file = FileOutputStream("examples/img/plot.png")
        val gnuplot = Gnuplot(pipeOutputTo = file)
        gnuplot("set term png")
        gnuplot.plot(data1D.asSequence(), inferXCoord = true)
        gnuplot.close()

    }

    fun surfacePlot() {
        val gnuplot = Gnuplot()
        gnuplot("set title 'Simple Surface Plot'")
        val plotData = data2D.asSequence().flatMap { it.asSequence() }
        gnuplot.splot(plotData, XSIZE, YSIZE, inferXYCoords = true)
        gnuplot.close()
    }

    fun contourPlot() {
        val gnuplot = Gnuplot()
        gnuplot("set contour")
        gnuplot("unset surface")
        gnuplot("set view map")
        gnuplot("set cntrparam levels incremental -10,2,10")
        gnuplot("set key off")
        gnuplot("set xlabel 'xaxis'")
        gnuplot("set ylabel 'yaxis'")
        val plotData = data2D.asSequence().flatMap { it.asSequence() }.map { it*10.0f }
        gnuplot.splot(plotData, XSIZE, YSIZE, inferXYCoords = true)
    }

}
