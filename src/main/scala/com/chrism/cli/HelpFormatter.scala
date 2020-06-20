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
package com.chrism.cli

import java.{io => jio}

final class HelpFormatter private (
  options: CommandLineOptions,
  pad: Int,
  width: Int,
  shortOptWidth: Int,
  longOptWidth: Int,
  requiredWidth: Int,
  rangeWidth: Int,
  descWidth: Int) {

  import HelpFormatter._

  def printHelp(): Unit = {
    val writer = new jio.PrintWriter(System.out)

    // TODO: handle new line
    printAppName(writer)
    printUsage(writer)
    printCommands(writer)

    writer.flush()
  }

  private[this] def printAppName(writer: jio.PrintWriter): Unit =
    options.appName
      .foreach { name =>
        val line = horizontalLine((name.length + 2) min width)
        writer.println(line)
        writer.print(" ")
        if (name.length <= (width - 2)) {
          writer.println(name)
        } else {
          var start = 0
          while (start < name.length) {
            if (start > 0) {
              writer.print(" ")
            }
            val nextStart = start + width - 2
            writer.println(name.substring(start, nextStart min name.length))
            start = nextStart
          }

        }
        writer.println(line)
        writer.println()
      }

  private[this] def printUsage(writer: jio.PrintWriter): Unit =
    options.usage
      .foreach { usage =>
        writer.print(Usage)
        val usagePadLen = UsagePad.length
        if (usage.length <= (width - usagePadLen)) {
          writer.println(usage)
        } else {
          var start = 0
          while (start < usage.length) {
            if (start > 0) {
              writer.print(UsagePad)
            }
            val nextStart = start + width - usagePadLen
            writer.println(usage.substring(start, nextStart min usage.length))
            start = nextStart
          }
        }
        writer.println()
      }

  private[this] def printCommands(writer: jio.PrintWriter): Unit = {
    writer.println("Available Options:")

    val shortOptFiller = " " * (shortOptWidth + pad)
    val longOptFiller = " " * longOptWidth
    val requiredFiller = " " * requiredWidth
    val rangeFiller = " " * rangeWidth
    val padFiller = " " * pad

    printHeader(writer)

    writer.println(horizontalLine(width))

    options.opts.toSeq
      .sortBy(o => (o.shortOpt, o.longOpt))
      .foreach { o =>
        val shortOpt = o.prefixedShortOpt.getOrElse("")
        val longOpt = o.prefixedLongOpt.getOrElse("")
        val required = if (o.required) "Required" else "Optional"
        val range = o.argRange.toString
        val desc = o.description.getOrElse("")

        var shortOptStart = 0
        var longOptStart = 0
        var requiredStart = 0
        var rangeStart = 0
        var descStart = 0

        var shortOptDone = shortOpt.length == 0
        var longOptDone = longOpt.length == 0
        var requiredDone = false
        var rangeDone = false
        var descDone = desc.length == 0

        while (!shortOptDone || !longOptDone || !requiredDone || !rangeDone || !descDone) {
          if (shortOptDone) {
            writer.print(shortOptFiller)
          } else {
            writer.print(padFiller)
            val nextStart = (shortOptStart + shortOptWidth - pad) min shortOpt.length
            writer.print(shortOpt.substring(shortOptStart, nextStart))
            if (nextStart == shortOpt.length) {
              writer.print(" " * (shortOptWidth - (nextStart - shortOptStart)))
              shortOptDone = true
            } else {
              writer.print(" " * pad)
              shortOptStart = nextStart
            }
          }

          if (longOptDone) {
            writer.print(longOptFiller)
          } else {
            val nextStart = (longOptStart + longOptWidth - pad) min longOpt.length
            writer.print(longOpt.substring(longOptStart, nextStart))
            if (nextStart == longOpt.length) {
              writer.print(" " * (longOptWidth - (nextStart - longOptStart)))
              longOptDone = true
            } else {
              writer.print(" " * pad)
              longOptStart = nextStart
            }
          }

          if (requiredDone) {
            writer.print(requiredFiller)
          } else {
            val nextStart = (requiredStart + requiredWidth - pad) min required.length
            writer.print(required.substring(requiredStart, nextStart))
            if (nextStart == required.length) {
              writer.print(" " * (requiredWidth - (nextStart - requiredStart)))
              requiredDone = true
            } else {
              writer.print(" " * pad)
              requiredStart = nextStart
            }
          }

          if (rangeDone) {
            writer.print(rangeFiller)
          } else {
            val nextStart = (rangeStart + rangeWidth - pad) min range.length
            writer.print(range.substring(rangeStart, nextStart))
            if (nextStart == range.length) {
              writer.print(" " * (rangeWidth - (nextStart - rangeStart)))
              rangeDone = true
            } else {
              writer.print(" " * pad)
              rangeStart = nextStart
            }
          }

          if (!descDone) {
            val nextStart = (descStart + descWidth - pad) min desc.length
            writer.print(desc.substring(descStart, nextStart))
            if (nextStart == desc.length) {
              descDone = true
            } else {
              descStart = nextStart
            }
          }

          writer.println()
        }

        writer.println(horizontalLine(width))
      }
  }

  private[this] def printHeader(writer: jio.PrintWriter): Unit = {
    writer.print(" " * pad)
    writer.print(ShortHeader)
    writer.print(" " * (shortOptWidth - ShortHeader.length))
    writer.print(LongHeader)
    writer.print(" " * (longOptWidth - LongHeader.length))
    writer.print(RequiredHeader)
    writer.print(" " * (requiredWidth - RequiredHeader.length))
    writer.print(RangeHeader)
    writer.print(" " * (rangeWidth - RangeHeader.length))
    writer.print(DescHeader)
    writer.print(" " * (descWidth - DescHeader.length))
    writer.println()
  }
}

