package com.heluna.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import scala.io.Source

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 5/9/13
 * Time: 11:46 AM
 */

object CompressUtil {

	def compress(in: String): String = {
		val out = new ByteArrayOutputStream()
		val gzip = new GZIPOutputStream(out)
		gzip.write(in.getBytes("ISO-8859-1"))
		gzip.close()
		out.toString("ISO-8859-1")
	}

	def decompress(in: String): String =
		Source.fromInputStream(new GZIPInputStream(new ByteArrayInputStream(in.getBytes("ISO-8859-1")))).mkString

}
