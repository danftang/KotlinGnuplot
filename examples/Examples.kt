package examples

import Gnuplot
import Gnuplot.Companion.generateXYSequence
import gnuplot
import java.io.FileOutputStream
import kotlin.math.sin

fun main() {
    Examples.pointPlot()
    Examples.binaryData()
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
        gnuplot {
            val data = heredoc(data1D)
            invoke("""
                set title 'Simple Line Plot'
                plot $data with lines
            """)
        }
    }

    fun binaryData() {
        gnuplot {
            invoke("plot ${binary(1,XSIZE)} with lines")
            write(data1D)
        }
    }


    fun linePlotToPng() {
        val file = FileOutputStream("examples/img/plot.png")
        gnuplot(pipeOutputTo = file) {
            val data = heredoc(data1D)
            invoke("""
                set term png
                plot $data with lines
            """)
        }
    }

    fun surfacePlot() {
        gnuplot {
            val splotData = heredoc(generateXYSequence(XSIZE, YSIZE).map {
                Triple(it.x,it.y,data2D[it.x][it.y])
            }, YSIZE)
            invoke("""
                set title 'Simple surface plot'
                splot $splotData with lines
                """)
        }
    }

    fun contourPlot() {
        gnuplot {
            val plotData = data2D.asSequence().flatMap { it.asSequence() }.map { it * 10.0f }
 //           val plotData = generateXYSequence(XSIZE, YSIZE).map {
 //               Triple(it.x,it.y,data2D[it.x][it.y]) }
 //           val doc = heredoc(plotData,YSIZE)
            invoke("""
                set contour
                unset surface
                set view map
                set cntrparam levels incremental -10,2,10
                set key off
                set xlabel 'xaxis'
                set ylabel 'yaxis'
                splot ${binary(1,YSIZE,XSIZE)} with lines
                """)
            write(plotData)
        }
    }

    fun heredocumentPlot() {
        gnuplot {
            val plotData = Gnuplot.generateXYSequence(XSIZE, YSIZE).flatMap { coord ->
                sequenceOf(coord.x.toFloat(), coord.y.toFloat(), sin(coord.x*0.1f)*sin(coord.y*0.1f))
            }
            val doc = heredoc(plotData, 3, YSIZE)
            invoke("splot $doc with pm3d")
        }
    }
}