object HelpFormatter {

  private val Usage: String = "Usage: "
  private val UsagePad: String = " " * Usage.length

  private val ShortHeader: String = "<short>"
  private val LongHeader: String = "<long>"
  private val RequiredHeader: String = "<required>"
  private val RangeHeader: String = "<#args>"
  private val DescHeader: String = "<description>"

  private val Required: String = "Required"
  private val Optional: String = "Optional"

  private[this] val MinWidth: Int = 74
  private[this] val DefaultWidth: Int = 120
  private[this] val DefaultPad: Int = 2
  private[this] val PadWidthRatio: Int = 6

  def apply(options: CommandLineOptions, width: Int = DefaultWidth, pad: Int = DefaultPad): HelpFormatter = {
    require(width >= MinWidth, s"The width should be at least $MinWidth to properly display")
    require(width > (6 * pad), s"The width ($width) should be at least $PadWidthRatio times the pad ($pad)")
    val maxShortOptWidth = maxShortOptWidthOf(options, pad)
    val maxLongOptWidth = maxLongOptWidthOf(options, pad)
    val maxRequiredWidth = maxRequiredWidthOf(options, pad)
    val maxArgRangeWidth = maxArgRangeWidthOf(options, pad)
    val descWidth = width - maxShortOptWidth - maxLongOptWidth - maxRequiredWidth - maxArgRangeWidth
    require(
      descWidth >= (width * 0.4),
      s"The width for description ($descWidth) should be at least 40% of the total width ($width)!")
    // TODO: recompute the ratio

    new HelpFormatter(
      options,
      pad,
      width,
      maxShortOptWidth,
      maxLongOptWidth,
      maxRequiredWidth,
      maxArgRangeWidth,
      descWidth)
  }

  private def horizontalLine(length: Int): String = "-" * length

  private[this] def maxShortOptWidthOf(options: CommandLineOptions, pad: Int): Int =
    (options.opts.flatMap(_.prefixedShortOpt).map(_.length).max max ShortHeader.length) + (pad * 2)

  private[this] def maxLongOptWidthOf(options: CommandLineOptions, pad: Int): Int =
    (options.opts.flatMap(_.prefixedLongOpt).map(_.length).max max LongHeader.length) + pad

  private[this] def maxRequiredWidthOf(options: CommandLineOptions, pad: Int): Int =
    (options.opts.map(o => if (o.required) Required.length else Optional.length).max max RequiredHeader.length) + pad

  private[this] def maxArgRangeWidthOf(options: CommandLineOptions, pad: Int): Int =
    (options.opts.map(_.argRange.toString.length).max max RangeHeader.length) + pad
}
