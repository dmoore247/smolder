package com.databricks.smolder

import com.databricks.smolder.functions._
import org.apache.spark.sql.functions._

case class TextFile(file: String, value: String)

class functionsSuite extends SmolderBaseTest {

  test("loading an hl7 message as text and then parse") {

    val file = testFile("single_record.hl7")

    val df = spark.createDataFrame(spark.sparkContext
      .wholeTextFiles(file)
      .map(p => TextFile(p._1, p._2)))

    val hl7Df = df.select(parse_hl7_message(df("value")).alias("hl7"))

    assert(hl7Df.count() === 1)
    assert(hl7Df.selectExpr("explode(hl7.segments)").count() === 3)
    assert(hl7Df.selectExpr("explode(hl7.segments) as segments")
      .selectExpr("explode(segments.fields)")
      .count() === 57)
  }

  test("use the segment field function to extract the event type") {

    val file = testFile("single_record.hl7")

    val df = spark.read.format("hl7")
      .load(file)

    val evnType = df.select(segment_field("EVN", 0).alias("type"))
    assert(evnType.count() === 1)
    assert(evnType.first().getString(0) === "A03")
  }

  test("use the segment field function to extract the event type, different column name") {

    val file = testFile("single_record.hl7")

    val df = spark.createDataFrame(spark.sparkContext
      .wholeTextFiles(file)
      .map(p => TextFile(p._1, p._2)))

    val hl7Df = df.select(parse_hl7_message(df("value")).alias("hl7"))

    val evnType = hl7Df.select(segment_field("EVN", 0, col("hl7.segments"))
      .alias("type"))
    assert(evnType.count() === 1)
    assert(evnType.first().getString(0) === "A03")
  }

  test("use the segment field and subfield functions to extract the patient's first name") {

    val file = testFile("single_record.hl7")

    val df = spark.read.format("hl7")
      .load(file)

    val pidName = df.select(segment_field("PID", 4).alias("name"))
    assert(pidName.count() === 1)
    assert(pidName.first().getString(0) === "Heller^Keneth")

    val firstName = pidName.select(subfield(col("name"), 1))
    assert(firstName.count() === 1)
    assert(firstName.first().getString(0) === "Keneth")
  }
}